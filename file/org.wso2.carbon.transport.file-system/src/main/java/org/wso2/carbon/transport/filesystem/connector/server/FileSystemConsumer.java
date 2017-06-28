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

package org.wso2.carbon.transport.filesystem.connector.server;

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
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.filesystem.connector.server.exception.FileSystemServerConnectorException;
import org.wso2.carbon.transport.filesystem.connector.server.util.Constants;
import org.wso2.carbon.transport.filesystem.connector.server.util.FileTransportUtils;
import org.wso2.carbon.transport.filesystem.connector.server.util.ThreadPoolFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides the capability to process a file and move/delete it afterwards.
 */
public class FileSystemConsumer {

    private static final Logger log = LoggerFactory.getLogger(FileSystemConsumer.class);

    private Map<String, String> fileProperties;
    private FileSystemManager fsManager = null;
    private String serviceName;
    private CarbonMessageProcessor messageProcessor;
    private String fileURI;
    private FileObject fileObject;
    private FileSystemOptions fso;
    private boolean parallelProcess = false;
    private int threadPoolSize = 10;
    private int fileProcessCount;
    private int processCount;
    /**
     * Time-out interval (in milli-seconds) to wait for the callback.
     */
    private long timeOutInterval = 30000;
    private boolean continueIfNotAck = false;
    private String fileNamePattern = null;
    private String postProcessAction = Constants.ACTION_DELETE;
    private String postFailureAction = Constants.ACTION_DELETE;
    private boolean createRecoveryFiles = true;

    private List<String> processed = new ArrayList<>();
    private List<String> failed = new ArrayList<>();

