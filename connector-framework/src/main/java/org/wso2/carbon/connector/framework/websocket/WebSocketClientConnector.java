/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.connector.framework.websocket;

import java.util.Map;
import javax.websocket.Session;

/**
 * This interface represent the client connector for WebSocket protocol.
 */
public interface WebSocketClientConnector {

    /**
     * Initialize a WebSocket client connection for a remote server.
     *
     * @param remoteURL remote URL where the client should be connected.
     * @param inboundTarget Inbound target of the application. When a message is received from the remote server to the
     *                      client connector this represent the receiving target of the application.
     * @param properties Additional properties which are needed for the handshake with the server.
     * @return the client {@link Session} which is created for the connection between client and the remote server.
     */
    Session intializeConnection(String remoteURL, String inboundTarget, Map<String, String> properties);
}
