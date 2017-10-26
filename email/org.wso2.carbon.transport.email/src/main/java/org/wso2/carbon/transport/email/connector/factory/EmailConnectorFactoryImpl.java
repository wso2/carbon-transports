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

package org.wso2.carbon.transport.email.connector.factory;

import org.wso2.carbon.transport.email.client.connector.EmailClientConnectorImpl;
import org.wso2.carbon.transport.email.contract.EmailClientConnector;
import org.wso2.carbon.transport.email.contract.EmailConnectorFactory;
import org.wso2.carbon.transport.email.contract.EmailServerConnector;
import org.wso2.carbon.transport.email.exception.EmailConnectorException;
import org.wso2.carbon.transport.email.server.connector.contractimpl.EmailServerConnectorImpl;

import java.util.Map;

/**
 * Implementation for {@link EmailConnectorFactory}.
 */
public class EmailConnectorFactoryImpl implements EmailConnectorFactory {
    @Override
    public EmailServerConnector createEmailServerConnector(String serviceId,
            Map<String, String> connectorConfig)
            throws EmailConnectorException {
        return new EmailServerConnectorImpl(serviceId, connectorConfig);
    }

    @Override
    public EmailClientConnector createEmailClientConnector()
            throws EmailConnectorException {
        return new EmailClientConnectorImpl();
    }
}