    /**
     * Constructor for the FileSystemConsumer.
     *
     * @param id                Name of the service that creates the consumer
     * @param fileProperties    Map of property values
     * @param messageProcessor  Message processor instance
     */
    FileSystemConsumer(String id, Map<String, String> fileProperties, CarbonMessageProcessor messageProcessor)
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
            throw new FileSystemServerConnectorException("Failed to resolve fileURI: "
                                                         + FileTransportUtils.maskURLPassword(fileURI), e);
        }
        try {
            if (!fileObject.isWriteable()) {
                postProcessAction = Constants.ACTION_NONE;
            }
        } catch (FileSystemException e) {
            throw new FileSystemServerConnectorException(
                    "Exception while determining file: " + FileTransportUtils.maskURLPassword(fileURI) + " is readable",
                    e);
        }
        FileType fileType = getFileType(fileObject);
        if (fileType != FileType.FOLDER) {
            throw new FileSystemServerConnectorException("File system server connector is used to listen to a" +
                                                         " folder. But the given path does not refer to a folder.");
        }
        //Initialize the thread executor based on properties
        ThreadPoolFactory.createInstance(threadPoolSize, parallelProcess);
    }

    /**
     * Setup the required transport parameters from properties provided.
     */
    private void setupParams() throws ServerConnectorException {
        fileURI = fileProperties.get(Constants.TRANSPORT_FILE_FILE_URI);
        if (fileURI == null) {
            throw new ServerConnectorException(
                    Constants.TRANSPORT_FILE_FILE_URI + " is a " + "mandatory parameter for " +
                    Constants.PROTOCOL_FILE_SYSTEM + " transport.");
        }
        if (fileURI.trim().equals("")) {
            throw new ServerConnectorException(Constants.TRANSPORT_FILE_FILE_URI + " parameter " +
                                               "cannot be empty for " + Constants.PROTOCOL_FILE_SYSTEM + " transport.");
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
        String strContIfNotAck = fileProperties.get(Constants.FILE_CONT_IF_NOT_ACKNOWLEDGED);
        if (strContIfNotAck != null) {
            continueIfNotAck = Boolean.parseBoolean(strContIfNotAck);
        }
        String strParallel = fileProperties.get(Constants.PARALLEL);
        if (strParallel != null) {
            parallelProcess = Boolean.parseBoolean(strParallel);
        }
        String strPoolSize = fileProperties.get(Constants.THREAD_POOL_SIZE);
        if (strPoolSize != null) {
            threadPoolSize = Integer.parseInt(strPoolSize);
        }
        String strProcessCount = fileProperties.get(Constants.FILE_PROCESS_COUNT);
        if (strProcessCount != null) {
            fileProcessCount = Integer.parseInt(strProcessCount);
        }

        if (fileProperties.get(Constants.ACTION_AFTER_FAILURE) != null) {
            switch (fileProperties.get(Constants.ACTION_AFTER_FAILURE)) {
                case Constants.ACTION_MOVE:
                    postFailureAction = Constants.ACTION_MOVE;
                    break;
                case Constants.ACTION_NONE:
                    postFailureAction = Constants.ACTION_NONE;
                    break;
                default:
                    postFailureAction = Constants.ACTION_DELETE;
            }
        }

        if (fileProperties.get(Constants.ACTION_AFTER_PROCESS) != null) {
            switch (fileProperties.get(Constants.ACTION_AFTER_PROCESS)) {
                case Constants.ACTION_MOVE:
                    postProcessAction = Constants.ACTION_MOVE;
                    break;
                case Constants.ACTION_NONE:
                    postProcessAction = Constants.ACTION_NONE;
                    break;
                default:
                    postProcessAction = Constants.ACTION_DELETE;
            }
        }
        String strRecovery = fileProperties.get(Constants.CREATE_RECOVERY_FILES);
        if (strRecovery != null) {
            createRecoveryFiles = Boolean.parseBoolean(strRecovery);
        }
        String strPattern = fileProperties.get(Constants.FILE_NAME_PATTERN);
        if (strPattern != null) {
            fileNamePattern = strPattern;
        }
    }

    /**
     * Get file options specific to a particular scheme.
     *
     * @param fileURI   URI of file to get file options
     * @return          File options related to scheme.
     */
    private Map<String, String> parseSchemeFileOptions(String fileURI) {
        String scheme = UriParser.extractScheme(fileURI);
        if (scheme == null) {
            return null;
        }
        HashMap<String, String> schemeFileOptions = new HashMap<>();
        schemeFileOptions.put(Constants.SCHEME, scheme);
        if (scheme.equals(Constants.SCHEME_SFTP)) {
            for (Constants.SftpFileOption option : Constants.SftpFileOption.values()) {
                String strValue = fileProperties.get(Constants.SFTP_PREFIX + option.toString());
                if (strValue != null && !strValue.equals("")) {
                    schemeFileOptions.put(option.toString(), strValue);
                }
            }
        }
        return schemeFileOptions;
    }

    /**
     * Do the file processing operation for the given set of properties. Do the
     * checks and pass the control to file system processor thread/threads.
     */
    void consume() throws FileSystemServerConnectorException {
        if (log.isDebugEnabled()) {
            log.debug("Polling for directory or file : " + FileTransportUtils.maskURLPassword(fileURI));
        }
        //Resetting the process count, used to control number of files processed per batch
        processCount = 0;
        // If file/folder found proceed to the processing stage
        try {
            boolean isFileExists;
            boolean isFileReadable;
            try {
                isFileExists = fileObject.exists();
                isFileReadable = fileObject.isReadable();
            } catch (FileSystemException e) {
                throw new FileSystemServerConnectorException("Error occurred when determining" +
                                                             " whether the file at URI : " +
                                                             FileTransportUtils.maskURLPassword(fileURI) +
                                                             " exists and readable. " + e);
            }

            if (isFileExists && isFileReadable) {
                FileObject[] children = null;
                try {
                    children = fileObject.getChildren();
                } catch (FileSystemException ignored) {
                    if (log.isDebugEnabled()) {
                        log.debug("The file does not exist, or is not a folder, or an error " +
                                  "has occurred when trying to list the children. File URI : " +
                                  FileTransportUtils.maskURLPassword(fileURI), ignored);
                    }
                }
                if (children == null || children.length == 0) {
                    if (log.isDebugEnabled()) {
                        log.debug("Folder at " + FileTransportUtils.maskURLPassword(fileURI) + " is empty.");
                    }
                } else {
                    directoryHandler(children);
                }
            } else {
                throw new FileSystemServerConnectorException(
                        "Unable to access or read file or directory : " + FileTransportUtils.maskURLPassword(fileURI) +
                        ". Reason: " + (isFileExists ? "The file can not be read!" : "The file does not exist!"));
            }
        } finally {
            try {
                fileObject.close();
            } catch (FileSystemException e) {
                log.warn("Could not close file at URI: " + FileTransportUtils.maskURLPassword(fileURI), e);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("End : Scanning directory or file : " + FileTransportUtils.maskURLPassword(fileURI));
        }
    }

    /**
     * Handle directory with child elements.
     *
     * @param children The array containing child elements of a folder
     */
    private void directoryHandler(FileObject[] children) throws FileSystemServerConnectorException {
        // Sort the files according to given properties
        String strSortParam = fileProperties.get(Constants.FILE_SORT_PARAM);

        if (strSortParam != null && !"NONE".equals(strSortParam)) {
            log.debug("Starting to sort the files in folder: " + FileTransportUtils.maskURLPassword(fileURI));

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
            if (fileProcessCount != 0 && processCount > fileProcessCount) {
                return;
            }
            if (child.getName().getBaseName().endsWith(".lock") || child.getName().getBaseName().endsWith(".fail")) {
                continue;
            }
            if (!(fileNamePattern == null || child.getName().getBaseName().matches(fileNamePattern))) {
                if (log.isDebugEnabled()) {
                    log.debug("File " + FileTransportUtils.maskURLPassword(fileObject.getName().getBaseName()) +
                              " is not processed because it did not match the specified pattern.");
                }
            } else {
                FileType childType = getFileType(child);
                if (childType == FileType.FOLDER) {
                    FileObject[] c = null;
                    try {
                        c = child.getChildren();
                    } catch (FileSystemException ignored) {
                        if (log.isDebugEnabled()) {
                            log.debug("The file does not exist, or is not a folder, or an error " +
                                      "has occurred when trying to list the children. File URI : " +
                                      FileTransportUtils.maskURLPassword(fileURI), ignored);
                        }
                    }

                    // if this is a file that would translate to a single message
                    if (c == null || c.length == 0) {
                        if (log.isDebugEnabled()) {
                            log.debug("Folder at " + FileTransportUtils.maskURLPassword(child.getName().getURI()) +
                                      " is empty.");
                        }
                    } else {
                        directoryHandler(c);
                    }
                    postProcess(child, false);
                } else {
                    fileHandler(child);
                }
            }
        }
    }

    /**
     * Process a single file.
     *
     * @param file A single file to be processed
     */
    private void fileHandler(FileObject file) {
        String uri = file.getName().getURI();
        synchronized (this) {
            if (postProcessAction.equals(Constants.ACTION_NONE) && processed.contains(uri)) {
                log.debug("The file: " + FileTransportUtils.maskURLPassword(uri) + "is already processed");
                return;
            }
        }
        if (!postProcessAction.equals(Constants.ACTION_NONE) && isFailRecord(file)) {
            // it is a failed record
            try {
                postProcess(file, false);
            } catch (FileSystemServerConnectorException e) {
                log.error("File object '" + FileTransportUtils.maskURLPassword(uri) +
                          "'cloud not complete action " + postProcessAction + ", will remain in \"fail\" state", e);
            }
        } else {
            if (FileTransportUtils.acquireLock(fsManager, file)) {
                log.info("Processing file :" + FileTransportUtils.maskURLPassword(file.getName().getBaseName()));
                FileSystemProcessor fsp =
                        new FileSystemProcessor(messageProcessor, serviceName, file, continueIfNotAck, timeOutInterval,
                                                uri, this, postProcessAction);
                fsp.startProcessThread();
                processCount++;
            } else {
                log.warn("Couldn't get the lock for processing the file : " +
                         FileTransportUtils.maskURLPassword(file.getName().toString()));
            }
        }
    }

    /**
     * Do the post processing actions.
     *
     * @param file The file object which needs to be post processed
     * @param processFailed Whether processing of file failed
     */
    synchronized void postProcess(FileObject file, boolean processFailed) throws FileSystemServerConnectorException {
        String moveToDirectoryURI = null;
        FileType fileType = getFileType(file);
        if (!processFailed) {
            if (postProcessAction.equals(Constants.ACTION_MOVE)) {
                moveToDirectoryURI = fileProperties.get(Constants.MOVE_AFTER_PROCESS);
            }
        } else {
            if (postFailureAction.equals(Constants.ACTION_MOVE)) {
                moveToDirectoryURI = fileProperties.get(Constants.MOVE_AFTER_FAILURE);
            }
        }

        if (moveToDirectoryURI != null) {
            try {
                if (getFileType(fsManager.resolveFile(moveToDirectoryURI)) == FileType.FILE) {
                    moveToDirectoryURI = null;
                    if (processFailed) {
                        postFailureAction = Constants.ACTION_NONE;
                    } else {
                        postProcessAction = Constants.ACTION_NONE;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Cannot move file because provided location is not a folder. File is kept at source");
                    }
                }
            } catch (FileSystemException e) {
                throw new FileSystemServerConnectorException("Error occurred when resolving move destination file: " +
                                                             FileTransportUtils.maskURLPassword(fileURI), e);
            }
        }

        try {
            if (!(moveToDirectoryURI == null || fileType == FileType.FOLDER)) {
                FileObject moveToDirectory;
                String relativeName = file.getName().getURI().split(fileObject.getName().getURI())[1];
                int index = relativeName.lastIndexOf(File.separator);
                moveToDirectoryURI += relativeName.substring(0, index);
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

                FileObject dest = moveToDirectory.resolveFile(prefix + file.getName().getBaseName());
                if (log.isDebugEnabled()) {
                    log.debug("Moving to file :" + FileTransportUtils.maskURLPassword(dest.getName().getURI()));
                }
                log.info("Moving file :" + FileTransportUtils.maskURLPassword(file.getName().getBaseName()));
                try {
                    file.moveTo(dest);
                    if (isFailRecord(file)) {
                        releaseFail(file);
                    }
                } catch (FileSystemException e) {
                    if (!isFailRecord(file)) {
                        markFailRecord(file);
                    }
                    log.error("Error moving file : " + FileTransportUtils.maskURLPassword(file.toString()) +
                              " to " + FileTransportUtils.maskURLPassword(moveToDirectoryURI), e);
                }

            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Deleting file :" + FileTransportUtils.maskURLPassword(file.getName().getBaseName()));
                }
                log.info("Deleting file :" + FileTransportUtils.maskURLPassword(file.getName().getBaseName()));
                try {
                    if (!file.delete()) {
                        if (log.isDebugEnabled()) {
                            log.debug("Could not delete file : " +
                                      FileTransportUtils.maskURLPassword(file.getName().getBaseName()));
                        }
                    } else {
                        if (isFailRecord(file)) {
                            releaseFail(file);
                        }
                    }
                } catch (FileSystemException e) {
                    throw new FileSystemServerConnectorException("Could not delete file : " + FileTransportUtils
                            .maskURLPassword(file.getName().getBaseName()), e);
                }
            }
        } catch (FileSystemException e) {
            if (!isFailRecord(file)) {
                markFailRecord(file);
                log.error("Error resolving directory to move file : " +
                          FileTransportUtils.maskURLPassword(moveToDirectoryURI), e);
            }
        }
    }

    /**
     * Determine whether file object is a file or a folder.
     *
     * @param fileObject    File to get the type of
     * @return              FileType of given file
     */
    private FileType getFileType(FileObject fileObject) throws FileSystemServerConnectorException {
        try {
            return fileObject.getType();
        } catch (FileSystemException e) {
            throw new FileSystemServerConnectorException("Error occurred when determining whether file: " +
                                                         FileTransportUtils
                                                                 .maskURLPassword(fileObject.getName().getURI()) +
                                                         " is a file or a folder", e);
        }
    }

    /**
     * Mark a record as a failed record.
     *
     * @param fo    File to be marked as failed
     */
    private synchronized void markFailRecord(FileObject fo) {
        String fullPath = fo.getName().getURI();
        if (failed.contains(fullPath)) {
            log.debug("File : " + FileTransportUtils.maskURLPassword(fullPath)
                      + " is already marked as a failed record.");
            return;
        }
        failed.add(fullPath);
    }

    /**
     * Determine whether a file is a failed record.
     *
     * @param fo    File to determine whether failed
     * @return      true if file is a failed file
     */
    private boolean isFailRecord(FileObject fo) {
        return failed.contains(fo.getName().getURI());
    }

    /**
     * Releases a file from its failed state.
     *
     * @param fo    File to release from failed state
     */
    private synchronized void releaseFail(FileObject fo) {
        String fullPath = fo.getName().getURI();
        failed.remove(fullPath);
    }

    /**
     * Mark a file as processed.
     *
     * @param uri URI of the file to be named as processed
     */
    synchronized void markProcessed(String uri) {
        processed.add(uri);
    }

    synchronized void saveFileData() throws FileSystemServerConnectorException {
        if (createRecoveryFiles) {
            try {
                if (processed.size() > 0) {
                    Path processedPath =
                            Paths.get(".." + File.separator + "tmp" + File.separator + serviceName + File.separator +
                                      "processed.txt");
                    if (!Files.exists(processedPath)) {
                        Path parent = processedPath.getParent();
                        if (parent != null) {
                            Files.createDirectories(parent);
                        }
                        Files.createFile(processedPath);
                    }
                    Files.write(processedPath, processed);
                }
                if (failed.size() > 0) {
                    Path failedPath =
                            Paths.get(".." + File.separator + "tmp" + File.separator + serviceName + File.separator +
                                      "failed.txt");
                    if (!Files.exists(failedPath)) {
                        Path parent = failedPath.getParent();
                        if (parent != null) {
                            Files.createDirectories(parent);
                        }
                        Files.createFile(failedPath);
                    }
                    Files.write(failedPath, failed);
                }
            } catch (IOException e) {
                throw new FileSystemServerConnectorException("Exception occurred while writing files.");
            }
        }
    }

    synchronized void loadFileData() throws FileSystemServerConnectorException {
        Path processedPath =
                Paths.get(".." + File.separator + "tmp" + File.separator + serviceName + File.separator +
                          "processed.txt");
        Path failedPath = Paths.get(
                ".." + File.separator + "tmp" + File.separator + serviceName + File.separator + "failed.txt");
        try {
            if (postProcessAction.equals(Constants.ACTION_NONE)) {
                if (Files.isReadable(processedPath)) {
                    processed = Files.readAllLines(processedPath);
                }
            }
            if (Files.isReadable(failedPath)) {
                failed = Files.readAllLines(failedPath);
                Files.delete(failedPath);
            }
        } catch (IOException e) {
            throw new FileSystemServerConnectorException("Exception occurred while writing file.");
        }
    }

    /**
     * Comparator classes used to sort the files according to user input.
     */
    private static class FileNameAscComparator implements Comparator<FileObject>, Serializable {

        private static final long serialVersionUID = 1;

        @Override
        public int compare(FileObject o1, FileObject o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    private static class FileLastmodifiedtimestampAscComparator implements Comparator<FileObject>, Serializable {

        private static final long serialVersionUID = 1;

        @Override
        public int compare(FileObject o1, FileObject o2) {
            Long lDiff = 0L;
            try {
                lDiff = o1.getContent().getLastModifiedTime() - o2.getContent().getLastModifiedTime();
            } catch (FileSystemException e) {
                log.warn("Unable to compare last modified timestamp of the two files.", e);
            }
            return lDiff.intValue();
        }
    }

    private static class FileSizeAscComparator implements Comparator<FileObject>, Serializable {

        private static final long serialVersionUID = 1;

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

    private static class FileNameDesComparator implements Comparator<FileObject>, Serializable {

        private static final long serialVersionUID = 1;

        @Override
        public int compare(FileObject o1, FileObject o2) {
            return o2.getName().compareTo(o1.getName());
        }
    }

    private static class FileLastmodifiedtimestampDesComparator implements Comparator<FileObject>, Serializable {

        private static final long serialVersionUID = 1;

        @Override
        public int compare(FileObject o1, FileObject o2) {
            Long lDiff = 0L;
            try {
                lDiff = o2.getContent().getLastModifiedTime() - o1.getContent().getLastModifiedTime();
            } catch (FileSystemException e) {
                log.warn("Unable to compare last modified timestamp of the two files.", e);
            }
            return lDiff.intValue();
        }
    }

    private static class FileSizeDesComparator implements Comparator<FileObject>, Serializable {

        private static final long serialVersionUID = 1;

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
