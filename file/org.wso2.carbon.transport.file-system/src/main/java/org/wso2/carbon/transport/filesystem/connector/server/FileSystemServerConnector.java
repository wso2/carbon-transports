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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.connector.framework.server.polling.PollingServerConnector;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.filesystem.connector.server.exception.FileSystemServerConnectorException;

import java.util.Map;

/**
 * Server connector for File transport.
 */
class FileSystemServerConnector extends PollingServerConnector {

    private static final Logger log = LoggerFactory.getLogger(FileSystemServerConnector.class);

    private static final long FILE_CONNECTOR_DEFAULT_INTERVAL = 10000L;
    private CarbonMessageProcessor messageProcessor;
    private FileSystemConsumer consumer;

    FileSystemServerConnector(String id, Map<String, String> properties) {
        super(id, properties);
        interval = FILE_CONNECTOR_DEFAULT_INTERVAL; //this might be overridden in super.start()
    }

    @Override
    public void setMessageProcessor(CarbonMessageProcessor carbonMessageProcessor) {
        messageProcessor = carbonMessageProcessor;
    }

    @Override
    protected void init() throws ServerConnectorException {
        //There is nothing to do in the connector init phase (at the server start up).
    }

    @Override
    public void destroy() throws ServerConnectorException {
        stop();
    }

    @Override
    public void start() throws ServerConnectorException {
        try {
            consumer = new FileSystemConsumer(id, getProperties(), messageProcessor, errorHandler);
            super.start();
        } catch (RuntimeException e) {
            throw new ServerConnectorException("Failed to start File server connector for Service: " + id, e);
        }
    }

    @Override
    public void poll() {
        try {
            consumer.consume();
        } catch (FileSystemServerConnectorException e) {
            log.error("Error executing the polling cycle of File server connector for service: " + id, e);
        } 
    }
}
