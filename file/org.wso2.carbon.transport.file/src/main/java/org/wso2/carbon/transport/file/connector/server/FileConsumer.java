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

import org.apache.commons.vfs2.FileNotFoundException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.RandomAccessContent;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder;
import org.apache.commons.vfs2.util.RandomAccessMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.BinaryCarbonMessage;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.TextCarbonMessage;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.file.connector.server.exception.FileServerConnectorException;
import org.wso2.carbon.transport.file.connector.server.util.Constants;
import org.wso2.carbon.transport.file.connector.server.util.FileTransportUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides the capability to process a file and delete it afterwards.
 */
public class FileConsumer {

    private static final Logger log = LoggerFactory.getLogger(FileConsumer.class);

    private Map<String, String> fileProperties;
    private FileSystemManager fsManager = null;
    private CarbonMessageProcessor messageProcessor;
    private String path;
    private String serviceName;
    private FileObject fileObject;
    private FileSystemOptions fso;

    private final byte[] inbuf = new byte[4096];
    private int startPosition;
    private long currentTime = 0L;
    private long position = 0L;
    private RandomAccessContent reader = null;
    private int maxLinesPerPoll;

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
        Map<String, String> options = parseSchemeFileOptions(path);
        fso = FileTransportUtils.attachFileSystemOptions(options, fsManager);

        if (options != null && Constants.SCHEME_FTP.equals(options.get(Constants.SCHEME))) {
            FtpFileSystemConfigBuilder.getInstance().setPassiveMode(fso, true);
        }

