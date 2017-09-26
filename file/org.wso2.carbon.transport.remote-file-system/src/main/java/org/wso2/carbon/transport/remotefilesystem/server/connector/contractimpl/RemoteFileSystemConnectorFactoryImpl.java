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

import org.wso2.carbon.transport.remotefilesystem.server.connector.contract.RemoteFileSystemConnectorFactory;
import org.wso2.carbon.transport.remotefilesystem.server.connector.contract.RemoteFileSystemListener;
import org.wso2.carbon.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnector;
import org.wso2.carbon.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnectorFuture;
import org.wso2.carbon.transport.remotefilesystem.server.connector.exception.RemoteFileSystemServerConnectorException;

import java.util.Map;

/**
 * Implementation for {@link RemoteFileSystemConnectorFactory}.
 */
public class RemoteFileSystemConnectorFactoryImpl implements RemoteFileSystemConnectorFactory {

    @Override
    public RemoteFileSystemServerConnector createServerConnector(String serviceId, Map<String, String> connectorConfig,
                                                                 RemoteFileSystemListener remoteFileSystemListener)
            throws RemoteFileSystemServerConnectorException {
        RemoteFileSystemServerConnectorFuture remoteFileSystemServerConnectorFuture
                = new RemoteFileSystemServerConnectorFutureImpl(remoteFileSystemListener);
        return new RemoteFileSystemServerConnectorImpl(serviceId, connectorConfig,
                remoteFileSystemServerConnectorFuture);
    }
}
