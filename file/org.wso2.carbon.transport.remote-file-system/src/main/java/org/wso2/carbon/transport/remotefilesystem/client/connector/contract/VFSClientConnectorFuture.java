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

package org.wso2.carbon.transport.remotefilesystem.client.connector.contract;

import org.wso2.carbon.transport.remotefilesystem.listener.RemoteFileSystemListener;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemMessage;

/**
 * Represents the future events and results of {@link VFSClientConnector}.
 */
public interface VFSClientConnectorFuture {

    void setFileSystemListener(RemoteFileSystemListener fileSystemListener);

    /**
     * Notify the listeners when there is a message.
     *
     * @param message contains the data related to the event.
     */
    void notifyFileSystemListener(RemoteFileSystemMessage message);

    /**
     * Notify the listeners when there is an error.
     *
     * @param throwable contains the data related to the error.
     */
    void notifyFileSystemListener(Throwable throwable);
}
