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

package org.wso2.carbon.transport.localfilesystem.server.connector.contractimpl;


import org.wso2.carbon.transport.localfilesystem.server.DirectoryListener;
import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemListener;
import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemServerConnector;
import org.wso2.carbon.transport.localfilesystem.server.exception.LocalFileSystemServerConnectorException;

import java.util.Map;

/**
 * Implementation of the {@link LocalFileSystemServerConnector} interface.
 */
public class LocalFileSystemServerConnectorImpl implements LocalFileSystemServerConnector {

    private DirectoryListener directoryListener;

    public LocalFileSystemServerConnectorImpl(String id, Map<String, String> properties,
                                              LocalFileSystemListener localFileSystemListener)
            throws LocalFileSystemServerConnectorException {
        directoryListener = new DirectoryListener(id, properties, localFileSystemListener);
    }

    @Override
    public void start() throws LocalFileSystemServerConnectorException {
        directoryListener.start();
    }

    @Override
    public void stop() throws LocalFileSystemServerConnectorException {
        directoryListener.stop();
    }
}
