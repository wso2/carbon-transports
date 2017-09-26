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

import org.wso2.carbon.transport.remotefilesystem.server.connector.contract.RemoteFileSystemListener;
import org.wso2.carbon.transport.remotefilesystem.server.connector.contract.RemoteFileSystemMessage;
import org.wso2.carbon.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnectorFuture;

/**
 * Implementation of the {@link RemoteFileSystemServerConnectorFuture}.
 */
public class RemoteFileSystemServerConnectorFutureImpl implements RemoteFileSystemServerConnectorFuture {

    private RemoteFileSystemListener remoteFileSystemListener;

    public RemoteFileSystemServerConnectorFutureImpl(RemoteFileSystemListener remoteFileSystemListener) {
        this.remoteFileSystemListener = remoteFileSystemListener;
    }

    @Override
    public void notifyFileSystemListener(RemoteFileSystemMessage remoteFileSystemMessage) {
        remoteFileSystemListener.onMessage(remoteFileSystemMessage);
    }
}
