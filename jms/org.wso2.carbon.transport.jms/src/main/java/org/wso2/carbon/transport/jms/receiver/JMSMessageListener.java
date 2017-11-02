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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.jms.contract.JMSListener;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * JMS Message Listener which listens to a queue/topic in asynchronous manner.
 */
public class JMSMessageListener implements javax.jms.MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(JMSMessageListener.class);
    private JMSListener jmsListener;
    private String serviceId;
    private int acknowledgementMode;
    private Session session;

    /**
     * Handler which handles a received message.
     */
    private JMSMessageHandler messageHandler;

    /**
     * Creates a jms message receiver which receives message from a particular queue or topic.
     *
     * @param jmsListener Message where the relevant jms message should be passed to.
     * @param serviceId        Id of the service that is interested in particular destination.
     * @param session          Relevant session that is listening to the jms destination.
     * @throws JMSConnectorException
     */
    JMSMessageListener(JMSListener jmsListener, String serviceId, Session session) throws
            JMSConnectorException {
        this.jmsListener = jmsListener;
        this.serviceId = serviceId;
        this.session = session;
        this.messageHandler = new JMSMessageHandler(jmsListener, serviceId, session);
    }

    /**
     * Message is passed to application level, once the jms message is delivered.
     *
     * @param message the next received message.
     */
    @Override
    public void onMessage(Message message) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace("Message Received. MessageId : " + message.getJMSMessageID());
            }
            messageHandler.handle(message);
        } catch (JMSConnectorException e) {
            throw new RuntimeException("An error occurred while listening to messages", e);
        } catch (JMSException e) {
            throw new RuntimeException("Error retrieving messageId from the received message", e);
        }
    }

}
