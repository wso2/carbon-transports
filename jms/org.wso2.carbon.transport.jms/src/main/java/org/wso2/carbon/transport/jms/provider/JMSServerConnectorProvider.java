/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.transport.jms.provider;

import org.wso2.carbon.messaging.ServerConnector;
import org.wso2.carbon.messaging.ServerConnectorProvider;
import org.wso2.carbon.transport.jms.listener.JMSServerConnector;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

import java.util.List;
import java.util.Map;

/**
 * Server connector provider for jms.
 */
public class JMSServerConnectorProvider extends ServerConnectorProvider {

    /**
     * Creates a server connector provider for jms with the protocol name.
     */
    public JMSServerConnectorProvider() {
        super(JMSConstants.PROTOCOL_JMS);
    }

    /**
     * Creates a server connector provider with the protocol name.
     *
     * @param protocol Name of the protocol
     */
    public JMSServerConnectorProvider(String protocol) {
        super(protocol);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ServerConnector> initializeConnectors() {
        return null;
    }

    @Override
    public ServerConnector createConnector(String id, Map<String, String> properties) {
        return new JMSServerConnector(id, properties);
    }
}
