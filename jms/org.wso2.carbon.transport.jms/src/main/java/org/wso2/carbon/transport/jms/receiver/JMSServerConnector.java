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
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

/**
 * This is a transport receiver for JMS.
 */
public class JMSServerConnector extends ServerConnector {
    private static final Logger logger = LoggerFactory.getLogger(JMSServerConnector.class);
    /**
     * The {@link CarbonMessageProcessor} instance represents the carbon message processor that handles the out going
     * messages.
     */
    private CarbonMessageProcessor carbonMessageProcessor;
    /**
     * The {@link JMSConnectionFactory} instance represents the jms connection factory related with this server
     * connector.
     */
    private JMSConnectionFactory jmsConnectionFactory = null;
    /**
     * The {@link Connection} instance represents the jms connection related with this server connector.
     */
    private Connection connection;
    /**
     * The {@link Session} instance represents the jms session related with this server connector.
     */
    private Session session;
    /**
     * The {@link Destination} instance represents a particular jms destination, this server connector listening to.
     */
    private Destination destination;
    /**
     * The {@link MessageConsumer} instance represents a particular jms consumer, this server related with
     */
    private MessageConsumer messageConsumer;
    /**
     * The {@link String} instance represents the jms connection user-name.
     */
    private String userName;
    /**
     * The {@link String} instance represents the jms connection password.
     */
    private String password;
    /**
     * The {@link Properties} instance represents the jms connection properties.
     */
    private Properties properties;
    /**
     * The retry interval (in milli seconds) if the connection is lost or if the connection cannot be established.
     */
    private long retryInterval = 10000;
    /**
     * The maximum retry count, for retrying to establish a jms connection with the jms provider.
     */
    private int maxRetryCount = 5;

    /**
     * Tells to use a message receiver instead of a message listener.
     */
    private boolean useReceiver = false;

    /**
     * The exception listner that listens to exceptions from this connector.
     */
    private ExceptionListener exceptionListener;

    /**
     * The retry handle which is going to retry connections when failed from this connector.
     */
    private JMSConnectionRetryHandler retryHandler;

    /**
     * Creates a jms server connector with the id.
     *
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
     * Create a connection using the given properties.
     *
     * @param exceptionListener The exception listner which handles exception of the created consumers
     * @throws JMSConnectorException when consumer creation is failed due to a JMS layer error
     */
    private void createConsumer(ExceptionListener exceptionListener) throws JMSConnectorException {
        try {
            if (null != userName && null != password) {
                connection = jmsConnectionFactory.createConnection(userName, password);
            } else {
                connection = jmsConnectionFactory.createConnection();
            }
            connection.setExceptionListener(exceptionListener);
            jmsConnectionFactory.start(connection);
            session = jmsConnectionFactory.createSession(connection);
            destination = jmsConnectionFactory.getDestination(session);
            messageConsumer = jmsConnectionFactory.createMessageConsumer(session, destination);
        } catch (JMSException e) {
            throw new JMSConnectorException("Error occurred while creating a connection", e);
        }
    }

    /**
     * Create a message listener to a particular jms destination.
     *
     * @throws JMSConnectorException JMS Connector exception can be thrown when trying to connect to jms provider
     */
    void createMessageListener() throws JMSConnectorException {

        try {
            messageConsumer.setMessageListener(new JMSMessageListener(carbonMessageProcessor, id, session));

            if (logger.isDebugEnabled()) {
                logger.debug("Message listener created");
            }

        } catch (JMSException e) {
            throw new JMSConnectorException("Error while initializing message listener", e);
        }
    }

    /**
     * Create a message receiver to retrieve messages.
     *
     * @throws JMSConnectorException Can be thrown when initializing the message handler or receiving messages
     */
    private void createMessageReceiver() throws JMSConnectorException {

        if (logger.isDebugEnabled()) {
            logger.debug("Creating message receiver");
        }

        JMSMessageReceiver messageReceiver =
                new JMSMessageReceiver(carbonMessageProcessor, id, session, messageConsumer);
        messageReceiver.receive();
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
        messageConsumer = null;
        session = null;
        connection = null;
    }

    /**
     * To get the jms connection.
     *
     * @return JMS Connection
     */
    Connection getConnection() {
        return connection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMessageProcessor(CarbonMessageProcessor carbonMessageProcessor) {
        this.carbonMessageProcessor = carbonMessageProcessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() throws ServerConnectorException {
        /*
        not needed for jms, as this will be called in server start-up. We will not know about the destination at server
        start-up. We will get to know about that in service deployment.
        */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws JMSConnectorException {
        closeAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() throws JMSConnectorException {
        closeAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void beginMaintenance() throws JMSConnectorException {
        jmsConnectionFactory.stop(connection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void endMaintenance() throws JMSConnectorException {
        jmsConnectionFactory.start(connection);
    }

    /**
     * {@inheritDoc}
     */
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
        String retryIntervalParam = map.get(JMSConstants.RETRY_INTERVAL);
        if (retryIntervalParam != null) {
            try {
                this.retryInterval = Long.parseLong(retryIntervalParam);
            } catch (NumberFormatException ex) {
                logger.error("Provided value for retry interval is invalid, using the default retry interval value "
                        + this.retryInterval);
            }
        }

        String maxRetryCountParam = map.get(JMSConstants.MAX_RETRY_COUNT);
        if (maxRetryCountParam != null) {
            try {
                this.maxRetryCount = Integer.parseInt(maxRetryCountParam);
            } catch (NumberFormatException ex) {
                logger.error("Provided value for max retry count is invalid, using the default max retry count "
                        + this.maxRetryCount);
            }
        }

        String useReceiverParam = map.get(JMSConstants.USE_RECEIVER);

        if (useReceiverParam != null) {
            useReceiver = Boolean.parseBoolean(useReceiverParam);
        }

        exceptionListener = new JMSExceptionListener(this);
        retryHandler = new JMSConnectionRetryHandler(this, retryInterval, maxRetryCount);

        startConsuming();
    }

    /**
     * Start message consuming threads.
     *
     * @throws JMSConnectorException when consuming thread start fails
     */
    void startConsuming()
            throws JMSConnectorException {

        try {

            jmsConnectionFactory = new JMSConnectionFactory(properties);

            createConsumer(exceptionListener);

            if (useReceiver) {
                createMessageReceiver();
            } else {
                createMessageListener();
            }

        } catch (JMSConnectorException e) {
            if (null == jmsConnectionFactory) {
                throw new JMSConnectorException("Cannot create the jms connection factory. please check the connection"
                        + " properties and re-deploy the jms service. " + e.getMessage());
            } else if (connection != null) {
                closeAll();
                throw e;
            }

            closeAll();

            if (!retryHandler.retry()) {
                throw new JMSConnectorException("Retry was not successful");
            }

        }
    }
}
