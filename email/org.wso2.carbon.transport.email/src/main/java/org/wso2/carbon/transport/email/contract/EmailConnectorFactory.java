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

package org.wso2.carbon.transport.email.contract;

import org.wso2.carbon.transport.email.exception.EmailConnectorException;

import java.util.Map;

/**
 * Email Connector Factory class which provide EmailServerConnectors and EmailClientConnectors
 */
public interface EmailConnectorFactory {

    /**
     * Provide a email server connector
     *
     * @param serviceId          id used to identify the server connector instance.
     * @param connectorConfig    properties required for the {@link EmailServerConnector}.
     * @return EmailServerConnector EmailServerConnector instance.
     * @throws EmailConnectorException if any error occurred when creating the server connector.
     */
    EmailServerConnector createEmailServerConnector(String serviceId, Map<String, String> connectorConfig)
            throws EmailConnectorException;

    /**
     * Provide a email client connector
     *
     * @return EmailClientConnector EmailClientConnector instance
     * @throws EmailConnectorException if any error occurred when initializing the server connector.
     */
    EmailClientConnector createEmailClientConnector()
            throws EmailConnectorException;

}
