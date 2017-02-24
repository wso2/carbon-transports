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

package org.wso2.carbon.transport.jms.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.utils.JMSConstants;
import org.wso2.carbon.transport.jms.utils.JMSUtils;

import java.util.Properties;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

/**
 * JMSConnectionFactory that handles the JMS Connection, Session creation and closing.
 */
public class JMSConnectionFactory implements ConnectionFactory, QueueConnectionFactory, TopicConnectionFactory {
    private static final Logger logger = LoggerFactory.getLogger(JMSConnectionFactory.class);
    /**
     * The {@link Context} instance representing initial Context.
     */
    private Context ctx;
    /**
     * The {@link ConnectionFactory} instance representing jms connection factory.
     */
    private ConnectionFactory connectionFactory;
    /**
     * The {@link String} instance representing the connection factory JNDI name.
     */
    private String connectionFactoryString;
    /**
     * Represents whether to listen queue or topic.
     */
    private JMSConstants.JMSDestinationType destinationType;
    /**
     * The {@link Destination} instance representing the jms destination listening to.
     */
    private Destination destination;
    /**
     * The {@link String} instance representing the jms destination name.
     */
    private String destinationName;
    /**
     * The {@link Boolean} instance representing whether the session is transacted or not.
     */
    private boolean transactedSession = false;
    /**
     * The {@link Integer} instance representing the session acknowledgement mode.
     */
    private int sessionAckMode = Session.AUTO_ACKNOWLEDGE;
    /**
     * The {@link String} instance representing the jms spec version.
     */
    private String jmsSpec;
    /**
     * The {@link Boolean} instance representing whether subscription is durable or not.
     */
    private boolean isDurable;
    /**
     * The {@link Boolean} instance representing whether to create a pub-sub connection.
     */
    private boolean noPubSubLocal;
    /**
     * The {@link String} instance representing the client id of the durable subscription.
     */
    private String clientId;
    /**
     * The {@link String} instance representing the subscription name.
     */
    private String subscriptionName;
    /**
     * The {@link String} instance representing the message selector.
     */
    private String messageSelector;
    /**
     * The {@link Boolean} instance representing whether it is a shared subscription or not.
     */
    private boolean isSharedSubscription;

    /**
     * Initialization of JMS ConnectionFactory with the user specified properties.
     *
     * @param properties Properties to be added to the initial context
     * @throws JMSConnectorException Thrown when initial context name is wrong or when creating connection factory.
     */
    public JMSConnectionFactory(Properties properties) throws JMSConnectorException {
        try {
            ctx = new InitialContext(properties);
        } catch (NamingException e) {
            throw new JMSConnectorException("NamingException while obtaining initial context. ", e);
        }

        String connectionFactoryType = properties.getProperty(JMSConstants.CONNECTION_FACTORY_TYPE);
        if (JMSConstants.DESTINATION_TYPE_TOPIC.equalsIgnoreCase(connectionFactoryType)) {
            this.destinationType = JMSConstants.JMSDestinationType.TOPIC;
        } else {
            this.destinationType = JMSConstants.JMSDestinationType.QUEUE;
        }

        String jmsSpecVersion = properties.getProperty(JMSConstants.PARAM_JMS_SPEC_VER);

        if (null == jmsSpecVersion) {
            jmsSpec = JMSConstants.JMS_SPEC_VERSION_1_1;
        } else {
            switch (jmsSpecVersion) {
            case JMSConstants.JMS_SPEC_VERSION_1_1:
                jmsSpec = JMSConstants.JMS_SPEC_VERSION_1_1;
                break;
            case JMSConstants.JMS_SPEC_VERSION_2_0:
                jmsSpec = JMSConstants.JMS_SPEC_VERSION_2_0;
                break;
            case JMSConstants.JMS_SPEC_VERSION_1_0:
                jmsSpec = JMSConstants.JMS_SPEC_VERSION_1_0;
                break;
            default:
                jmsSpec = JMSConstants.JMS_SPEC_VERSION_1_1;
            }
        }
        isSharedSubscription = "true"
                .equalsIgnoreCase(properties.getProperty(JMSConstants.PARAM_IS_SHARED_SUBSCRIPTION));

        noPubSubLocal = Boolean.valueOf(properties.getProperty(JMSConstants.PARAM_PUBSUB_NO_LOCAL));

        clientId = properties.getProperty(JMSConstants.PARAM_DURABLE_SUB_CLIENT_ID);
        subscriptionName = properties.getProperty(JMSConstants.PARAM_DURABLE_SUB_NAME);

        if (isSharedSubscription && subscriptionName == null) {
            logger.warn("Subscription name is not given. Therefore declaring a non-shared subscription");
            isSharedSubscription = false;
        }

        String subDurable = properties.getProperty(JMSConstants.PARAM_SUB_DURABLE);
        if (null != subDurable) {
            isDurable = Boolean.parseBoolean(subDurable);
        }
        String msgSelector = properties.getProperty(JMSConstants.PARAM_MSG_SELECTOR);
        if (null != msgSelector) {
            messageSelector = msgSelector;
        }
        this.connectionFactoryString = properties.getProperty(JMSConstants.CONNECTION_FACTORY_JNDI_NAME);
        if (null == connectionFactoryString || "".equals(connectionFactoryString)) {
            connectionFactoryString = "QueueConnectionFactory";
        }

        this.destinationName = properties.getProperty(JMSConstants.DESTINATION_NAME);
        String strSessionAck = properties.getProperty(JMSConstants.SESSION_ACK);
        if (null == strSessionAck) {
            sessionAckMode = Session.AUTO_ACKNOWLEDGE;
        } else if (strSessionAck.equals(JMSConstants.CLIENT_ACKNOWLEDGE_MODE)) {
            sessionAckMode = Session.CLIENT_ACKNOWLEDGE;
        } else if (strSessionAck.equals(JMSConstants.DUPS_OK_ACKNOWLEDGE_MODE)) {
            sessionAckMode = Session.DUPS_OK_ACKNOWLEDGE;
        } else if (strSessionAck.equals(JMSConstants.SESSION_TRANSACTED_MODE)) {
            sessionAckMode = Session.SESSION_TRANSACTED;
            transactedSession = true;
        }
        createConnectionFactory();
    }

