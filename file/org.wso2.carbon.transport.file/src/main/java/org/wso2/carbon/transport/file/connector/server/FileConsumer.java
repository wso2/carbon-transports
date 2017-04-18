/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.transport.file.connector.server;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.TextCarbonMessage;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.file.connector.server.exception.FileServerConnectorException;
import org.wso2.carbon.transport.file.connector.server.util.Constants;
import org.wso2.carbon.transport.file.connector.server.util.FileTransportUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.wso2.carbon.transport.file.connector.server.util.FileTransportUtils.maskURLPassword;

/**
 * Provides the capability to process a file and delete it afterwards.
 */
public class FileConsumer {

    private static final Logger log = LoggerFactory.getLogger(FileConsumer.class);

    private Map<String, String> fileProperties;
    private FileSystemManager fsManager = null;
    private String serviceName;
    private CarbonMessageProcessor messageProcessor;
    private String fileURI;
    private FileObject fileObject;
    private FileSystemOptions fso;
    private boolean runPostProcess = true;
    private boolean fileLock = true;
    /**
     * Time-out interval (in mill-seconds) to wait for the callback.
     */
    private long timeOutInterval = 30000;
    private boolean deleteIfNotAck = false;
    private String fileNamePattern = null;

    public FileConsumer(String id, Map<String, String> fileProperties, CarbonMessageProcessor messageProcessor)
            throws ServerConnectorException {
        this.serviceName = id;
        this.fileProperties = fileProperties;
        this.messageProcessor = messageProcessor;

        setupParams();
        try {
            StandardFileSystemManager fsm = new StandardFileSystemManager();
            fsm.setConfiguration(getClass().getClassLoader().getResource("providers.xml"));
            fsm.init();
            fsManager = fsm;
        } catch (FileSystemException e) {
            throw new ServerConnectorException(
                    "Could not initialize File System Manager from " + "the configuration: providers.xml", e);
        }
        Map<String, String> options = parseSchemeFileOptions(fileURI);
        fso = FileTransportUtils.attachFileSystemOptions(options, fsManager);

        if (options != null && Constants.SCHEME_FTP.equals(options.get(Constants.SCHEME))) {
            FtpFileSystemConfigBuilder.getInstance().setPassiveMode(fso, true);
        }

        try {
            fileObject = fsManager.resolveFile(fileURI, fso);
        } catch (FileSystemException e) {
            throw new FileServerConnectorException("Failed to resolve fileURI: " + maskURLPassword(fileURI), e);
        }
    }