        try {
            fileObject = fsManager.resolveFile(path, fso);
            reader = fileObject.getContent().getRandomAccessContent(RandomAccessMode.READ);
            position = this.startPosition == -1 ? this.fileObject.getContent().getSize() : startPosition;
            currentTime = System.currentTimeMillis();
            reader.seek(position);
        } catch (FileSystemException e) {
            throw new FileServerConnectorException("Failed to resolve path: "
                    + FileTransportUtils.maskURLPassword(path), e);
        } catch (IOException e) {
            throw new FileServerConnectorException("Failed to read File: "
                                                   + FileTransportUtils.maskURLPassword(path), e);
        }
    }

    /**
     * Do the file processing operation for the given set of properties. Do the
     * checks and pass the control to processFile method
     */
    public void consume() throws FileServerConnectorException {
        if (log.isDebugEnabled()) {
            log.debug("Polling for file : " + FileTransportUtils.maskURLPassword(path));
        }

        // If file/folder found proceed to the processing stage
        boolean isFileExists;
        try {
            isFileExists = fileObject.exists();
        } catch (FileSystemException e) {
            throw new FileServerConnectorException("Error occurred when determining whether the file at URI : " +
                                                   FileTransportUtils.maskURLPassword(path) + " exists. " + e);
        }

        boolean isFileReadable;
        try {
            isFileReadable = fileObject.isReadable();
        } catch (FileSystemException e) {
            throw new FileServerConnectorException("Error occurred when determining whether the file at URI : " +
                                                   FileTransportUtils.maskURLPassword(path) + " is readable. " + e);
        }

        if (isFileExists && isFileReadable) {
            FileType fileType;
            try {
                fileType = fileObject.getType();
            } catch (FileSystemException e) {
                throw new FileServerConnectorException(
                        "Error occurred when determining whether file: " + FileTransportUtils.maskURLPassword(path) +
                        " is a file or a folder", e);
            }

            if (fileType == FileType.FILE) {
                processFile(fileObject);
            } else {
                throw new FileServerConnectorException(
                        "Unable to access or read file or directory : " + FileTransportUtils.maskURLPassword(path));
            }
            if (log.isDebugEnabled()) {
                log.debug("End : Scanning directory or file : " + FileTransportUtils.maskURLPassword(path));
            }
        }
    }



    /**
     * Setup the required transport parameters.
     */
    private void setupParams() throws ServerConnectorException {
        path = fileProperties.get(Constants.TRANSPORT_FILE_FILE_PATH);
        if (path == null) {
            throw new ServerConnectorException(Constants.TRANSPORT_FILE_FILE_PATH + " is a " +
                    "mandatory parameter for " + Constants.PROTOCOL_FILE + " transport.");
        }
        if (path.trim().equals("")) {
            throw new ServerConnectorException(Constants.TRANSPORT_FILE_FILE_PATH + " parameter " +
                    "cannot be empty for " + Constants.PROTOCOL_FILE + " transport.");
        }
        String startPosition = fileProperties.get(Constants.START_POSITION);
        if (startPosition != null) {
            this.startPosition = Integer.parseInt(startPosition);
        } else {
            this.startPosition = -1;
        }
        String maxLinesPerPoll = fileProperties.get(Constants.MAX_LINES_PER_POLL);
        if (maxLinesPerPoll != null) {
            this.maxLinesPerPoll = Integer.parseInt(maxLinesPerPoll);
        } else {
            this.maxLinesPerPoll = -1;
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
     * Actual processing of the file/folder.
     *
     * @param file
     * @return
     */
    private FileObject processFile(FileObject file) throws FileServerConnectorException {

        try {
            boolean newer = isFileNewer(fileObject, currentTime);
            long length = this.fileObject.getContent().getSize();
            if (length < position) {

                EventListener.fileRotated(fileObject, messageProcessor, serviceName);
                try {
                    reader = fileObject.getContent().getRandomAccessContent(RandomAccessMode.READ);
                    position = fileObject.getContent().getSize();
                } catch (FileNotFoundException e) {
                    throw new FileServerConnectorException("File Not Found: " +
                                                           FileTransportUtils.maskURLPassword(path), e);
                }
            } else {
                if (length > position) {
                    position = this.readLines(reader);
                    currentTime = System.currentTimeMillis();
                } else if (newer) {
                    position = fileObject.getContent().getSize();
                    reader.seek(position);
                    position = this.readLines(reader);
                    currentTime = System.currentTimeMillis();
                }
                FileObject parent = fileObject.getParent();
                parent.getType(); // assure that parent folder is attached
                parent.refresh();
                fileObject.refresh();
                reader.close();
                reader = fileObject.getContent().getRandomAccessContent(RandomAccessMode.READ);
                reader.seek(position);
            }
        } catch (FileSystemException e) {
            throw new FileServerConnectorException(
                    "Error in reading file: " + FileTransportUtils.maskURLPassword(path), e);
        } catch (IOException e) {
            throw new FileServerConnectorException(
                    "Error in reading file: " + FileTransportUtils.maskURLPassword(path), e);
        }
        return file;
    }

    private long readLines(RandomAccessContent reader)
            throws IOException, FileServerConnectorException {

        long pos = reader.getFilePointer();
        long rePos = pos;

        List<Byte> list = new ArrayList<>();
        int num;
        int lines = 0;
        boolean throttled = false;
            for (;
                 ((num = read(reader, inbuf)) != -1) && !throttled; pos = reader.getFilePointer()) {
                for (int i = 0; (i < num) && !throttled; ++i) {
                    byte ch = this.inbuf[i];
                    if (ch == 10) {
                        Byte[] line = new Byte[list.size()];
                        line = list.toArray(line);
                        EventListener.fileUpdated(line, messageProcessor, serviceName);
                        lines++;
                        list.clear();
                        rePos = pos + (long) i + 1L;
                    } else {
                        list.add(ch);
                    }
                    if (maxLinesPerPoll != -1 && (lines > maxLinesPerPoll) && ch == 10) {
                        throttled = true;
                    }
                }
            }

        reader.seek(rePos);
        return rePos;
    }
    private static boolean isFileNewer(FileObject file, long timeMillis) throws FileSystemException {
        if (file == null) {
            throw new IllegalArgumentException("No specified file");
        } else {
            return !file.exists() || file.getContent().getLastModifiedTime() > timeMillis;
        }
    }

    private static int read(RandomAccessContent reader, byte[] inbuf) throws IOException {
        InputStream is = reader.getInputStream();
        int count  = is.read(inbuf);
        return count;
    }

    private static class EventListener {

        private static void fileRotated(FileObject file, CarbonMessageProcessor messageProcessor, String serviceName)
                throws FileServerConnectorException {

            try {

                TextCarbonMessage cMessage = new TextCarbonMessage(file.getURL().toString());
                cMessage.setProperty(org.wso2.carbon.messaging.Constants.PROTOCOL, Constants.PROTOCOL_FILE);
                cMessage.setProperty(Constants.FILE_TRANSPORT_PROPERTY_SERVICE_NAME, serviceName);
                cMessage.setProperty(Constants.FILE_TRANSPORT_EVENT_NAME, Constants.FILE_ROTATE);

                messageProcessor.receive(cMessage, null);
            } catch (Exception e) {
                throw new FileServerConnectorException("Failed to send event message processor. ", e);
            }
        }

        private static void fileUpdated(Byte[] content, CarbonMessageProcessor messageProcessor, String serviceName)
                throws FileServerConnectorException {
            try {
                CarbonMessage cMessage = new BinaryCarbonMessage(ByteBuffer.wrap(toPrimitives(content)), true);
                cMessage.setProperty(org.wso2.carbon.messaging.Constants.PROTOCOL, Constants.PROTOCOL_FILE);
                cMessage.setProperty(Constants.FILE_TRANSPORT_PROPERTY_SERVICE_NAME, serviceName);
                cMessage.setProperty(Constants.FILE_TRANSPORT_EVENT_NAME, Constants.FILE_UPDATE);
                cMessage.setProperty(Constants.SINGLE_THREADED_EXECUTION, serviceName);

                messageProcessor.receive(cMessage, null);
            } catch (Exception e) {
                throw new FileServerConnectorException("Failed to send event message processor. ", e);
            }
        }

        private static byte[] toPrimitives(Byte[] oBytes) {

            byte[] bytes = new byte[oBytes.length];

            for (int i = 0; i < oBytes.length; i++) {
                bytes[i] = oBytes[i];
            }

            return bytes;
        }

    }

}
