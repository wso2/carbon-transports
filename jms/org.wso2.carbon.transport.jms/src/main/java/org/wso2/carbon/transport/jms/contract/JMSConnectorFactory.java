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

package org.wso2.carbon.transport.jms.contract;

import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

import java.util.Map;

/**
 * Allows you create JMS server and JMS client connectors
 */
public interface JMSConnectorFactory {

    /**
     * Returns an instance of the {@link JMSServerConnector} using the given serviceId.
     *
     * @param serviceId             id used to create the server connector instance.
     * @param connectorConfig       properties required for the {@link JMSServerConnector} class.
     * @param jmsListener           listener which gets triggered when message comes.
     * @return jmsServerConnector   newly created JMS server connector instance.
     * @throws JMSConnectorException if any error occurred when creating the server connector.
     */
    JMSServerConnector createServerConnector(String serviceId, Map<String, String> connectorConfig,
                                             JMSListener jmsListener) throws JMSConnectorException;

    /**
     * Returns an instance of the {@link JMSClientConnector} class.
     * @param propertyMap       properties required for the {@link JMSClientConnector} class.
     * @return jmsClientConnector   instance.
     * @throws JMSConnectorException if any error occurred when creating the client connector.
     */
    JMSClientConnector createClientConnector(Map<String, String> propertyMap) throws JMSConnectorException;
}