    /**
     * To get the JMS Connection Factory.
     *
     * @return JMS Connection Factory
     * @throws JMSConnectorException Thrown when creating jms connection.
     */
    public ConnectionFactory getConnectionFactory() throws JMSConnectorException {
        if (this.connectionFactory != null) {
            return this.connectionFactory;
        }

        return createConnectionFactory();
    }

    /**
     * To create the JMS Connection Factory.
     *
     * @return JMS Connection Factory
     * @throws JMSConnectorException Thrown when creating {@link ConnectionFactory} instance.
     */
    private ConnectionFactory createConnectionFactory() throws JMSConnectorException {
        if (null != this.connectionFactory) {
            return this.connectionFactory;
        }
        try {
            if (JMSConstants.JMSDestinationType.QUEUE.equals(this.destinationType)) {
                this.connectionFactory = (QueueConnectionFactory) ctx.lookup(this.connectionFactoryString);
            } else if (JMSConstants.JMSDestinationType.TOPIC.equals(this.destinationType)) {
                this.connectionFactory = (TopicConnectionFactory) ctx.lookup(this.connectionFactoryString);
            }
        } catch (NamingException e) {
            throw new JMSConnectorException(
                    "Naming exception while obtaining connection factory for " + this.connectionFactoryString, e);
        }
        return this.connectionFactory;
    }

    /**
     * To create a connection.
     *
     * @return JMS connection
     * @throws JMSException Thrown when creating jms connection.
     */
    public Connection getConnection() throws JMSException {
        return createConnection();
    }