    /**
     * Do the file processing operation for the given set of properties. Do the
     * checks and pass the control to processFile method
     */
    public void consume() throws FileServerConnectorException {
        if (log.isDebugEnabled()) {
            log.debug("Polling for directory or file : " + maskURLPassword(fileURI));
        }

        // If file/folder found proceed to the processing stage
        try {
            boolean isFileExists;
            try {
                isFileExists = fileObject.exists();
            } catch (FileSystemException e) {
                throw new FileServerConnectorException("Error occurred when determining whether the file at URI : " +
                                                       maskURLPassword(fileURI) + " exists. " + e);
            }

            boolean isFileReadable;
            try {
                isFileReadable = fileObject.isReadable();
            } catch (FileSystemException e) {
                throw new FileServerConnectorException("Error occurred when determining whether the file at URI : " +
                                                       maskURLPassword(fileURI) + " is readable. " +
                                                       e);
            }

            if (isFileExists && isFileReadable) {
                FileType fileType;
                try {
                    fileType = fileObject.getType();
                } catch (FileSystemException e) {
                    throw new FileServerConnectorException("Error occurred when determining whether file: " +
                                                           maskURLPassword(fileURI) + " is a file or a folder", e);
                }

                if (fileType == FileType.FILE) {
                    fileHandler();
                } else if (fileType == FileType.FOLDER) {
                    FileObject[] children = null;
                    try {
                        children = fileObject.getChildren();
                    } catch (FileSystemException ignored) {
                        if (log.isDebugEnabled()) {
                            log.debug("The file does not exist, or is not a folder, or an error " +
                                      "has occurred when trying to list the children. File URI : " +
                                      maskURLPassword(fileURI), ignored);
                        }
                    }

                    // if this is a file that would translate to a single message
                    if (children == null || children.length == 0) {
                        if (log.isDebugEnabled()) {
                            log.debug("Folder at " + maskURLPassword(fileURI) + " is empty.");
                        }
                    } else {
                        directoryHandler(children);
                    }
                } else {
                    throw new FileServerConnectorException(
                            "File: " + maskURLPassword(fileURI) + " is neither a file or " +
                            "a folder" + (fileType == null ? "" : ". Found file type: " + fileType.toString()));
                }
            } else {
                throw new FileServerConnectorException(
                        "Unable to access or read file or directory : " + maskURLPassword(fileURI) +
                        ". Reason: " +
                        (isFileExists ? (isFileReadable ? "Unknown reason" : "The file can not be read!") :
                         "The file does not exist!"));
            }
        } finally {
            try {
                fileObject.close();
            } catch (FileSystemException e) {
                log.warn("Could not close file at URI: " + maskURLPassword(fileURI), e);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("End : Scanning directory or file : " + maskURLPassword(fileURI));
        }
    }

    /**
     * Setup the required transport parameters.
     */
    private void setupParams() throws ServerConnectorException {
        fileURI = fileProperties.get(Constants.TRANSPORT_FILE_FILE_URI);
        if (fileURI == null) {
            throw new ServerConnectorException(Constants.TRANSPORT_FILE_FILE_URI + " is a " +
                                               "mandatory parameter for " + Constants.PROTOCOL_NAME + " transport.");
        }
        if (fileURI.trim().equals("")) {
            throw new ServerConnectorException(Constants.TRANSPORT_FILE_FILE_URI + " parameter " +
                                               "cannot be empty for " + Constants.PROTOCOL_NAME + " transport.");
        }
        String timeOut = fileProperties.get(Constants.FILE_ACKNOWLEDGEMENT_TIME_OUT);
        if (timeOut != null) {
            try {
                timeOutInterval = Long.parseLong(timeOut);
            } catch (NumberFormatException e) {
                log.error("Provided " + Constants.FILE_ACKNOWLEDGEMENT_TIME_OUT + " is invalid. " +
                          "Using the default callback timeout, " +
                          timeOutInterval + " milliseconds", e);
            }
        }
        String strDeleteIfNotAck = fileProperties.get(Constants.FILE_DELETE_IF_NOT_ACKNOWLEDGED);
        if (strDeleteIfNotAck != null) {
            deleteIfNotAck = Boolean.parseBoolean(strDeleteIfNotAck);
        }
        fileNamePattern = fileProperties.get(Constants.FILE_NAME_PATTERN);
        String strLocking = fileProperties.get(Constants.LOCKING);
        if (strLocking != null) {
            fileLock = Boolean.parseBoolean(strLocking);
        }
    }

    private Map<String, String> parseSchemeFileOptions(String fileURI) {
        String scheme = UriParser.extractScheme(fileURI);
        if (scheme == null) {
            return null;
        }
        HashMap<String, String> schemeFileOptions = new HashMap<>();
        schemeFileOptions.put(Constants.SCHEME, scheme);
        addOptions(scheme, schemeFileOptions);
        return schemeFileOptions;
    }

    private void addOptions(String scheme, Map<String, String> schemeFileOptions) {
        if (scheme.equals(Constants.SCHEME_SFTP)) {
            for (Constants.SftpFileOption option : Constants.SftpFileOption.values()) {
                String strValue = fileProperties.get(Constants.SFTP_PREFIX + option.toString());
                if (strValue != null && !strValue.equals("")) {
                    schemeFileOptions.put(option.toString(), strValue);
                }
            }
        }
    }

    /**
     * Handle directory with child elements.
     *
     * @param children
     * @return
     * @throws FileSystemException
     */
    private void directoryHandler(FileObject[] children) throws FileServerConnectorException {
        // Sort the files
        String strSortParam = fileProperties.get(Constants.FILE_SORT_PARAM);

        if (strSortParam != null && !"NONE".equals(strSortParam)) {
            log.debug("Starting to sort the files in folder: " + maskURLPassword(fileURI));

            String strSortOrder = fileProperties.get(Constants.FILE_SORT_ORDER);
            boolean bSortOrderAscending = true;

            if (strSortOrder != null) {
                bSortOrderAscending = Boolean.parseBoolean(strSortOrder);
            }
            if (log.isDebugEnabled()) {
                log.debug("Sorting the files by : " + strSortOrder + ". (" +
                          bSortOrderAscending + ")");
            }
            switch (strSortParam) {
                case Constants.FILE_SORT_VALUE_NAME:
                    if (bSortOrderAscending) {
                        Arrays.sort(children, new FileNameAscComparator());
                    } else {
                        Arrays.sort(children, new FileNameDesComparator());
                    }
                    break;
                case Constants.FILE_SORT_VALUE_SIZE:
                    if (bSortOrderAscending) {
                        Arrays.sort(children, new FileSizeAscComparator());
                    } else {
                        Arrays.sort(children, new FileSizeDesComparator());
                    }
                    break;
                case Constants.FILE_SORT_VALUE_LASTMODIFIEDTIMESTAMP:
                    if (bSortOrderAscending) {
                        Arrays.sort(children, new FileLastmodifiedtimestampAscComparator());
                    } else {
                        Arrays.sort(children, new FileLastmodifiedtimestampDesComparator());
                    }
                    break;
                default:
                    log.warn("Invalid value given for " + Constants.FILE_SORT_PARAM + " parameter. " +
                             " Expected one of the values: " + Constants.FILE_SORT_VALUE_NAME + ", " +
                             Constants.FILE_SORT_VALUE_SIZE + " or " + Constants.FILE_SORT_VALUE_LASTMODIFIEDTIMESTAMP +
                             ". Found: " + strSortParam);
                    break;
            }
            if (log.isDebugEnabled()) {
                log.debug("End sorting the files.");
            }
        }
        for (FileObject child : children) {
            if (child.getName().getBaseName().endsWith(".lock")
                /*|| child.getName().getBaseName().endsWith(".fail")*/) {
                continue;
            }
            if ((fileNamePattern != null && !fileObject.getName().getBaseName().matches(fileNamePattern)) &&
                log.isDebugEnabled()) {
                log.debug("File " + maskURLPassword(fileObject.getName().getBaseName()) +
                          " is not processed because it did not match the specified pattern.");
            } else {
                if (!fileLock || acquireLock(fsManager, child)) {
                    processFile(child);
                    postProcess(child);
                    if (fileLock) {
                        // TODO: passing null to avoid build break. Fix properly
                        releaseLock(fsManager, child, fso);
                        if (log.isDebugEnabled()) {
                            log.debug("Removed the lock file '" + maskURLPassword(fileObject.toString()) +
                                      ".lock' of the file '" + maskURLPassword(fileObject.toString()));
                        }
                    }
                } else {
                    log.error("Couldn't get the lock for processing the file : " + maskURLPassword(fileURI));
                }
            }
            //close the file system after processing
            try {
                child.close();
            } catch (FileSystemException e) {
                log.warn("Could not close the file: " + child.getName().getPath(), e);
            }
        }
    }

    /**
     * If not a folder just a file handle the flow.
     *
     * @throws FileSystemException
     */
    private void fileHandler() {
        if (!fileLock || (acquireLock(fsManager, fileObject))) {
            try {
                processFile(fileObject);
                //lastCycle = 1;
            } catch (FileServerConnectorException e) {
                //lastCycle = 2;
                log.error("Error processing File URI : " + maskURLPassword(fileObject.getName().toString()), e);
            }

            try {
                postProcess(fileObject);
            } catch (FileServerConnectorException e) {
                //lastCycle = 3;
                log.error("File object '" + maskURLPassword(fileObject.getName().toString()) + "' " +
                          "cloud not be moved", e);
                //VFSUtils.markFailRecord(fsManager, fileObject);
            }

            if (fileLock) {
                // TODO: passing null to avoid build break. Fix properly
                releaseLock(fsManager, fileObject, fso);
                if (log.isDebugEnabled()) {
                    log.debug("Removed the lock file '" + maskURLPassword(fileObject.toString()) +
                              ".lock' of the file '" + maskURLPassword(fileObject.toString()));
                }
            }
        } else {
            log.error("Couldn't get the lock for processing the file : " +
                      maskURLPassword(fileObject.getName().toString()));
        }
    }

    /**
     * Actual processing of the file/folder.
     *
     * @param file
     * @return
     */
    private FileObject processFile(FileObject file) throws FileServerConnectorException {
        /*FileContent content;
        String fileURI;

        String fileName = file.getName().getBaseName();
        String filePath = file.getName().getPath();
        fileURI = file.getName().getURI();
        try {
            content = file.getContent();
        } catch (FileSystemException e) {
            throw new FileServerConnectorException(
                    "Could not read content of file at URI: " + maskURLPassword(fileURI) + ". ", e);
        }

        InputStream inputStream;
        try {
            inputStream = content.getInputStream();
        } catch (FileSystemException e) {
            throw new FileServerConnectorException("Error occurred when trying to get " +
                                                   "input stream from file at URI :" +
                                                   maskURLPassword(fileURI), e);
        }
        CarbonMessage cMessage = new StreamingCarbonMessage(inputStream);
        cMessage.setProperty(org.wso2.carbon.messaging.Constants.PROTOCOL, Constants.PROTOCOL_NAME);
        cMessage.setProperty(Constants.FILE_TRANSPORT_PROPERTY_SERVICE_NAME, serviceName);
        cMessage.setHeader(Constants.FILE_PATH, filePath);
        cMessage.setHeader(Constants.FILE_NAME, fileName);
        cMessage.setHeader(Constants.FILE_URI, fileURI);
        try {
            cMessage.setHeader(Constants.FILE_LENGTH, Long.toString(content.getSize()));
            cMessage.setHeader(Constants.LAST_MODIFIED, Long.toString(content.getLastModifiedTime()));
        } catch (FileSystemException e) {
            log.warn("Unable to set file length or last modified date header.", e);
        }*/

        CarbonMessage cMessage = new TextCarbonMessage(file.getName().getURI());
        cMessage.setProperty(org.wso2.carbon.messaging.Constants.PROTOCOL, Constants.PROTOCOL_NAME);
        cMessage.setProperty(Constants.FILE_TRANSPORT_PROPERTY_SERVICE_NAME, serviceName);

        FileServerConnectorCallback callback = new FileServerConnectorCallback();
        try {
            messageProcessor.receive(cMessage, callback);
        } catch (Exception e) {
            throw new FileServerConnectorException("Failed to send stream from file: " + maskURLPassword(fileURI) +
                                                   " to message processor. ", e);
        }
        try {
            callback.waitTillDone(timeOutInterval, deleteIfNotAck, fileURI);
        } catch (InterruptedException e) {
            throw new FileServerConnectorException("Interrupted while waiting for message processor to consume" +
                                                   " the file input stream. Aborting processing of file: " +
                                                   maskURLPassword(fileURI), e);
        }
        return file;
    }

    /**
     * Do the post processing actions.
     *
     * @param fileObject
     */
    private void postProcess(FileObject fileObject) throws FileServerConnectorException {
        String moveToDirectoryURI = null;
        try {
            if (Constants.ACTION_MOVE.equalsIgnoreCase(fileProperties.get(Constants.ACTION_AFTER_PROCESS))) {
                moveToDirectoryURI = fileProperties.get(Constants.MOVE_AFTER_PROCESS);
                if (moveToDirectoryURI == null) {
                    log.warn("Cannot move file because moveAfterProcess annotation is not defined.");
                }
            }
            if (moveToDirectoryURI != null) {
                FileObject moveToDirectory;
                moveToDirectory = fsManager.resolveFile(moveToDirectoryURI, fso);

                String prefix;
                if (fileProperties.get(Constants.MOVE_TIMESTAMP_FORMAT) != null) {
                    prefix = new SimpleDateFormat(fileProperties.get(Constants.MOVE_TIMESTAMP_FORMAT))
                            .format(new Date());
                } else {
                    prefix = "";
                }

                //Forcefully create the folder(s) if does not exists
                String strForceCreateFolder = fileProperties.get(Constants.FORCE_CREATE_FOLDER);
                if (strForceCreateFolder != null && strForceCreateFolder.equalsIgnoreCase("true") &&
                    !moveToDirectory.exists()) {
                    moveToDirectory.createFolder();
                }

                FileObject dest = moveToDirectory.resolveFile(prefix + fileObject.getName().getBaseName());
                if (log.isDebugEnabled()) {
                    log.debug("Moving to file :" + maskURLPassword(dest.getName().getURI()));
                }
                try {
                    fileObject.moveTo(dest);
                    /*if (FileTransportUtils.isFailRecord(fsManager, fileObject)) {
                        VFSUtils.releaseFail(fsManager, fileObject);
                    }*/
                } catch (FileSystemException e) {
                    /*if (!VFSUtils.isFailRecord(fsManager, fileObject)) {
                        VFSUtils.markFailRecord(fsManager, fileObject);
                    }*/
                    log.error("Error moving file : " + maskURLPassword(fileObject.toString()) +
                              " to " + maskURLPassword(moveToDirectoryURI), e);
                }

            } else {

                if (log.isDebugEnabled()) {
                    log.debug("Deleting file :" + maskURLPassword(fileObject.getName().getBaseName()));
                }
                try {
                    if (!fileObject.delete()) {
                        throw new FileServerConnectorException(
                                "Could not delete file : " + maskURLPassword(fileObject.getName().getBaseName()));
                    }
                } catch (FileSystemException e) {
                    throw new FileServerConnectorException(
                            "Could not delete file : " + maskURLPassword(fileObject.getName().getBaseName()), e);
                }
            }
        } catch (FileSystemException e) {
            throw new FileServerConnectorException("Failed to resolve file post process.", e);
        }
    }

    /**
     * Acquire the file level locking.
     *
     * @param fsManager
     * @param fileObject
     * @return
     */
    private boolean acquireLock(FileSystemManager fsManager, FileObject fileObject) {
        String strContext = fileObject.getName().getURI();
        boolean rtnValue = false;

        // When processing a directory list is fetched initially. Therefore
        // there is still a chance of file processed by another process.
        // Need to check the source file before processing.
        try {
            String parentURI = fileObject.getParent().getName().getURI();
            if (parentURI.contains("?")) {
                String suffix = parentURI.substring(parentURI.indexOf("?"));
                strContext += suffix;
            }
            FileObject sourceFile = fsManager.resolveFile(strContext);
            if (!sourceFile.exists()) {
                return false;
            }
        } catch (FileSystemException e) {
            return false;
        }
        /*VFSParamDTO vfsParamDTO = new VFSParamDTO();
        vfsParamDTO.setAutoLockRelease(autoLockRelease);
        vfsParamDTO.setAutoLockReleaseSameNode(autoLockReleaseSameNode);
        vfsParamDTO.setAutoLockReleaseInterval(autoLockReleaseInterval);
        rtnValue = VFSUtils.acquireLock(fsManager, fileObject, vfsParamDTO, fso, true);*/

        /* finally {
            if (distributedLock) {
                ClusteringServiceUtil.releaseLock(strContext);
            }
        }*/
        //return rtnValue;

        String stringSplitter = ":";
        // generate a random lock value to ensure that there are no two parties
        // processing the same file
        Random random = new Random();
        // Lock format random:hostname:hostip:time
        String strLockValue = String.valueOf(random.nextLong());
        try {
            strLockValue += stringSplitter + InetAddress.getLocalHost().getHostName();
            strLockValue += stringSplitter + InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ue) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to get the Hostname or IP.");
            }
        }
        strLockValue += stringSplitter + (new Date()).getTime();
        byte[] lockValue = strLockValue.getBytes(Charset.defaultCharset());
        FileObject lockObject = null;

