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

package org.wso2.carbon.transport.remotefilesystem.impl;

import org.wso2.carbon.transport.remotefilesystem.RemoteFileSystemConnectorFactory;
import org.wso2.carbon.transport.remotefilesystem.client.connector.contract.VFSClientConnector;
import org.wso2.carbon.transport.remotefilesystem.client.connector.contractimpl.VFSClientConnectorImpl;
import org.wso2.carbon.transport.remotefilesystem.exception.RemoteFileSystemConnectorException;
import org.wso2.carbon.transport.remotefilesystem.listener.RemoteFileSystemListener;
import org.wso2.carbon.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnector;
import org.wso2.carbon.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnectorFuture;
import org.wso2.carbon.transport.remotefilesystem.server.connector.contractimpl.RemoteFileSystemServerConnectorFutureImpl;
import org.wso2.carbon.transport.remotefilesystem.server.connector.contractimpl.RemoteFileSystemServerConnectorImpl;

import java.util.Map;

/**
 * Implementation for {@link RemoteFileSystemConnectorFactory}.
 */
public class RemoteFileSystemConnectorFactoryImpl implements RemoteFileSystemConnectorFactory {

    @Override
    public RemoteFileSystemServerConnector createServerConnector(String serviceId, Map<String, String> connectorConfig,
                                                                 RemoteFileSystemListener remoteFileSystemListener)
            throws RemoteFileSystemConnectorException {
        RemoteFileSystemServerConnectorFuture remoteFileSystemServerConnectorFuture
                = new RemoteFileSystemServerConnectorFutureImpl(remoteFileSystemListener);
        return new RemoteFileSystemServerConnectorImpl(serviceId, connectorConfig,
                remoteFileSystemServerConnectorFuture);
    }

    @Override
    public VFSClientConnector createVFSClientConnector(String serviceId, Map<String, String> connectorConfig,
                                                       RemoteFileSystemListener remoteFileSystemListener) {
        return new VFSClientConnectorImpl(serviceId, connectorConfig, remoteFileSystemListener);
    }
}
