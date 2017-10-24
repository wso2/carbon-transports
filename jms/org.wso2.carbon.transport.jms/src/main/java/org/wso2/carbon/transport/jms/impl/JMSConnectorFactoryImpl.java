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

package org.wso2.carbon.transport.jms.impl;

import org.wso2.carbon.transport.jms.contract.JMSClientConnector;
import org.wso2.carbon.transport.jms.contract.JMSConnectorFactory;
import org.wso2.carbon.transport.jms.contract.JMSListener;
import org.wso2.carbon.transport.jms.contract.JMSServerConnector;
import org.wso2.carbon.transport.jms.contract.JMSServerConnectorFuture;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.receiver.JMSServerConnectorImpl;
import org.wso2.carbon.transport.jms.sender.JMSClientConnectorImpl;

import java.util.Map;

/**
 * Implementation of JMSConnectorFactory interface
 */
public class JMSConnectorFactoryImpl implements JMSConnectorFactory {

    @Override
    public JMSServerConnector createServerConnector(String serviceId, Map<String, String> connectorConfig,
                                                    JMSListener jmsListener) throws JMSConnectorException {
        JMSServerConnectorFuture jmsServerConnectorFuture = new JMSServerConnectorFutureImpl(jmsListener);
        return new JMSServerConnectorImpl(serviceId, connectorConfig, jmsServerConnectorFuture);
    }

    @Override
    public JMSClientConnector createClientConnector(Map<String, String> propertyMap) throws JMSConnectorException {
        return new JMSClientConnectorImpl(propertyMap);
    }
}
