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

import java.util.HashMap;
import java.util.Map;
import javax.websocket.Session;

/**
 * This interface can be used to set the common details of a given WebSocket message. <br>
 * <b>Note: Use this class in the application level only and only if the user needs to set the parameters of a given
 * WebSocket message or need transport properties in any mean. <br>
 *
 * Casting any WebSocket message to this class in the application level is not recommended. If user needs this class
 * for any casting in application level indicates that there is a missing functionality from the given WebSocket
 * message type.<br>
 *
 * Also note that this class is public since developers might need to create WebSocket messages in the application
 * level for testing purposes and this class can be extended to use the setter methods for those purposes.</b>
 */
public abstract class WebSocketMessagePropertiesManager {

    private Map<String, Object> properties = new HashMap<>();

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public void setProperties(Map<String, Object> properties) {
        properties.entrySet().forEach(
                entry -> this.properties.put(entry.getKey(), entry.getValue())
        );
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    // WebSocket channel context related setters
    abstract void setSubProtocol(String subProtocol);
    abstract void setTarget(String target);
    abstract void setListenerPort(String listenerPort);
    abstract void setWebSocketProtocolVersion(String protocolVersion);
    abstract void setIsConnectionSecured(boolean isConnectionSecured);

    // WebSocket Session context related setters
    abstract void setServerSession(Session serverSession);
    abstract void addClientSession(Session clientSession);
}
