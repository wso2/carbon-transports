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

package org.wso2.carbon.transport.filesystem.connector.server.contract;

import org.wso2.carbon.transport.filesystem.connector.server.exception.FileSystemServerConnectorException;

import java.util.Map;

/**
 * Allow to create File system connector.
 */
public interface FileSystemConnectorFactory {

    /**
     * @param serviceId          id used to create the server connector instance.
     * @param connectorConfig    properties required for the {@link FileSystemServerConnector} class.
     * @param fileSystemListener listener which gets triggered when message comes.
     * @return FileSystemServerConnector A newly created File System server connector instance.
     * @throws FileSystemServerConnectorException if any error occurred when creating the server connector.
     */
    FileSystemServerConnector createServerConnector(String serviceId, Map<String, String> connectorConfig,
                                                    FileSystemListener fileSystemListener)
            throws FileSystemServerConnectorException;
}
