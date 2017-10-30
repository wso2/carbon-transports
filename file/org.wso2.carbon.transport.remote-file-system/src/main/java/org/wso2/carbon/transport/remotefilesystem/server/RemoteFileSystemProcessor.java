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

package org.wso2.carbon.transport.remotefilesystem.server;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.remotefilesystem.Constants;
import org.wso2.carbon.transport.remotefilesystem.exception.RemoteFileSystemConnectorException;
import org.wso2.carbon.transport.remotefilesystem.listener.RemoteFileSystemListener;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemEvent;
import org.wso2.carbon.transport.remotefilesystem.server.util.FileTransportUtils;

/**
 * File processor to process a single file.
 */
public class RemoteFileSystemProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(RemoteFileSystemProcessor.class);

    private RemoteFileSystemListener listener;
    private FileObject file;
    private String serviceName;
    private RemoteFileSystemConsumer remoteFileSystemConsumer;
    private String postProcessAction;
    private FileSystemManager fsManager;
    private FileSystemOptions fso;

    /**
     * Initializes the message processor with file details.
     *  @param remoteFileSystemListener   The RemoteFileSystemListener instance to notify callback
     * @param serviceName        The name of the destination service
     * @param file               The file to be processed
     * @param remoteFileSystemConsumer RemoteFileSystemConsumer instance of processed file/directory
     * @param postProcessAction  Action to be applied to file once it is processed
     * @param fsManager FileSystemManager instance
     * @param fso FileSystemOptions instance
     */
    RemoteFileSystemProcessor(RemoteFileSystemListener remoteFileSystemListener, String serviceName,
                              FileObject file, RemoteFileSystemConsumer remoteFileSystemConsumer,
                              String postProcessAction, FileSystemManager fsManager, FileSystemOptions fso) {
        this.listener = remoteFileSystemListener;
        this.file = file;
        this.serviceName = serviceName;
        this.remoteFileSystemConsumer = remoteFileSystemConsumer;
        this.postProcessAction = postProcessAction;
        this.fsManager = fsManager;
        this.fso = fso;
    }

    /**
     * The runnable implementation which is invoked when file processing started.
     */
    @Override
    public void run() {
        if (FileTransportUtils.acquireLock(fsManager, file, fso)) {
            String uri = file.getName().getURI();
            String originalUri = uri;
            uri = uri.startsWith("file://") ? uri.replace("file://", "") : uri;
            RemoteFileSystemEvent message = new RemoteFileSystemEvent(uri);
            try {
                message.setFileSize(file.getContent().getSize());
                message.setLastModifiedTime(file.getContent().getLastModifiedTime());
            } catch (FileSystemException e) {
                logger.error("[" + serviceName + "] Failed to set meta data for file: " + file.getName().getURI(), e);
                listener.onError(e);
            }
            boolean processSucceed = false;
            try {
                processSucceed = listener.onMessage(message);
            } catch (Exception e) {
                listener.onError(e);
                logger.warn("[" + serviceName + "] Failed to send stream from file: " +
                        FileTransportUtils.maskURLPassword(originalUri) + " to listener. ", e);
            }
            if (postProcessAction.equals(Constants.ACTION_NONE)) {
                remoteFileSystemConsumer.markProcessed(originalUri);
            } else {
                try {
                    remoteFileSystemConsumer.postProcess(file, processSucceed);
                } catch (RemoteFileSystemConnectorException e) {
                    listener.onError(e);
                    logger.error("[" + serviceName + "] File object '" +
                            FileTransportUtils.maskURLPassword(file.getName().toString()) + "' " +
                            "could not be moved", e);
                }
            }
            FileTransportUtils.releaseLock(file);
            if (logger.isDebugEnabled()) {
                logger.debug("Released the lock file '" + FileTransportUtils.maskURLPassword(file.toString()) +
                        ".lock' of the file '" + FileTransportUtils.maskURLPassword(file.toString()));
            }
            try {
                file.close();
            } catch (FileSystemException e) {
                logger.warn("[" + serviceName + "] Could not close the file: " + file.getName().getPath(), e);
            }
            remoteFileSystemConsumer.removeProcessPending(originalUri);
        } else {
            logger.warn("[" + serviceName + "] Couldn't get the lock for processing the file: " +
                    FileTransportUtils.maskURLPassword(file.getName().toString()));
        }
    }
}
