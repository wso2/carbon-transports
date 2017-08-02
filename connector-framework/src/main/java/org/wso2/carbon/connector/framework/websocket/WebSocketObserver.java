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

/**
 * Message processor for WebSocket inbound messages.
 */
public interface WebSocketObserver {

    /**
     * This method is used to trigger WebSocket handshake. This will initialize a client connection for WebSocket
     * server connector.
     *
     * @param initMessage {@link WebSocketInitMessage} to initialize connection.
     */
    void notifyConnectionOpen(WebSocketInitMessage initMessage);

    /**
     * This method is used to process incoming WebSocket text messages.
     *
     * @param textMessage {@link WebSocketTextMessage} to process text messages.
     */
    void notifyTextMessage(WebSocketTextMessage textMessage);

    /**
     * This method is used to process incoming WebSocket binary messages.
     *
     * @param binaryMessage {@link WebSocketBinaryMessage} to process binary messages.
     */
    void notifyBinaryMessage(WebSocketBinaryMessage binaryMessage);

    /**
     * This method is used to process incoming WebSocket binary messages.
     *
     * @param controlMessage {@link WebSocketControlMessage} to indicate a incoming pong messages.
     */
    void notifyPongMessage(WebSocketControlMessage controlMessage);

    /**
     * This method is used to process incoming WebSocket close messages.
     *
     * @param closeMessage {@link WebSocketCloseMessage} to indicate incoming close messages.
     */
    void notifyConnectionClosure(WebSocketCloseMessage closeMessage);

    /**
     * Handle any transport error.
     *
     * @param throwable error received from transport.
     */
    void notifyError(Throwable throwable);

}