        try {
            // check whether there is an existing lock for this item, if so it is assumed
            // to be processed by an another listener (downloading) or a sender (uploading)
            // lock file is derived by attaching the ".lock" second extension to the file name
            String fullPath = fileObject.getName().getURI();
            int pos = fullPath.indexOf("?");
            if (pos != -1) {
                fullPath = fullPath.substring(0, pos);
            }
            lockObject = fsManager.resolveFile(fullPath + ".lock", fso);
            if (lockObject.exists()) {
                log.debug("There seems to be an external lock, aborting the processing of the file " +
                          maskURLPassword(fileObject.getName().getURI()) +
                          ". This could possibly be due to some other party already " +
                          "processing this file or the file is still being uploaded");
            /*if(paramDTO != null && paramDTO.isAutoLockRelease()){
                releaseLock(lockValue, strLockValue, lockObject, paramDTO.isAutoLockReleaseSameNode(),
                            paramDTO.getAutoLockReleaseInterval());
            }*/
            } else {
                //Check the original file existence before the lock file to handle concurrent access scenario
                FileObject originalFileObject = fsManager.resolveFile(fullPath, fso);
                if (!originalFileObject.exists()) {
                    return false;
                }
                // write a lock file before starting of the processing, to ensure that the
                // item is not processed by any other parties
                lockObject.createFile();
                OutputStream stream = lockObject.getContent().getOutputStream();
                try {
                    stream.write(lockValue);
                    stream.flush();
                    stream.close();
                } catch (IOException e) {
                    lockObject.delete();
                    log.error("Couldn't create the lock file before processing the file " + maskURLPassword(fullPath),
                              e);
                    return false;
                } finally {
                    lockObject.close();
                }

                // check whether the lock is in place and is it me who holds the lock. This is
                // required because it is possible to write the lock file simultaneously by
                // two processing parties. It checks whether the lock file content is the same
                // as the written random lock value.
                // NOTE: this may not be optimal but is sub optimal
                FileObject verifyingLockObject = fsManager.resolveFile(fullPath + ".lock", fso);
                if (verifyingLockObject.exists() && verifyLock(lockValue, verifyingLockObject)) {
                    return true;
                }
            }
        } catch (FileSystemException fse) {
            log.error("Cannot get the lock for the file : " + maskURLPassword(fileObject.getName().getURI()) +
                      " before processing", fse);
            if (lockObject != null) {
                try {
                    fsManager.closeFileSystem(lockObject.getParent().getFileSystem());
                } catch (FileSystemException e) {
                    log.warn("Unable to close the lockObject parent file system");
                }
            }
        }
        return false;
    }

    /**
     * Release a file item lock acquired either by the VFS listener or a sender.
     *
     * @param fsManager which is used to resolve the processed file
     * @param fo        representing the processed file
     * @param fso       represents file system options used when resolving file from file system manager.
     */
    public static void releaseLock(FileSystemManager fsManager, FileObject fo, FileSystemOptions fso) {
        String fullPath = fo.getName().getURI();

        try {
            int pos = fullPath.indexOf("?");
            if (pos > -1) {
                fullPath = fullPath.substring(0, pos);
            }
            FileObject lockObject = fsManager.resolveFile(fullPath + ".lock", fso);
            if (lockObject.exists()) {
                lockObject.delete();
            }
        } catch (FileSystemException e) {
            log.error("Couldn't release the lock for the file : " + maskURLPassword(fo.getName().getURI()) +
                      " after processing");
        }
    }

    private static boolean verifyLock(byte[] lockValue, FileObject lockObject) {
        try {
            InputStream is = lockObject.getContent().getInputStream();
            byte[] val = new byte[lockValue.length];
            int data;
            int i = 0;
            while ((data = is.read()) != -1) {
                val[i++] = (byte) data;
            }
            if (lockValue.length == val.length && Arrays.equals(lockValue, val)) {
                return true;
            } else {
                log.debug("The lock has been acquired by an another party");
            }
        } catch (FileSystemException e) {
            log.error("Couldn't verify the lock", e);
            return false;
        } catch (IOException e) {
            log.error("Couldn't verify the lock", e);
            return false;
        }
        return false;
    }

    /**
     * Comparator classes used to sort the files according to user input.
     */
    static class FileNameAscComparator implements Comparator<FileObject>, Serializable {

        private static final long serialVersionUID = 1;

        @Override public int compare(FileObject o1, FileObject o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    static class FileLastmodifiedtimestampAscComparator implements Comparator<FileObject>, Serializable {

        private static final long serialVersionUID = 1;

        @Override public int compare(FileObject o1, FileObject o2) {
            Long lDiff = 0L;
            try {
                lDiff = o1.getContent().getLastModifiedTime() - o2.getContent().getLastModifiedTime();
            } catch (FileSystemException e) {
                log.warn("Unable to compare last modified timestamp of the two files.", e);
            }
            return lDiff.intValue();
        }
    }

    static class FileSizeAscComparator implements Comparator<FileObject>, Serializable {

        private static final long serialVersionUID = 1;

        @Override public int compare(FileObject o1, FileObject o2) {
            Long lDiff = 0L;
            try {
                lDiff = o1.getContent().getSize() - o2.getContent().getSize();
            } catch (FileSystemException e) {
                log.warn("Unable to compare size of the two files.", e);
            }
            return lDiff.intValue();
        }
    }

    static class FileNameDesComparator implements Comparator<FileObject>, Serializable {

        private static final long serialVersionUID = 1;

        @Override public int compare(FileObject o1, FileObject o2) {
            return o2.getName().compareTo(o1.getName());
        }
    }

    static class FileLastmodifiedtimestampDesComparator implements Comparator<FileObject>, Serializable {

        private static final long serialVersionUID = 1;

        @Override public int compare(FileObject o1, FileObject o2) {
            Long lDiff = 0L;
            try {
                lDiff = o2.getContent().getLastModifiedTime() - o1.getContent().getLastModifiedTime();
            } catch (FileSystemException e) {
                log.warn("Unable to compare last modified timestamp of the two files.", e);
            }
            return lDiff.intValue();
        }
    }

    static class FileSizeDesComparator implements Comparator<FileObject>, Serializable {

        private static final long serialVersionUID = 1;

        @Override public int compare(FileObject o1, FileObject o2) {
            Long lDiff = 0L;
            try {
                lDiff = o2.getContent().getSize() - o1.getContent().getSize();
            } catch (FileSystemException e) {
                log.warn("Unable to compare size of the two files.", e);
            }
            return lDiff.intValue();
        }
    }

}
