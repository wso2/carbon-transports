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

package org.wso2.carbon.transport.remotefilesystem.server.connector.contract;

import org.wso2.carbon.transport.remotefilesystem.server.connector.exception.RemoteFileSystemServerConnectorException;

import java.util.Map;

/**
 * Allow to create File system connector.
 */
public interface RemoteFileSystemConnectorFactory {

    /**
     * @param serviceId          id used to create the server connector instance.
     * @param connectorConfig    properties required for the {@link RemoteFileSystemServerConnector} class.
     * @param remoteFileSystemListener listener which gets triggered when message comes.
     * @return RemoteFileSystemServerConnector A newly created File System server connector instance.
     * @throws RemoteFileSystemServerConnectorException if any error occurred when creating the server connector.
     */
    RemoteFileSystemServerConnector createServerConnector(String serviceId, Map<String, String> connectorConfig,
                                                          RemoteFileSystemListener remoteFileSystemListener)
            throws RemoteFileSystemServerConnectorException;
}
