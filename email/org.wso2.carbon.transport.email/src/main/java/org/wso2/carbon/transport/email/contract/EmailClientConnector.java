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

import org.wso2.carbon.transport.email.contract.message.EmailBaseMessage;
import org.wso2.carbon.transport.email.exception.EmailConnectorException;

import java.util.Map;

/**
 * A Client Connector interface to send the email message to backend.
 */
public interface EmailClientConnector {

     /**
      * Create the connection that need to be send the email. Additionally, this method accepts a map of parameters that
      * is needed to create the connection.
      *
      * @param initialProperties A Map of properties that need to create the connection.
      * @throws EmailConnectorException on error while trying to create the connection.
      */
     void init(Map<String, String> initialProperties) throws EmailConnectorException;

     /**
      * Message sending logic to send message to a backend endpoint. Additionally, this method accepts a map of
      * parameters that is used as data to construct the message to be send.
      *
      * @param emailMessage the email message used with sending the a message to backend.
      * @throws EmailConnectorException on error while trying to send message to backend.
      */
     void send(EmailBaseMessage emailMessage)
            throws EmailConnectorException;
}
