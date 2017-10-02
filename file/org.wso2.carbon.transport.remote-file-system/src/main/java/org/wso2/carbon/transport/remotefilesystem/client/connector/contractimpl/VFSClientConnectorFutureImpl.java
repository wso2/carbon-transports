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

package org.wso2.carbon.transport.remotefilesystem.client.connector.contractimpl;

import org.wso2.carbon.transport.remotefilesystem.client.connector.contract.VFSClientConnectorFuture;
import org.wso2.carbon.transport.remotefilesystem.listener.RemoteFileSystemListener;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemMessage;

/**
 * Implementation for {@link VFSClientConnectorFuture} interface.
 */
public class VFSClientConnectorFutureImpl implements VFSClientConnectorFuture {

    private RemoteFileSystemListener fileSystemListener;

    @Override
    public void setFileSystemListener(RemoteFileSystemListener fileSystemListener) {
        this.fileSystemListener = fileSystemListener;
    }

    @Override
    public void notifyFileSystemListener(RemoteFileSystemMessage message) {
        if (fileSystemListener != null) {
            fileSystemListener.onMessage(message);
        }
    }

    @Override
    public void notifyFileSystemListener(Throwable throwable) {
        if (fileSystemListener != null) {
            fileSystemListener.onError(throwable);
        }
    }
}
