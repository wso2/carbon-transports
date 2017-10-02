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

package org.wso2.carbon.transport.remotefilesystem.server.connector.contractimpl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.connector.framework.server.polling.PollingServerConnector;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.remotefilesystem.exception.RemoteFileSystemConnectorException;
import org.wso2.carbon.transport.remotefilesystem.server.RemoteFileSystemConsumer;
import org.wso2.carbon.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnector;
import org.wso2.carbon.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnectorFuture;

import java.util.Map;

/**
 * Implementation of the {@link RemoteFileSystemServerConnector} interface.
 */
public class RemoteFileSystemServerConnectorImpl extends PollingServerConnector
        implements RemoteFileSystemServerConnector {

    private static final Logger log = LoggerFactory.getLogger(RemoteFileSystemServerConnectorImpl.class);

    private static final long FILE_CONNECTOR_DEFAULT_INTERVAL = 10000L;
    private RemoteFileSystemConsumer consumer;

    public RemoteFileSystemServerConnectorImpl(String id, Map<String, String> properties,
                                               RemoteFileSystemServerConnectorFuture connectorFuture)
            throws RemoteFileSystemConnectorException {
        super(id, properties);
        interval = FILE_CONNECTOR_DEFAULT_INTERVAL; //this might be overridden in super.start()
        try {
            consumer = new RemoteFileSystemConsumer(id, getProperties(), connectorFuture);
        } catch (ServerConnectorException e) {
            throw new RemoteFileSystemConnectorException("Failed to initialize File server connector " +
                    "for Service: " + id, e);
        }
    }

    @Override
    public void setMessageProcessor(CarbonMessageProcessor carbonMessageProcessor) {
    }

    @Override
    protected void init() throws ServerConnectorException {

    }

    @Override
    protected void destroy() throws ServerConnectorException {
        super.stop();
    }

    @Override
    public void start() throws RemoteFileSystemConnectorException {
        try {
            super.start();
        } catch (RuntimeException | ServerConnectorException e) {
            throw new RemoteFileSystemConnectorException("Failed to start RemoteFileSystemServer" +
                    " connector for Service: " + id, e);
        }
    }

    @Override
    public void stop() throws RemoteFileSystemConnectorException {
        try {
            /*ExecutorService executor = ThreadPoolFactory.getInstance().getExecutor();
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }*/
            destroy();
        } catch (ServerConnectorException e) {
            throw new RemoteFileSystemConnectorException("Failed to stop RemoteFileSystemServer" +
                    " for Service: " + id, e);
        }
    }

    @Override
    protected void poll() {
        try {
            consumer.consume();
        } catch (RemoteFileSystemConnectorException e) {
            log.error("Error executing the polling cycle of RemoteFileSystemServer for service: " + id, e);
        }
    }
}
