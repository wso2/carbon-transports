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

import org.apache.commons.lang.WordUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.UriParser;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.file.connector.server.exception.FileServerConnectorException;
import org.wso2.carbon.transport.file.connector.server.util.Constants;
import org.wso2.carbon.transport.file.connector.server.util.FileTransportUtils;
import org.wso2.carbon.transport.file.message.StreamingCarbonMessage;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is able to poll for a file and consume it once it is available.
 */
public class FileConsumer {

    private static final Log log = LogFactory.getLog(FileConsumer.class);
    private Map<String, String> fileProperties;
    private boolean fileLock = true;
    private FileSystemManager fsManager = null;
    private String serviceName;
    private int lastCycle;

    private CarbonMessageProcessor messageProcessor;

    private String fileURI;
    private FileObject fileObject;
    private Integer iFileProcessingInterval = null;
    private Integer iFileProcessingCount = null;
    private String strFilePattern;
    private boolean autoLockRelease;
    private Boolean autoLockReleaseSameNode;
    private Long autoLockReleaseInterval;
    private FileSystemOptions fso;

    public FileConsumer(String id, Map<String, String> fileProperties,
                        CarbonMessageProcessor messageProcessor)
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
            throw new ServerConnectorException("Could not initialize File System Manager from " +
                    "the configuration: providers.xml", e);
        }
        fso = FileTransportUtils.attachFileSystemOptions(parseSchemeFileOptions(fileURI), fsManager);
        try {
            fileObject = fsManager.resolveFile(fileURI, fso);
        } catch (FileSystemException e) {
            throw new FileServerConnectorException("Failed to resolve fileURI: "
                    + FileTransportUtils.maskURLPassword(fileURI), e);
        }
    }

    /**
     * Do the file processing operation for the given set of properties. Do the
     * checks and pass the control to processFile method
     */
    public FileObject consume() throws FileServerConnectorException {
        if (log.isDebugEnabled()) {
            log.debug("Polling for directory or file : " +
                    FileTransportUtils.maskURLPassword(fileURI));
        }

        // If file/folder found proceed to the processing stage
        try {
            lastCycle = 0;
            boolean isFileExists;
            try {
                isFileExists = fileObject.exists();
            } catch (FileSystemException e1) {
                log.error("Error occurred when determining whether the file at URI : "
                        + FileTransportUtils.maskURLPassword(fileURI) + " exists. " + e1.getMessage());
                return null;    //// TODO: 2/8/17 throw an exception and return, rather than returning with a null.
            }

            boolean isFileReadable;
            try {
                isFileReadable = fileObject.isReadable();
            } catch (FileSystemException e1) {
                log.error("Error occurred when determining whether the file at URI : "
                        + FileTransportUtils.maskURLPassword(fileURI) + " is readable. " + e1.getMessage());
                return null;
            }
            if (isFileExists && isFileReadable) {
                FileObject[] children = null;
                try {
                    children = fileObject.getChildren();
                } catch (FileSystemException ignored) {
                    if (log.isDebugEnabled()) {
                        log.debug("The file does not exist, or is not a folder, or an error " +
                                "has occurred when trying to list the children. File URI : "
                                + FileTransportUtils.maskURLPassword(fileURI));
                    }
                }

                // if this is a file that would translate to a single message
                if (children == null || children.length == 0) {
                    processFile(fileObject);
                    deleteFile(fileObject);
                } else {
                    FileObject fileObject;
                    fileObject = directoryHandler(children);
                    if (fileObject != null) {
                        return fileObject;
                    }
                }
            } else {
                log.warn("Unable to access or read file or directory : "
                        + FileTransportUtils.maskURLPassword(fileURI)
                        + ". Reason: "
                        + (isFileExists ? (isFileReadable ? "Unknown reason"
                        : "The file can not be read!") : "The file does not exist!"));
                return null;
            }
        } finally {
            try {
                if (fsManager != null) {
                    fsManager.closeFileSystem(fileObject.getParent().getFileSystem());
                }
                fileObject.close();
            } catch (Exception e) {
                log.error("Unable to close the file system. " + e.getMessage());
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("End : Scanning directory or file : " + FileTransportUtils.maskURLPassword(fileURI));
        }
        return null;
    }

    /**
     * If not a folder just a file handle the flow
     *
     * @throws FileSystemException
     */
    private void fileHandler() throws FileServerConnectorException {
        FileType fileType;
        try {
            fileType = fileObject.getType();
        } catch (FileSystemException e) {
            throw new FileServerConnectorException("Could not get file type for file: " +
                    fileObject.getName().toString() + e.getMessage());
        }
        if (fileType == FileType.FILE) {
            if (!fileLock || acquireLock(fsManager, fileObject)) {
                boolean runPostProcess = true;
                FileObject fo = null;
                fo = processFile(fileObject);
                if (fo == null) {
                    runPostProcess = false;
                }
                lastCycle = 1;

                if (runPostProcess) {
                    try {
                        deleteFile(fileObject);
                    } catch (FileServerConnectorException e) {
                        lastCycle = 3;
                        FileTransportUtils.markFailRecord(fsManager, fileObject);
                        throw new FileServerConnectorException("File object '" + fileObject.getName().toString() + "' "
                                + "could not be moved. " + e.getMessage());
                    }
                }
                if (fileLock) {
                    FileTransportUtils.releaseLock(fsManager, fileObject, fso);
                    if (log.isDebugEnabled()) {
                        log.debug("Removed the lock file '" +
                                FileTransportUtils.maskURLPassword(fileObject.toString())
                                + ".lock' of the file '" +
                                FileTransportUtils.maskURLPassword(fileObject.toString()));
                    }
                }
            } else {
                throw new FileServerConnectorException("Couldn't get the lock for processing the file : " +
                        FileTransportUtils.maskURLPassword(fileObject.getName().toString()));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Cannot find the file or failed file record. File : "
                        + FileTransportUtils.maskURLPassword(fileURI));
            }
        }
    }

    /**
     * Setup the required transport parameters
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

        String strFileLock = fileProperties.get(Constants.TRANSPORT_FILE_LOCKING);
        if (strFileLock != null
                && strFileLock.equalsIgnoreCase(Constants.TRANSPORT_FILE_LOCKING_DISABLED)) {
            fileLock = false;
        }

        strFilePattern = fileProperties.get(Constants.TRANSPORT_FILE_FILE_NAME_PATTERN);
        if (fileProperties.get(Constants.TRANSPORT_FILE_INTERVAL) != null) {
            try {
                iFileProcessingInterval = Integer.valueOf(fileProperties
                        .get(Constants.TRANSPORT_FILE_INTERVAL));
            } catch (NumberFormatException e) {
                log.warn("Invalid parameter value given for " + Constants.TRANSPORT_FILE_INTERVAL + " : "
                        + fileProperties.get(Constants.TRANSPORT_FILE_INTERVAL)
                        + ". Expected numeric value. " + e.getMessage());
            }
        }
        if (fileProperties.get(Constants.TRANSPORT_FILE_COUNT) != null) {
            try {
                iFileProcessingCount = Integer.valueOf(fileProperties
                        .get(Constants.TRANSPORT_FILE_COUNT));
            } catch (NumberFormatException e) {
                log.warn("Invalid param value for " + Constants.TRANSPORT_FILE_COUNT + " : "
                        + fileProperties.get(Constants.TRANSPORT_FILE_COUNT)
                        + ". Expected numeric value. " + e.getMessage());
            }
        }

        String strAutoLock = fileProperties.get(Constants.TRANSPORT_AUTO_LOCK_RELEASE);
        autoLockRelease = false;
        autoLockReleaseSameNode = true;
        autoLockReleaseInterval = null;
        if (strAutoLock != null) {
            try {
                autoLockRelease = Boolean.parseBoolean(strAutoLock);
            } catch (Exception e) {
                autoLockRelease = false;
                log.warn(Constants.TRANSPORT_AUTO_LOCK_RELEASE + " property should be" +
                        " a boolean. Given value is : " + strAutoLock + ". " + e.getMessage());
            }
            if (autoLockRelease) {
                String strAutoLockInterval = fileProperties
                        .get(Constants.TRANSPORT_AUTO_LOCK_RELEASE_INTERVAL);
                if (strAutoLockInterval != null) {
                    try {
                        autoLockReleaseInterval = Long.parseLong(strAutoLockInterval);
                    } catch (Exception e) {
                        autoLockReleaseInterval = null;
                        log.warn(Constants.TRANSPORT_AUTO_LOCK_RELEASE_INTERVAL + " property " +
                                "should be a Long. Given value is : " + strAutoLockInterval + ". "
                                + e.getMessage());
                    }
                }
                String strAutoLockReleaseSameNode = fileProperties
                        .get(Constants.TRANSPORT_AUTO_LOCK_RELEASE_SAME_NODE);
                if (strAutoLockReleaseSameNode != null) {
                    try {
                        autoLockReleaseSameNode = Boolean.parseBoolean(strAutoLockReleaseSameNode);
                    } catch (Exception e) {
                        autoLockReleaseSameNode = true;
                        log.warn(Constants.TRANSPORT_AUTO_LOCK_RELEASE_SAME_NODE +
                                " property should be a boolean. Given value is : "
                                + strAutoLockReleaseSameNode + e.getMessage());
                    }
                }
            }

        }
    }

    private Map<String, String> parseSchemeFileOptions(String fileURI) {
        String scheme = UriParser.extractScheme(fileURI);
        if (scheme == null) {
            return null;
        }
        HashMap<String, String> schemeFileOptions = new HashMap<String, String>();
        schemeFileOptions.put(Constants.SCHEME, scheme);
        addOptions(scheme, schemeFileOptions);
        return schemeFileOptions;
    }

    private void addOptions(String scheme, Map<String, String> schemeFileOptions) {
        if (scheme.equals(Constants.SCHEME_SFTP)) {
            for (Constants.SftpFileOption option : Constants.SftpFileOption.values()) {
                String strValue = fileProperties.get(Constants.SFTP_PREFIX
                        + WordUtils.capitalize(option.toString()));
                if (strValue != null && !strValue.equals("")) {
                    schemeFileOptions.put(option.toString(), strValue);
                }
            }
        }
    }

    /**
     * Handle directory with chile elements
     *
     * @param children
     * @return
     * @throws FileSystemException
     */
    private FileObject directoryHandler(FileObject[] children) throws FileServerConnectorException {
        // Sort the files
        String strSortParam = fileProperties.get(Constants.FILE_SORT_PARAM);
        if (strSortParam != null && !"NONE".equals(strSortParam)) {
            log.debug("Start sorting the files.");
            String strSortOrder = fileProperties.get(Constants.FILE_SORT_ORDER);
            boolean bSortOrderAscending = true;
            if (strSortOrder != null) {
                try {
                    bSortOrderAscending = Boolean.parseBoolean(strSortOrder);
                } catch (RuntimeException re) {
                    log.warn(Constants.FILE_SORT_ORDER + " parameter should be either " +
                            "\"true\" or \"false\". Found: " + strSortOrder +
                            ". Assigning default value \"true\"." + re.getMessage());
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Sorting the files by : " + strSortOrder + ". (" +
                        bSortOrderAscending + ")");
            }
            if (strSortParam.equals(Constants.FILE_SORT_VALUE_NAME) && bSortOrderAscending) {
                Arrays.sort(children, new FileNameAscComparator());
            } else if (strSortParam.equals(Constants.FILE_SORT_VALUE_NAME)
                    && !bSortOrderAscending) {
                Arrays.sort(children, new FileNameDesComparator());
            } else if (strSortParam.equals(Constants.FILE_SORT_VALUE_SIZE)
                    && bSortOrderAscending) {
                Arrays.sort(children, new FileSizeAscComparator());
            } else if (strSortParam.equals(Constants.FILE_SORT_VALUE_SIZE)
                    && !bSortOrderAscending) {
                Arrays.sort(children, new FileSizeDesComparator());
            } else if (strSortParam.equals(Constants.FILE_SORT_VALUE_LASTMODIFIEDTIMESTAMP)
                    && bSortOrderAscending) {
                Arrays.sort(children, new FileLastmodifiedtimestampAscComparator());
            } else if (strSortParam.equals(Constants.FILE_SORT_VALUE_LASTMODIFIEDTIMESTAMP)
                    && !bSortOrderAscending) {
                Arrays.sort(children, new FileLastmodifiedtimestampDesComparator());
            }
            log.debug("End Sorting the files.");
        }

        for (FileObject child : children) {
            processFile(child);
            deleteFile(child);

            //close the file system after processing
            try {
                child.close();
            } catch (Exception e) {
                log.warn("Could not close the file: " + child.getName().getPath());
                //todo: debug child.getName().getPath()
            }

        }
        return null;
    }

    /**
     * Do the file level locking
     *
     * @param fsManager
     * @param fileObject
     * @return
     */
    private boolean acquireLock(FileSystemManager fsManager, FileObject fileObject) {
        String strContext = fileObject.getName().getURI();
        boolean rtnValue;
        // When processing a directory list is fetched initially. Therefore
        // there is still a chance of file processed by another process.
        // Need to check the source file before processing.
        String parentURI = null;
        try {
            parentURI = fileObject.getParent().getName().getURI();
        } catch (FileSystemException e) {
            log.error("Error occurred when trying to get parent of file: "
                    + fileObject.getName().toString() + ". " + e.getMessage());
            return false;
        }
        if (parentURI.contains("?")) {
            String suffix = parentURI.substring(parentURI.indexOf("?"));
            strContext += suffix;
        }
        FileObject sourceFile;
        try {
            sourceFile = fsManager.resolveFile(strContext);
        } catch (FileSystemException e) {
            log.error("Error occurred when trying to resolve file: " + strContext
                    + ". " + e.getMessage());
            return false;
        }
        boolean isSourceFileExist = false;
        try {
            isSourceFileExist = sourceFile.exists();
        } catch (FileSystemException e) {
            log.error("Error occrred when trying to check whether file : "
                    + sourceFile.getName().toString() + " exists. " + e.getMessage());
        }
        if (!isSourceFileExist) {
            return false;
        }
        LockParamsDTO lockingParamsDTO = new LockParamsDTO();
        lockingParamsDTO.setAutoLockRelease(autoLockRelease);
        lockingParamsDTO.setAutoLockReleaseSameNode(autoLockReleaseSameNode);
        lockingParamsDTO.setAutoLockReleaseInterval(autoLockReleaseInterval);
        rtnValue = FileTransportUtils.acquireLock(fsManager, fileObject, lockingParamsDTO,
                fso, true);
        return rtnValue;
    }

    /**
     * Actual processing of the file/folder
     *
     * @param file
     * @return
     */
    private FileObject processFile(FileObject file) throws FileServerConnectorException {
        FileContent content;
        String fileURI = null;

        String fileName = file.getName().getBaseName();
        String filePath = file.getName().getPath();
        fileURI = file.getName().getURI();

        try {
            content = file.getContent();
        } catch (FileSystemException e) {
            throw new FileServerConnectorException("Could not read content of file at URI: "
                    + fileURI + ". " + e.getMessage());
        }

        Map<String, Object> transportHeaders = new HashMap<>();
        transportHeaders.put(Constants.FILE_PATH, filePath);
        transportHeaders.put(Constants.FILE_NAME, fileName);
        transportHeaders.put(Constants.FILE_URI, fileURI);

        try {
            transportHeaders.put(Constants.FILE_LENGTH, content.getSize());
            transportHeaders.put(Constants.LAST_MODIFIED, content.getLastModifiedTime());
        } catch (FileSystemException e) {
            log.warn("Unable to set file length or last modified date header. "
                    + e.getMessage());
        }

        InputStream inputStream = null;
        try {
            inputStream = content.getInputStream();
        } catch (FileSystemException e) {
            throw new FileServerConnectorException("Error occurred when trying to get " +
                    "input stream from file at URI :" + fileURI + ". " + e.getMessage());
        }
        CarbonMessage cMessage = new StreamingCarbonMessage(inputStream);
        cMessage.setProperty(org.wso2.carbon.messaging.Constants.PROTOCOL, Constants.PROTOCOL_NAME);
        cMessage.setProperty(Constants.FILE_TRANSPORT_PROPERTY_SERVICE_NAME, serviceName);
        cMessage.setProperty("TRANSPORT_HEADERS", transportHeaders);
        FileServerConnectorCallback callback = new FileServerConnectorCallback();
        try {
            messageProcessor.receive(cMessage, callback);
        } catch (Exception e) {
            throw new FileServerConnectorException("Failed to send stream from file: "
                    + fileURI + " to message processor. " + e.getMessage());
        }
        try {
            callback.waitTillDone();
        } catch (InterruptedException e) {
            throw new FileServerConnectorException("Error occurred while waiting for message " +
                    "processor to consume the file input stream. Input stream may be closed " +
                    "before the Message processor reads it. " + e.getMessage());
        }
        return file;
    }

    /**
     * Do the post processing actions
     *
     * @param fileObject
     */
    private void deleteFile(FileObject fileObject) throws FileServerConnectorException {
        if (log.isDebugEnabled()) {
            log.debug("Deleting file :" + FileTransportUtils.
                    maskURLPassword(fileObject.getName().getBaseName()));
        }
        try {
            fileObject.close();
        } catch (FileSystemException e) {
            throw new FileServerConnectorException("Could not close file : "
                    + FileTransportUtils.maskURLPassword(fileObject.getName().getBaseName()));
        }
        try {
            if (!fileObject.delete()) {
                throw new FileServerConnectorException("Could not delete file : "
                        + FileTransportUtils.maskURLPassword(fileObject.getName().getBaseName()));
            }
        } catch (FileSystemException e) {
            throw new FileServerConnectorException("Could not delete file : "
                    + FileTransportUtils.maskURLPassword(fileObject.getName().getBaseName()));
        }
    }

    /**
     * Comparator classed used to sort the files according to user input
     */
    static class FileNameAscComparator implements Comparator<FileObject> {
        @Override
        public int compare(FileObject o1, FileObject o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    static class FileLastmodifiedtimestampAscComparator implements Comparator<FileObject> {
        @Override
        public int compare(FileObject o1, FileObject o2) {
            Long lDiff = 0L;
            try {
                lDiff = o1.getContent().getLastModifiedTime()
                        - o2.getContent().getLastModifiedTime();
            } catch (FileSystemException e) {
                log.warn("Unable to compare lastmodified timestamp of the two files.", e);
            }
            return lDiff.intValue();
        }
    }

    static class FileSizeAscComparator implements Comparator<FileObject> {
        @Override
        public int compare(FileObject o1, FileObject o2) {
            Long lDiff = 0L;
            try {
                lDiff = o1.getContent().getSize() - o2.getContent().getSize();
            } catch (FileSystemException e) {
                log.warn("Unable to compare size of the two files.", e);
            }
            return lDiff.intValue();
        }
    }

    static class FileNameDesComparator implements Comparator<FileObject> {
        @Override
        public int compare(FileObject o1, FileObject o2) {
            return o2.getName().compareTo(o1.getName());
        }
    }

    static class FileLastmodifiedtimestampDesComparator implements Comparator<FileObject> {
        @Override
        public int compare(FileObject o1, FileObject o2) {
            Long lDiff = 0L;
            try {
                lDiff = o2.getContent().getLastModifiedTime()
                        - o1.getContent().getLastModifiedTime();
            } catch (FileSystemException e) {
                log.warn("Unable to compare lastmodified timestamp of the two files.", e);
            }
            return lDiff.intValue();
        }
    }

    static class FileSizeDesComparator implements Comparator<FileObject> {
        @Override
        public int compare(FileObject o1, FileObject o2) {
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
