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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.connector.framework.server.polling.PollingServerConnector;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.file.connector.server.exception.FileServerConnectorException;

import java.util.Map;

/**
 * Server connector for File transport.
 */
public class FileServerConnector extends PollingServerConnector {
    private static final Log log = LogFactory.getLog(FileServerConnector.class);

    private long interval = 10000L;
    private CarbonMessageProcessor messageProcessor;
    private FileConsumer consumer;

    public FileServerConnector(String id) {
        super(id);
    }

    @Override
    public void setMessageProcessor(CarbonMessageProcessor carbonMessageProcessor) {
        messageProcessor = carbonMessageProcessor;
    }

    public CarbonMessageProcessor getMessageProcessor() {
        return messageProcessor;
    }

    @Override
    protected void init() throws ServerConnectorException {
        //There is nothing to do in the connector init phase (at the server start up).
    }

    @Override
    protected void destroy() throws ServerConnectorException {
        stop();
    }


    @Override
    public void start(Map<String, String> parameters) throws ServerConnectorException {
        consumer = new FileConsumer(id, parameters, messageProcessor);
        super.start(parameters);
    }

    @Override
    public void poll() {
        try {
            consumer.consume();
        } catch (FileServerConnectorException e) {
            log.error("Error executing the polling cycle for " +
                    "server connector ID: " + id + ". Reason: " + e.getMessage());
        }
    }

    @Override
    protected void beginMaintenance() {
        //No maintenance work for FileServerConnector
    }

    @Override
    protected void endMaintenance() {
        //No maintenance work for FileServerConnector
    }

    @Override
    public long getInterval() {
        return this.interval;
    }
}
