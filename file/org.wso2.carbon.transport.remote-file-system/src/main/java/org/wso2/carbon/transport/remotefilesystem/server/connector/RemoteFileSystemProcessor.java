/*
 * Copyright (c) 2017 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.transport.remotefilesystem.server.connector;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.remotefilesystem.server.connector.contract.RemoteFileSystemMessage;
import org.wso2.carbon.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnectorFuture;
import org.wso2.carbon.transport.remotefilesystem.server.connector.exception.RemoteFileSystemServerConnectorException;
import org.wso2.carbon.transport.remotefilesystem.server.connector.util.Constants;
import org.wso2.carbon.transport.remotefilesystem.server.connector.util.FileTransportUtils;
import org.wso2.carbon.transport.remotefilesystem.server.connector.util.ThreadPoolFactory;


/**
 * File processor to process a single file.
 */
class RemoteFileSystemProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(RemoteFileSystemProcessor.class);

    private RemoteFileSystemServerConnectorFuture connectorFuture;
    private FileObject file;
    private String serviceName;
    private String fileURI;
    private RemoteFileSystemConsumer remoteFileSystemConsumer;
    private String postProcessAction;

    /**
     * Initializes the message processor with file details.
     *
     * @param connectorFuture   The RemoteFileSystemServerConnectorFuture instance to notify callback
     * @param serviceName        The name of the destination service
     * @param file               The file to be processed
     * @param fileURI            The URI of the file which is being processed
     * @param remoteFileSystemConsumer RemoteFileSystemConsumer instance of processed file/directory
     * @param postProcessAction  Action to be applied to file once it is processed
     */
    RemoteFileSystemProcessor(RemoteFileSystemServerConnectorFuture connectorFuture, String serviceName,
                              FileObject file, String fileURI,
                              RemoteFileSystemConsumer remoteFileSystemConsumer, String postProcessAction) {
        this.connectorFuture = connectorFuture;
        this.file = file;
        this.serviceName = serviceName;
        this.fileURI = fileURI;
        this.remoteFileSystemConsumer = remoteFileSystemConsumer;
        this.postProcessAction = postProcessAction;
    }

    /**
     * The runnable implementation which is invoked when file processing started.
     */
    @Override
    public void run() {
        String uri = file.getName().getURI();
        uri = uri.startsWith("file://") ? uri.replace("file://", "") : uri;

        RemoteFileSystemMessage message = new RemoteFileSystemMessage(uri);
        try {
            String protocol = file.getURL().getProtocol();

            // Since there is a separate module for File Server Connector, if the protocol is file, mark it as fs
            if (Constants.PROTOCOL_FILE.equals(protocol)) {
                protocol = Constants.PROTOCOL_FILE_SYSTEM;
            }
            message.setProperty(org.wso2.carbon.messaging.Constants.PROTOCOL, protocol);
        } catch (FileSystemException e) {
            logger.error("Exception occurred while retrieving the file protocol", e);
            message.setProperty(org.wso2.carbon.messaging.Constants.PROTOCOL, Constants.PROTOCOL_FILE_SYSTEM);
        }

        message.setProperty(Constants.FILE_TRANSPORT_PROPERTY_SERVICE_NAME, serviceName);
        try {
            message.setProperty(Constants.META_FILE_SIZE, file.getContent().getSize());
            message.setProperty(Constants.META_FILE_LAST_MODIFIED_TIME, file.getContent().getLastModifiedTime());
        } catch (FileSystemException e) {
            logger.error("Failed to set meta data for file: " + file.getName().getURI(), e);
        }

        boolean processFailed = false;
        try {
            connectorFuture.notifyFileSystemListener(message);
        } catch (Exception e) {
            logger.warn(
                    "Failed to send stream from file: " + FileTransportUtils.maskURLPassword(fileURI)
                    + " to message processor. ", e);
            processFailed = true;
        }

        if (postProcessAction.equals(Constants.ACTION_NONE)) {
            remoteFileSystemConsumer.markProcessed(fileURI);
        } else {
            try {
                remoteFileSystemConsumer.postProcess(file, processFailed);
            } catch (RemoteFileSystemServerConnectorException e) {
                logger.error("File object '" + FileTransportUtils.maskURLPassword(file.getName().toString()) + "' " +
                             "could not be moved", e);
            }
        }

        FileTransportUtils.releaseLock(file);
        if (logger.isDebugEnabled()) {
            logger.debug("Released the lock file '" + FileTransportUtils.maskURLPassword(file.toString()) +
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
