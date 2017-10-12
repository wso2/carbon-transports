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
package org.wso2.carbon.transport.jms.receiver;

import org.wso2.carbon.transport.jms.callback.AcknowledgementCallback;
import org.wso2.carbon.transport.jms.callback.TransactedSessionCallback;
import org.wso2.carbon.transport.jms.contract.JMSServerConnectorFuture;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * Handles a received JMS message by passing it to the relevant message processor.
 */
class JMSMessageHandler {

    private JMSServerConnectorFuture jmsServerConnectorFuture;
    private int acknowledgementMode;
    private Session session;

    /**
     * Initializes the message handler with connection details.
     *
     * @param jmsServerConnectorFuture The message processor which is going to process the received messages
     * @param serviceId              Id of the service that is interested in particular destination
     * @param session                The session that is used to create the consumer
     * @throws JMSConnectorException Throws if an error occurs when retrieving session acknowledgement mode
     */
    JMSMessageHandler(JMSServerConnectorFuture jmsServerConnectorFuture, String serviceId, Session session) throws
            JMSConnectorException {
        this.jmsServerConnectorFuture = jmsServerConnectorFuture;
        this.session = session;

        try {
            acknowledgementMode = session.getAcknowledgeMode();
        } catch (JMSException e) {
            throw new JMSConnectorException("Error retrieving acknowledgement mode", e);
        }
    }

    /**
     * Processes a received JMS message and forward to the relevant {@link JMSServerConnectorFuture}
     * <br>
     * <br>
     * Acknowledge/Commit/Rollback .etc needs to be handled within the same thread since JMS API specifies that
     * session objects should be handled within a single thread.
     * <br>
     * See <a href="https://docs.oracle.com/cd/E19340-01/820-6767/aeqdb/index.html">JMS Threading Restrictions</a>.
     * Hence, onMessage will wait for the relevant callback to complete before returning.
     *
     * @param message The received JMS Message
     * @throws JMSConnectorException Throws when an errors occurs when processing the received message
     */
    void handle(Message message) throws JMSConnectorException {
        try {
            switch (acknowledgementMode) {
                case Session.CLIENT_ACKNOWLEDGE:
                    AcknowledgementCallback acknowledgementCallback = new AcknowledgementCallback(session, this,
                            message);
                    jmsServerConnectorFuture.notifyJMSListener(message, acknowledgementCallback);
                    synchronized (this) {
                        while (!acknowledgementCallback.isOperationComplete()) {
                            wait();
                        }
                        acknowledgementCallback.updateAcknowledgementStatus();
                    }
                    break;
                case Session.SESSION_TRANSACTED:
                    TransactedSessionCallback transactedSessionCallback = new TransactedSessionCallback(session, this);
                    jmsServerConnectorFuture.notifyJMSListener(message, transactedSessionCallback);
                    synchronized (this) {
                        while (!transactedSessionCallback.isOperationComplete()) {
                            wait();
                        }
                        transactedSessionCallback.updateTransactionStatus();
                    }
                    break;
                default:
                    //Session.AUTO_ACKNOWLEDGE and Session.DUPS_OK_ACKNOWLEDGE will be handled by this
                    jmsServerConnectorFuture.notifyJMSListener(message, null);
            }

        } catch (InterruptedException e) {
            throw new JMSConnectorException("Error waiting for the operation to complete", e);
        } catch (Exception e) {
            throw new JMSConnectorException("Error while getting the message from jms provider.", e);
        }
    }
}
