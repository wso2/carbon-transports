/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.transport.filesystem.connector.server;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.TextCarbonMessage;
import org.wso2.carbon.transport.filesystem.connector.server.exception.FileSystemServerConnectorException;
import org.wso2.carbon.transport.filesystem.connector.server.util.Constants;
import org.wso2.carbon.transport.filesystem.connector.server.util.FileTransportUtils;
import org.wso2.carbon.transport.filesystem.connector.server.util.ThreadPoolFactory;


/**
 * Message receiver for receiving JMS messages synchronously.
 */
class FileSystemProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemProcessor.class);

    private CarbonMessageProcessor messageProcessor;
    private FileObject file;
    private boolean continueIfNotAck;
    private long timeOutInterval;
    private String serviceName;
    private String fileURI;
    private FileSystemConsumer fileSystemConsumer;
    private boolean fileLock;
    private FileSystemManager fsManager;
    private FileSystemOptions fso;

    /**
     *
     */
    FileSystemProcessor(CarbonMessageProcessor messageProcessor, String serviceName, FileObject file,
                        boolean continueIfNotAck, long timeOutInterval, String fileURI,
                        FileSystemConsumer fileSystemConsumer, boolean fileLock, FileSystemManager fsManager,
                        FileSystemOptions fso) {
        this.messageProcessor = messageProcessor;
        this.file = file;
        this.continueIfNotAck = continueIfNotAck;
        this.timeOutInterval = timeOutInterval;
        this.serviceName = serviceName;
        this.fileURI = fileURI;
        this.fileSystemConsumer = fileSystemConsumer;
        this.fileLock = fileLock;
        this.fsManager = fsManager;
        this.fso = fso;
    }

    /**
     * The runnable implementation which is invoked when message receiving is started.
     */
    @Override
    public void run() {
        TextCarbonMessage message = new TextCarbonMessage(file.getName().getURI());
        message.setProperty(org.wso2.carbon.messaging.Constants.PROTOCOL, Constants.PROTOCOL_FILE_SYSTEM);
        message.setProperty(Constants.FILE_TRANSPORT_PROPERTY_SERVICE_NAME, serviceName);

        FileSystemServerConnectorCallback callback = new FileSystemServerConnectorCallback();
        try {
            messageProcessor.receive(message, callback);
        } catch (Exception e) {
            logger.warn(
                    "Failed to send stream from file: " + FileTransportUtils.maskURLPassword(fileURI)
                    + " to message processor. ", e);
        }
        try {
            callback.waitTillDone(timeOutInterval, continueIfNotAck, fileURI);
            fileSystemConsumer.processFailed = false;
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for message processor to consume" +
                                                         " the file input stream. Aborting processing of file: " +
                                                         FileTransportUtils.maskURLPassword(fileURI), e);
        } catch (FileSystemServerConnectorException e) {
            logger.warn(e.getMessage());
            fileSystemConsumer.processFailed = true;
        }

        try {
            fileSystemConsumer.postProcess(file);
        } catch (FileSystemServerConnectorException e) {
            logger.error("File object '" + FileTransportUtils.maskURLPassword(file.getName().toString()) + "' " +
                      "cloud not be moved", e);
        }

        if (fileLock) {
            // TODO: passing null to avoid build break. Fix properly
            FileTransportUtils.releaseLock(fsManager, file, fso);
            if (logger.isDebugEnabled()) {
                logger.debug("Removed the lock file '" + FileTransportUtils.maskURLPassword(file.toString()) +
                          ".lock' of the file '" + FileTransportUtils.maskURLPassword(file.toString()));
            }
        }
    }

    /**
     * Start file processing thread.
     */
    void startProcessThread() {
        ThreadPoolFactory.getInstance().getExecutor().execute(this);
    }
}
