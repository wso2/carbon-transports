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

package org.wso2.carbon.transport.jms.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.ServerConnector;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.factory.JMSConnectionFactory;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

/**
 * This is a transport listener for JMS.
 */
public class JMSServerConnector extends ServerConnector {
    private static final Logger logger = LoggerFactory.getLogger(JMSServerConnector.class);
    private CarbonMessageProcessor carbonMessageProcessor;
    private JMSConnectionFactory jmsConnectionFactory = null;
    private Connection connection;
    private Session session;
    private Destination destination;
    private MessageConsumer messageConsumer;
    private String userName;
    private String password;
    private Properties properties;
    private long retryInterval = 10000;
    private int maxRetryCount = 5;

    /**
     * Creates a jms server connector with the id.
     * @param id Unique identifier for the server connector.
     */
    public JMSServerConnector(String id) {
        super(id);
    }

    /**
     * Creates a jms server connector with the protocol name.
     */
    public JMSServerConnector() {
        super(JMSConstants.PROTOCOL_JMS);
    }

    /**
     * To create a message listener to a particular jms destination.
     * @throws JMSConnectorException JMS Connector exception can be thrown when trying to connect to jms provider
     */
    void createMessageListener() throws JMSConnectorException {
        try {
            if (null != userName && null != password) {
                connection = jmsConnectionFactory.createConnection(userName, password);
            } else {
                connection = jmsConnectionFactory.createConnection();
            }
            connection.setExceptionListener(new JMSExceptionListener(this, retryInterval, maxRetryCount));
            jmsConnectionFactory.start(connection);
            session = jmsConnectionFactory.createSession(connection);
            destination = jmsConnectionFactory.getDestination(session);
            messageConsumer = jmsConnectionFactory.createMessageConsumer(session, destination);
            messageConsumer.setMessageListener(
                    new JMSMessageListener(carbonMessageProcessor, id, session.getAcknowledgeMode(), session));
        } catch (RuntimeException e) {
            throw new JMSConnectorException("Error while creating the connection from connection factory", e);
        } catch (JMSException e) {
            throw new JMSConnectorException("Error while creating the connection from the connection factory. ", e);
        }
    }

    /**
     * Close the connection, session and consumer.
     *
     * @throws JMSConnectorException Exception that can be thrown when trying to close the connection, session
     *                               and message consumer
     */
    void closeAll() throws JMSConnectorException {
        jmsConnectionFactory.closeMessageConsumer(messageConsumer);
        jmsConnectionFactory.closeSession(session);
        jmsConnectionFactory.closeConnection(connection);
    }

    /**
     * To get the jms connection.
     *
     * @return JMS Connection
     */
    Connection getConnection() {
        return connection;
    }

    @Override
    public void setMessageProcessor(CarbonMessageProcessor carbonMessageProcessor) {
        this.carbonMessageProcessor = carbonMessageProcessor;
    }

    @Override
    public void init() throws ServerConnectorException {
        /*
        not needed for jms, as this will be called in server start-up. We will not know about the destination at server
        start-up. We will get to know about that in service deployment.
        */
    }

    @Override
    public void destroy() throws JMSConnectorException {
        closeAll();
    }

    @Override
    public void stop() throws JMSConnectorException {
        closeAll();
    }

    @Override
    protected void beginMaintenance() throws JMSConnectorException {
        jmsConnectionFactory.stop(connection);
    }

    @Override
    protected void endMaintenance() throws JMSConnectorException {
        jmsConnectionFactory.start(connection);
    }

    @Override
    public void start(Map<String, String> map) throws ServerConnectorException {
        properties = new Properties();
        Set<Map.Entry<String, String>> set = map.entrySet();
        for (Map.Entry<String, String> entry : set) {
            String mappedParameter = JMSConstants.MAPPING_PARAMETERS.get(entry.getKey());
            if (mappedParameter != null) {
                properties.put(mappedParameter, entry.getValue());
            }
        }

        userName = map.get(JMSConstants.CONNECTION_USERNAME);
        password = map.get(JMSConstants.CONNECTION_PASSWORD);
        String retryInterval = map.get(JMSConstants.RETRY_INTERVAL);
        if (retryInterval != null) {
            try {
                this.retryInterval = Long.parseLong(retryInterval);
            } catch (NumberFormatException ex) {
                logger.error("Provided value for retry interval is invalid, using the default retry interval value "
                        + this.retryInterval);
            }
        }

        String maxRetryCount = map.get(JMSConstants.MAX_RETRY_COUNT);
        if (maxRetryCount != null) {
            try {
                this.maxRetryCount = Integer.parseInt(maxRetryCount);
            } catch (NumberFormatException ex) {
                logger.error("Provided value for max retry count is invalid, using the default max retry count "
                        + this.maxRetryCount);
            }
        }

        try {
            jmsConnectionFactory = new JMSConnectionFactory(properties);
            createMessageListener();
        } catch (JMSConnectorException e) {
            if (null == jmsConnectionFactory) {
                throw new JMSConnectorException("Cannot create the jms connection factory. please check the connection"
                        + " properties and re-deploy the jms service. " + e.getMessage());
            } else if (connection != null) {
                closeAll();
                throw e;
            }
            closeAll();
            JMSConnectionRetryHandler jmsConnectionRetryHandler = new JMSConnectionRetryHandler(this,
                    this.retryInterval, this.maxRetryCount);
            jmsConnectionRetryHandler.start();

        }
    }
}
