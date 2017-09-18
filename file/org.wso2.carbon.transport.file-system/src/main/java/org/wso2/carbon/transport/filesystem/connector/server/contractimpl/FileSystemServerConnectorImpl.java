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

package org.wso2.carbon.transport.filesystem.connector.server.contractimpl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.connector.framework.server.polling.PollingServerConnector;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.filesystem.connector.server.FileSystemConsumer;
import org.wso2.carbon.transport.filesystem.connector.server.contract.FileSystemServerConnector;
import org.wso2.carbon.transport.filesystem.connector.server.contract.FileSystemServerConnectorFuture;
import org.wso2.carbon.transport.filesystem.connector.server.exception.FileSystemServerConnectorException;

import java.util.Map;

/**
 * Implementation of the {@link FileSystemServerConnector} interface.
 */
public class FileSystemServerConnectorImpl extends PollingServerConnector implements FileSystemServerConnector {

    private static final Logger log = LoggerFactory.getLogger(FileSystemServerConnectorImpl.class);

    private static final long FILE_CONNECTOR_DEFAULT_INTERVAL = 10000L;
    private CarbonMessageProcessor messageProcessor;
    private FileSystemConsumer consumer;

    public FileSystemServerConnectorImpl(String id, Map<String, String> properties,
                                         FileSystemServerConnectorFuture fileSystemServerConnectorFuture)
            throws FileSystemServerConnectorException {
        super(id, properties);
        interval = FILE_CONNECTOR_DEFAULT_INTERVAL; //this might be overridden in super.start()
        try {
            consumer = new FileSystemConsumer(id, getProperties(), fileSystemServerConnectorFuture, errorHandler);
        } catch (ServerConnectorException e) {
            throw new FileSystemServerConnectorException("Failed to initialize File server connector " +
                    "for Service: " + id, e);
        }
    }

    @Override
    public void setMessageProcessor(CarbonMessageProcessor carbonMessageProcessor) {
        this.messageProcessor = carbonMessageProcessor;
    }

    @Override
    protected void init() throws ServerConnectorException {

    }

    @Override
    protected void destroy() throws ServerConnectorException {
        super.stop();
    }

    @Override
    public void start() throws FileSystemServerConnectorException {
        try {
            super.start();
        } catch (RuntimeException | ServerConnectorException e) {
            throw new FileSystemServerConnectorException("Failed to start File server connector for Service: " + id, e);
        }
    }

    @Override
    public void stop() throws FileSystemServerConnectorException {
        try {
            destroy();
        } catch (ServerConnectorException e) {
            throw new FileSystemServerConnectorException("Failed to stop File server connector for Service: " + id, e);
        }
    }

    @Override
    protected void poll() {
        try {
            consumer.consume();
        } catch (FileSystemServerConnectorException e) {
            log.error("Error executing the polling cycle of File server connector for service: " + id, e);
        }
    }
}
