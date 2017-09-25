/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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
package org.wso2.carbon.transport.jms.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.jms.clientfactory.ExtendedJMSClientConnectionFactory;
import org.wso2.carbon.transport.jms.clientfactory.JMSConnectionFactoryManager;
import org.wso2.carbon.transport.jms.clientfactory.SessionWrapper;
import org.wso2.carbon.transport.jms.contract.JMSClientConnector;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

import java.util.Map;
import java.util.Properties;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * JMS sender implementation.
 */
public class JMSClientConnectorImpl implements JMSClientConnector {

    private static final Logger logger = LoggerFactory.getLogger(JMSClientConnectorImpl.class);

    private ExtendedJMSClientConnectionFactory jmsConnectionFactory;

    public JMSClientConnectorImpl(Map<String, String> propertyMap) throws JMSConnectorException {
        setupConnectionFactory(propertyMap);
    }

    @Override
    public boolean send(Message jmsMessage, String destinationName) throws JMSConnectorException {
        Destination destination;
        SessionWrapper sessionWrapper = null;
        try {
            sessionWrapper = jmsConnectionFactory.getSessionWrapper();
            destination = jmsConnectionFactory.createDestination(sessionWrapper.getSession(), destinationName);
            sessionWrapper.getMessageProducer().send(destination, jmsMessage);
        } catch (JMSException e) {
            throw new JMSConnectorException("JMS Send Failed with [" + e.getMessage() + " ]", e);
        } catch (Exception e) {
            throw new JMSConnectorException("Error getting the session. " + e.getMessage(), e);
        } finally {
            if (sessionWrapper != null) {
                jmsConnectionFactory.returnSessionWrapper(sessionWrapper);
            }
        }
        return false;
    }

    @Override
    public boolean sendTransactedMessage(Message jmsMessage, String destinationName, SessionWrapper sessionWrapper)
            throws JMSConnectorException {
        Destination destination;
        try {
            destination = jmsConnectionFactory.createDestination(sessionWrapper.getSession(), destinationName);
            sessionWrapper.getMessageProducer().send(destination, jmsMessage);
        } catch (JMSException e) {
            throw new JMSConnectorException("JMS Send Failed with [" + e.getMessage() + " ]", e);
        } catch (Exception e) {
            throw new JMSConnectorException("Error getting the session. " + e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Message createJMSMessage(String messageType) throws JMSConnectorException {
        Message jmsMessage = null;
        SessionWrapper sessionWrapper = null;
        try {
            sessionWrapper = jmsConnectionFactory.getSessionWrapper();
            Session session = sessionWrapper.getSession();
            switch (messageType) {
            case JMSConstants.TEXT_MESSAGE_TYPE:
                jmsMessage = session.createTextMessage();
                break;
            case JMSConstants.MAP_MESSAGE_TYPE:
                jmsMessage = session.createMapMessage();
                break;
            case JMSConstants.BYTES_MESSAGE_TYPE:
                jmsMessage = session.createBytesMessage();
                break;
            case JMSConstants.STREAM_MESSAGE_TYPE:
                jmsMessage = session.createStreamMessage();
                break;
            case JMSConstants.OBJECT_MESSAGE_TYPE:
                jmsMessage = session.createMapMessage();
                break;
            default:
                logger.error("Unsupported JMS Message type");
            }

        } catch (JMSException e) {
            throw new JMSConnectorException("Error creating the JMS Message. " + e.getMessage(), e);
        } catch (Exception e) {
            throw new JMSConnectorException("Error getting the session. " + e.getMessage(), e);
        } finally {
            if (sessionWrapper != null) {
                jmsConnectionFactory.returnSessionWrapper(sessionWrapper);
            }
        }
        return jmsMessage;
    }

    /**
     * Creates a new {@link MessageProducer} using a new or existing connection to the JMS provider.
     *
     * @param propertyMap Map of user defined properties
     * @throws JMSConnectorException throws when an internal error occur trying to make the JMS provider connection
     */
    private void setupConnectionFactory(Map<String, String> propertyMap) throws JMSConnectorException {
        try {
            Properties properties = new Properties();
            properties.putAll(propertyMap);

            jmsConnectionFactory = JMSConnectionFactoryManager.getInstance().getJMSConnectionFactory(properties);

        } catch (JMSConnectorException e) {
            throw new JMSConnectorException("Error connecting to JMS provider. " + e.getMessage(), e);
        }
    }

    @Override
    public SessionWrapper acquireSession() throws JMSConnectorException {
        SessionWrapper sessionWrapper;
        try {
            sessionWrapper = jmsConnectionFactory.getSessionWrapper();
        } catch (Exception e) {
            throw new JMSConnectorException("Error when acquiring the session. ", e);
        }
        return sessionWrapper;
    }

    @Override
    public void releaseSession(SessionWrapper sessionWrapper)
            throws JMSConnectorException {
        jmsConnectionFactory.returnSessionWrapper(sessionWrapper);
    }
}
