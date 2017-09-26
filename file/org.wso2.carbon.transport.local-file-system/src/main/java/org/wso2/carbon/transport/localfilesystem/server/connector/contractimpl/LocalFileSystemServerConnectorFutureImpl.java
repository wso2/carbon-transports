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

import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemListener;
import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemMessage;
import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemServerConnectorFuture;

/**
 * Implementation of the {@link LocalFileSystemServerConnectorFuture}.
 */
public class LocalFileSystemServerConnectorFutureImpl implements LocalFileSystemServerConnectorFuture {

    private LocalFileSystemListener localFileSystemListener;

    public LocalFileSystemServerConnectorFutureImpl(LocalFileSystemListener localFileSystemListener) {
        this.localFileSystemListener = localFileSystemListener;
    }

    @Override
    public void notifyFileSystemListener(LocalFileSystemMessage localFileSystemMessage) {
        localFileSystemListener.onMessage(localFileSystemMessage);
    }
}