    /**
     * To create a connection to a password protected connection factory.
     *
     * @param userName Valid username
     * @param password valid password
     * @return JMS connection
     * @throws JMSException Thrown when creating jms connection.
     */
    public Connection getConnection(String userName, String password) throws JMSException {
        return createConnection(userName, password);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection createConnection() throws JMSException {
        if (null == connectionFactory) {
            throw new JMSException("Connection cannot be establish to the broker. Connection Factory is null. Please "
                    + "check the Please check the broker libs provided.");
        }
        Connection connection = null;
        try {

            if (JMSConstants.JMS_SPEC_VERSION_1_1.equals(jmsSpec)) {
                if (JMSConstants.JMSDestinationType.QUEUE.equals(this.destinationType)) {
                    connection = ((QueueConnectionFactory) (this.connectionFactory)).createQueueConnection();
                } else if (JMSConstants.JMSDestinationType.TOPIC.equals(this.destinationType)) {
                    connection = ((TopicConnectionFactory) (this.connectionFactory)).createTopicConnection();
                    if (isDurable) {
                        connection.setClientID(clientId);
                    }
                }
                return connection;
            } else {
                QueueConnectionFactory qConFac = null;
                TopicConnectionFactory tConFac = null;
                if (JMSConstants.JMSDestinationType.QUEUE.equals(this.destinationType)) {
                    qConFac = (QueueConnectionFactory) this.connectionFactory;
                } else {
                    tConFac = (TopicConnectionFactory) this.connectionFactory;
                }
                if (null != qConFac) {
                    connection = qConFac.createQueueConnection();
                } else {
                    connection = tConFac.createTopicConnection();
                }
                if (isDurable && !isSharedSubscription) {
                    connection.setClientID(clientId);
                }
                return connection;
            }
        } catch (JMSException e) {
            // Need to close the connection in the case if durable subscriptions
            if (null != connection) {
                try {
                    connection.close();
                } catch (Exception ex) {
                    logger.error("Error while closing the connection. ", ex);
                }
            }
            throw e;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection createConnection(String userName, String password) throws JMSException {
        Connection connection = null;
        try {
            if (JMSConstants.JMS_SPEC_VERSION_1_1.equals(jmsSpec)) {
                if (JMSConstants.JMSDestinationType.QUEUE.equals(this.destinationType)) {
                    connection = ((QueueConnectionFactory) (this.connectionFactory))
                            .createQueueConnection(userName, password);
                } else if (JMSConstants.JMSDestinationType.TOPIC.equals(this.destinationType)) {
                    connection = ((TopicConnectionFactory) (this.connectionFactory))
                            .createTopicConnection(userName, password);
                    if (isDurable) {
                        connection.setClientID(clientId);
                    }
                }
                return connection;
            } else {
                QueueConnectionFactory qConFac = null;
                TopicConnectionFactory tConFac = null;
                if (JMSConstants.JMSDestinationType.QUEUE.equals(this.destinationType)) {
                    qConFac = (QueueConnectionFactory) this.connectionFactory;
                } else {
                    tConFac = (TopicConnectionFactory) this.connectionFactory;
                }
                if (null != qConFac) {
                    connection = qConFac.createQueueConnection(userName, password);
                } else if (null != tConFac) {
                    connection = tConFac.createTopicConnection(userName, password);
                }
                if (isDurable && !isSharedSubscription) {
                    if (connection != null) {
                        connection.setClientID(clientId);
                    } else {
                        throw new JMSException("Connection is null. Cannot set client ID " + clientId
                                               + "for durable subscription");
                    }
                }
                return connection;
            }
        } catch (JMSException e) {
            // Need to close the connection in the case if durable subscriptions
            if (null != connection) {
                try {
                    connection.close();
                } catch (Exception ex) {
                    logger.error("Error while closing the connection", ex);
                }
            }
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JMSContext createContext() {
        return connectionFactory.createContext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JMSContext createContext(int sessionMode) {
        return connectionFactory.createContext(sessionMode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JMSContext createContext(String userName, String password) {
        return connectionFactory.createContext(userName, password);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JMSContext createContext(String userName, String password, int sessionMode) {
        return connectionFactory.createContext(userName, password, sessionMode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueueConnection createQueueConnection() throws JMSException {
        return ((QueueConnectionFactory) (this.connectionFactory)).createQueueConnection();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueueConnection createQueueConnection(String userName, String password) throws JMSException {
        return ((QueueConnectionFactory) (this.connectionFactory)).createQueueConnection(userName, password);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TopicConnection createTopicConnection() throws JMSException {
        return ((TopicConnectionFactory) (this.connectionFactory)).createTopicConnection();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TopicConnection createTopicConnection(String userName, String password) throws JMSException {
        return ((TopicConnectionFactory) (this.connectionFactory)).createTopicConnection(userName, password);

    }

    /**
     * To get the destination of the particular session.
     *
     * @param session JMS session that we need to find the destination
     * @return destination the particular is related with
     * @throws JMSConnectorException Thrown when looking up destination
     */
    public Destination getDestination(Session session) throws JMSConnectorException {
        if (null != this.destination) {
            return this.destination;
        }
        return createDestination(session);
    }

    /**
     * Get a message consumer for particular session and destination.
     *
     * @param session     JMS Session to create the consumer
     * @param destination JMS destination which the consumer should listen to
     * @return Message Consumer, who is listening in particular destination with the given session
     * @throws JMSConnectorException Thrown when creating jms message consumer.
     */
    public MessageConsumer getMessageConsumer(Session session, Destination destination) throws JMSConnectorException {
        return createMessageConsumer(session, destination);
    }

    /**
     * Create a message consumer for particular session and destination.
     *
     * @param session     JMS Session to create the consumer
     * @param destination JMS destination which the consumer should listen to
     * @return Message Consumer, who is listening in particular destination with the given session
     * @throws JMSConnectorException Thrown when creating jms message consumer
     */
    public MessageConsumer createMessageConsumer(Session session, Destination destination)
            throws JMSConnectorException {
        try {
            if (JMSConstants.JMS_SPEC_VERSION_2_0.equals(jmsSpec) && isSharedSubscription) {
                if (isDurable) {
                    return session.createSharedDurableConsumer((Topic) destination, subscriptionName, messageSelector);
                } else {
                    return session.createSharedConsumer((Topic) destination, subscriptionName, messageSelector);
                }
            } else if ((JMSConstants.JMS_SPEC_VERSION_1_1.equals(jmsSpec)) || (
                    JMSConstants.JMS_SPEC_VERSION_2_0.equals(jmsSpec) && !isSharedSubscription)) {
                if (isDurable) {
                    return session.createDurableSubscriber((Topic) destination, subscriptionName, messageSelector,
                            noPubSubLocal);
                } else {
                    return session.createConsumer(destination, messageSelector);
                }
            } else {
                if (JMSConstants.JMSDestinationType.QUEUE.equals(this.destinationType)) {
                    return ((QueueSession) session).createReceiver((Queue) destination, messageSelector);
                } else {
                    if (isDurable) {
                        return session
                                .createDurableSubscriber((Topic) destination, subscriptionName, messageSelector,
                                        noPubSubLocal);
                    } else {
                        return ((TopicSession) session).createSubscriber((Topic) destination, messageSelector, false);
                    }
                }
            }
        } catch (Exception e) {
            throw new JMSConnectorException(
                    "JMS Exception while creating consumer for the destination " + destinationName, e);
        }
    }

    /**
     * Get a message producer for particular session and destination.
     *
     * @param session     JMS Session to create the producer
     * @param destination JMS destination which the producer should publish to
     * @return MessageProducer, who publish messages to particular destination with the given session
     * @throws JMSConnectorException Thrown when creating jms message producer
     */
    public MessageProducer getMessageProducer(Session session, Destination destination) throws JMSConnectorException {
        return createMessageProducer(session, destination);
    }

    /**
     * Create a message producer for particular session and destination.
     *
     * @param session     JMS Session to create the producer
     * @param destination JMS destination which the producer should publish to
     * @return MessageProducer, who publish messages to particular destination with the given session
     * @throws JMSConnectorException Thrown when creating jms message producer
     */
    public MessageProducer createMessageProducer(Session session, Destination destination)
            throws JMSConnectorException {
        try {
            if ((JMSConstants.JMS_SPEC_VERSION_1_1.equals(jmsSpec)) || (JMSConstants.JMS_SPEC_VERSION_2_0
                    .equals(jmsSpec))) {
                return session.createProducer(destination);
            } else {
                if (JMSConstants.JMSDestinationType.QUEUE.equals(this.destinationType)) {
                    return ((QueueSession) session).createSender((Queue) destination);
                } else {
                    return ((TopicSession) session).createPublisher((Topic) destination);
                }
            }
        } catch (JMSException e) {
            throw new JMSConnectorException(
                    "JMS Exception while creating the producer for the destination " + destinationName, e);
        }
    }

    /**
     * To create a destination for particular session.
     *
     * @param session Specific session to create the destination
     * @return destination for particular session
     * @throws JMSConnectorException Thrown when looking up destination
     */
    private Destination createDestination(Session session) throws JMSConnectorException {
        this.destination = createDestination(session, this.destinationName);
        return this.destination;
    }

    /**
     * To create the destination.
     *
     * @param session         relevant session to create the destination
     * @param destinationName Destination jms destination
     * @return the destination that is created from session
     * @throws JMSConnectorException Thrown when looking up destination
     */
    private Destination createDestination(Session session, String destinationName) throws JMSConnectorException {
        Destination destination = null;
        try {
            if (JMSConstants.JMSDestinationType.QUEUE.equals(this.destinationType)) {
                destination = JMSUtils.lookupDestination(ctx, destinationName, JMSConstants.DESTINATION_TYPE_QUEUE);
            } else if (JMSConstants.JMSDestinationType.TOPIC.equals(this.destinationType)) {
                destination = JMSUtils.lookupDestination(ctx, destinationName, JMSConstants.DESTINATION_TYPE_TOPIC);
            }
        } catch (NameNotFoundException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Could not find destination '" + destinationName + "' on connection factory for '"
                        + this.connectionFactoryString + "'. " + e.getMessage());
                logger.debug("Creating destination '" + destinationName + "' on connection factory for '"
                        + this.connectionFactoryString + ".");
            }
            /*
              If the destination is not found already, create the destination
             */
            try {
                if (JMSConstants.JMSDestinationType.QUEUE.equals(this.destinationType)) {
                    destination = session.createQueue(destinationName);
                } else if (JMSConstants.JMSDestinationType.TOPIC.equals(this.destinationType)) {
                    destination = session.createTopic(destinationName);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Created '" + destinationName + "' on connection factory for '"
                            + this.connectionFactoryString + "'.");
                }
            } catch (JMSException e1) {
                throw new JMSConnectorException(
                        "Could not find nor create '" + destinationName + "' on connection factory for "
                                + this.connectionFactoryString, e1);
            }
        } catch (NamingException e) {
            throw new JMSConnectorException(
                    "Naming exception while looking up for the destination name " + destinationName, e);
        }
        return destination;
    }

    /**
     * To get a session with the given connection.
     *
     * @param connection Connection that is needed to create the session
     * @return Session that is created from the connection
     * @throws JMSConnectorException Thrown when creating jms session
     */
    public Session getSession(Connection connection) throws JMSConnectorException {
        return createSession(connection);
    }

    /**
     * To create a session from the given connection.
     *
     * @param connection Specific connection which we is needed for creating session
     * @return session created from the given connection
     * @throws JMSConnectorException Thrown when creating jms session
     */
    public Session createSession(Connection connection) throws JMSConnectorException {
        try {
            if (JMSConstants.JMS_SPEC_VERSION_1_1.equals(jmsSpec) || JMSConstants.JMS_SPEC_VERSION_2_0
                    .equals(jmsSpec)) {
                return connection.createSession(transactedSession, sessionAckMode);
            } else if (JMSConstants.JMSDestinationType.QUEUE.equals(this.destinationType)) {
                return ((QueueConnection) (connection))
                        .createQueueSession(transactedSession, sessionAckMode);
            } else {
                return ((TopicConnection) (connection))
                        .createTopicSession(transactedSession, sessionAckMode);

            }
        } catch (JMSException e) {
            throw new JMSConnectorException(
                    "JMS Exception while obtaining session for factory " + connectionFactoryString, e);
        }
    }

    /**
     * Start the jms connection to start the message delivery.
     *
     * @param connection Connection that need to be started
     * @throws JMSConnectorException Thrown when starting jms connection
     */
    public void start(Connection connection) throws JMSConnectorException {
        try {
            connection.start();
        } catch (JMSException e) {
            throw new JMSConnectorException(
                    "JMS Exception while starting connection for factory " + this.connectionFactoryString, e);
        }
    }

    /**
     * Stop the jms connection to stop the message delivery.
     *
     * @param connection JMS connection that need to be stopped
     * @throws JMSConnectorException Thrown when stopping jms connection
     */
    public void stop(Connection connection) throws JMSConnectorException {
        try {
            if (null != connection) {
                connection.stop();
            }
        } catch (JMSException e) {
            throw new JMSConnectorException(
                    "JMS Exception while stopping the connection for factory " + this.connectionFactoryString, e);
        }
    }

    /**
     * Close the jms connection.
     *
     * @param connection JMS connection that need to be closed
     * @throws JMSConnectorException Thrown when closing jms connection
     */
    public void closeConnection(Connection connection) throws JMSConnectorException {
        try {
            if (null != connection) {
                connection.close();
            }
        } catch (JMSException e) {
            throw new JMSConnectorException("JMS Exception while closing the connection. ", e);
        }
    }

    /**
     * To close the session.
     *
     * @param session JMS session that need to be closed
     * @throws JMSConnectorException Thrown when closing jms session
     */
    public void closeSession(Session session) throws JMSConnectorException {
        try {
            if (null != session) {
                session.close();
            }
        } catch (JMSException e) {
            throw new JMSConnectorException("JMS Exception while closing the session. ", e);
        }
    }

    /**
     * To close the message consumer.
     *
     * @param messageConsumer Message consumer that need to be closed
     * @throws JMSConnectorException Thrown when closing jms message consumer
     */
    public void closeMessageConsumer(MessageConsumer messageConsumer) throws JMSConnectorException {
        try {
            if (null != messageConsumer) {
                messageConsumer.close();
            }
        } catch (JMSException e) {
            throw new JMSConnectorException("JMS Exception while closing the subscriber. ", e);
        }
    }

    /**
     * To close the message producer.
     *
     * @param messageProducer Message producer that need to be closed
     * @throws JMSConnectorException Thrown when closing jms message producer
     */
    public void closeMessageProducer(MessageProducer messageProducer) throws JMSConnectorException {
        try {
            if (messageProducer != null) {
                messageProducer.close();
            }
        } catch (JMSException e) {
            throw new JMSConnectorException("JMS Exception while closing the producer. ", e);
        }
    }
}
