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
import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.TextCarbonMessage;
import org.wso2.carbon.transport.filesystem.connector.server.exception.FileSystemServerConnectorException;
import org.wso2.carbon.transport.filesystem.connector.server.util.Constants;
import org.wso2.carbon.transport.filesystem.connector.server.util.FileTransportUtils;
import org.wso2.carbon.transport.filesystem.connector.server.util.ThreadPoolFactory;


/**
 * File processor to process a single file.
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
    private String postProcessAction;

    /**
     * Initializes the message processor with file details.
     *
     * @param messageProcessor   The message processor instance
     * @param serviceName        The name of the destination service
     * @param file               The file to be processed
     * @param continueIfNotAck   Whether to continue processing if acknowledgement is not received
     * @param timeOutInterval    Time-out interval in milliseconds for waiting for the acknowledgement
     * @param fileURI            The URI of the file which is being processed
     * @param fileSystemConsumer FileSystemConsumer instance of processed file/directory
     * @param postProcessAction  Action to be applied to file once it is processed
     */
    FileSystemProcessor(CarbonMessageProcessor messageProcessor, String serviceName, FileObject file,
                        boolean continueIfNotAck, long timeOutInterval, String fileURI,
                        FileSystemConsumer fileSystemConsumer, String postProcessAction) {
        this.messageProcessor = messageProcessor;
        this.file = file;
        this.continueIfNotAck = continueIfNotAck;
        this.timeOutInterval = timeOutInterval;
        this.serviceName = serviceName;
        this.fileURI = fileURI;
        this.fileSystemConsumer = fileSystemConsumer;
        this.postProcessAction = postProcessAction;
    }

    /**
     * The runnable implementation which is invoked when file processing started.
     */
    @Override
    public void run() {
        TextCarbonMessage message = new TextCarbonMessage(file.getName().getURI());
        message.setProperty(org.wso2.carbon.messaging.Constants.PROTOCOL, Constants.PROTOCOL_FILE_SYSTEM);
        message.setProperty(Constants.FILE_TRANSPORT_PROPERTY_SERVICE_NAME, serviceName);
        boolean processFailed = false;
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
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for message processor to consume" +
                                                         " the file input stream. Aborting processing of file: " +
                                                         FileTransportUtils.maskURLPassword(fileURI), e);
        } catch (FileSystemServerConnectorException e) {
            logger.warn(e.getMessage());
            processFailed = true;
        }

        if (postProcessAction.equals(Constants.ACTION_NONE)) {
            fileSystemConsumer.markProcessed(fileURI);
        }

        if (!postProcessAction.equals(Constants.ACTION_NONE)) {
            try {
                fileSystemConsumer.postProcess(file, processFailed);
            } catch (FileSystemServerConnectorException e) {
                logger.error("File object '" + FileTransportUtils.maskURLPassword(file.getName().toString()) + "' " +
                             "cloud not be moved", e);
            }
        }

        FileTransportUtils.releaseLock(file);
        if (logger.isDebugEnabled()) {
            logger.debug("Removed the lock file '" + FileTransportUtils.maskURLPassword(file.toString()) +
                         ".lock' of the file '" + FileTransportUtils.maskURLPassword(file.toString()));
        }
        //close the file system after processing
        try {
            file.close();
        } catch (FileSystemException e) {
            logger.warn("Could not close the file: " + file.getName().getPath(), e);
        }
    }

    /**
     * Start file processing thread.
     */
    void startProcessThread() {
        ThreadPoolFactory.getInstance().getExecutor().execute(this);
    }
}
