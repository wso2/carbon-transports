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

import org.wso2.carbon.transport.jms.clientfactory.SessionWrapper;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

import java.util.Map;
import javax.jms.Message;

/**
 * Allows to send outbound messages
 */
public interface JMSClientConnector {

    /**
     * Message sending logic to send message to a backend endpoint. Additionally, this method accepts a map of
     * parameters that is used as data to create the connection and construct the message to be send.
     *
     * @param message     the carbon message used with sending the a message to backend.
     * @return return true if the sending was successful, false otherwise.
     * @throws JMSConnectorException on error while trying to send message to backend.
     */
    boolean send(Message message, String destinationName) throws JMSConnectorException;

    Message createJMSMessage(String messageType) throws JMSConnectorException;

    SessionWrapper acquireSession() throws JMSConnectorException;

    boolean sendTransactedMessage(Message jmsMessage, String destinationName, SessionWrapper sessionWrapper)
            throws JMSConnectorException;

    void releaseSession(SessionWrapper sessionWrapper) throws JMSConnectorException;

}
