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

import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.ClientConnector;
import org.wso2.carbon.messaging.MapCarbonMessage;
import org.wso2.carbon.messaging.SerializableCarbonMessage;
import org.wso2.carbon.messaging.TextCarbonMessage;
import org.wso2.carbon.messaging.exceptions.ClientConnectorException;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.factory.CachedJMSConnectionFactory;
import org.wso2.carbon.transport.jms.factory.JMSConnectionFactory;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 * JMS sender implementation.
 */
public class JMSClientConnector implements ClientConnector {

    private MessageProducer messageProducer;
    private Session session;
    private Connection connection;
    private JMSConnectionFactory jmsConnectionFactory;

    public JMSClientConnector() {
        super();
    }

    /**
     * @return false because, in this instance, the send method with a map parameter is required.
     */
    @Override public boolean send(CarbonMessage carbonMessage, CarbonCallback carbonCallback)
            throws ClientConnectorException {
        return false;
    }

    @Override public synchronized boolean send(CarbonMessage carbonMessage, CarbonCallback carbonCallback,
                                  Map<String, String> propertyMap) throws ClientConnectorException {
        try {
            try {
                Set<Map.Entry<String, String>> propertySet = propertyMap.entrySet();
                this.createConnection(propertySet);
            } catch (JMSConnectorException e) {
                throw new ClientConnectorException(e.getMessage(), e);
            }

            Message message = null;
            String messageType = propertyMap.get(JMSConstants.JMS_MESSAGE_TYPE);

            if (carbonMessage instanceof TextCarbonMessage) {
                String textData = ((TextCarbonMessage) carbonMessage).getText();
                if (messageType.equals(JMSConstants.TEXT_MESSAGE_TYPE)) {
                    message = session.createTextMessage();
                    TextMessage textMessage = (TextMessage) message;
                    textMessage.setText(textData);
                } else if (messageType.equals(JMSConstants.BYTES_MESSAGE_TYPE)) {
                    message = session.createBytesMessage();
                    BytesMessage bytesMessage = (BytesMessage) message;
                    bytesMessage.writeBytes(textData.getBytes(Charset.defaultCharset()));
                }
            } else if (messageType.equals(JMSConstants.OBJECT_MESSAGE_TYPE) &&
                       carbonMessage instanceof SerializableCarbonMessage) {
                message = session.createObjectMessage((SerializableCarbonMessage) carbonMessage);
            } else if (messageType.equals(JMSConstants.MAP_MESSAGE_TYPE) && carbonMessage instanceof MapCarbonMessage) {
                message = session.createMapMessage();
                MapMessage mapMessage = (MapMessage) message;
                Enumeration<String> mapNames = ((MapCarbonMessage) carbonMessage).getMapNames();
                while (mapNames.hasMoreElements()) {
                    String key = mapNames.nextElement();
                    mapMessage.setString(key, ((MapCarbonMessage) carbonMessage).getValue(key));
                }
            }

            if (carbonMessage.getProperty(JMSConstants.PERSISTENCE) != null &&
                carbonMessage.getProperty(JMSConstants.PERSISTENCE).equals(false)) {
                messageProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            }

            if (message != null) {
                messageProducer.send(message);
            } else {
              throw new ClientConnectorException("Exception occured while creating the message");
            }

        } catch (JMSException e) {
            throw new ClientConnectorException("Exception occurred while sending the message", e);
        } finally {
            try {
                jmsConnectionFactory.closeMessageProducer(messageProducer);
                jmsConnectionFactory.closeSession(session);
                jmsConnectionFactory.closeConnection(connection);
            } catch (JMSConnectorException e) {
                throw new ClientConnectorException(e.getMessage(), e);
            }
        }
        return false;
    }

    /**
     * To create jms connection.
     *
     * @param propertySet      Set of user defined properties
     * @throws JMSConnectorException
     * @throws JMSException
     */
    private void createConnection(Set<Map.Entry<String, String>> propertySet)
            throws JMSConnectorException, JMSException {
        Properties properties = new Properties();
        for (Map.Entry<String, String> entry : propertySet) {
            String mappedParameter = JMSConstants.MAPPING_PARAMETERS.get(entry.getKey());
            if (mappedParameter != null) {
                properties.put(mappedParameter, entry.getValue());
            } else {
                properties.put(entry.getKey(), entry.getValue());
            }
        }
        JMSConnectionFactory jmsConnectionFactory;
        if ((Integer.parseInt(properties.getProperty(JMSConstants.PARAM_CACHE_LEVEL)) > JMSConstants.CACHE_NONE) &&
            this.jmsConnectionFactory != null) {
            jmsConnectionFactory = this.jmsConnectionFactory;
        } else {
            jmsConnectionFactory = new CachedJMSConnectionFactory(properties);
            this.jmsConnectionFactory = jmsConnectionFactory;
        }

        String conUsername = properties.getProperty(JMSConstants.CONNECTION_USERNAME);
        String conPassword = properties.getProperty(JMSConstants.CONNECTION_PASSWORD);

        Connection connection;
        if (conUsername != null && conPassword != null) {
            connection = jmsConnectionFactory.getConnection(conUsername, conPassword);
        } else {
            connection = jmsConnectionFactory.getConnection();
        }

        this.connection = connection;

        Session session = jmsConnectionFactory.getSession(connection);
        this.session = session;

        Destination destination = jmsConnectionFactory.getDestination(session);
        MessageProducer messageProducer = jmsConnectionFactory.createMessageProducer(session, destination);
        this.messageProducer = messageProducer;
    }

    @Override public String getProtocol() {
        return "jms";
    }

    @Override
    public void setMessageProcessor(CarbonMessageProcessor messageProcessor) {
        // Message processor is not needed with regards to jms client connector
    }
}
