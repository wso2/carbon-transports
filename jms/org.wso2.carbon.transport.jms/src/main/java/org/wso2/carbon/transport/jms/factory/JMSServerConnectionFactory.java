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
import org.wso2.carbon.kernel.utils.StringUtils;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

import java.util.Properties;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSession;

/**
 * JMSServerConnectionFactory that handles the JMS Connection, Session creation and closing.
 */
public class JMSServerConnectionFactory extends JMSConnectionResourceFactory {
    private static final Logger logger = LoggerFactory.getLogger(JMSServerConnectionFactory.class);

    /**
     * The {@link String} instance representing the jms destination name.
     */
    private String destinationName;
    /**
     * The {@link Destination} instance representing the jms destination listening to.
     */
    private Destination destination;
    /**
     * The {@link Boolean} instance representing whether subscription is durable or not.
     */
    private boolean isDurable;
    /**
     * The {@link Boolean} instance representing whether to create a pub-sub connection.
     */
    private boolean noPubSubLocal;
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
     * @param properties Properties to be added to the initial context.
     * @throws JMSConnectorException Thrown when initial context name is wrong or when creating connection factory.
     */
    public JMSServerConnectionFactory(Properties properties) throws JMSConnectorException {
        super(properties);

        isSharedSubscription = Boolean.parseBoolean(properties.getProperty(JMSConstants.PARAM_IS_SHARED_SUBSCRIPTION));

        noPubSubLocal = Boolean.valueOf(properties.getProperty(JMSConstants.PARAM_PUBSUB_NO_LOCAL));

        subscriptionName = properties.getProperty(JMSConstants.PARAM_DURABLE_SUB_ID);

        if (isSharedSubscription && subscriptionName == null) {
            logger.warn("Subscription name is not given. Therefore declaring a non-shared subscription");
            isSharedSubscription = false;
        }

        isDurable = !StringUtils.isNullOrEmptyAfterTrim(properties.getProperty(JMSConstants.PARAM_DURABLE_SUB_ID));

        String msgSelector = properties.getProperty(JMSConstants.PARAM_MSG_SELECTOR);
        if (null != msgSelector) {
            messageSelector = msgSelector;
        }

        this.destinationName = properties.getProperty(JMSConstants.PARAM_DESTINATION_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection createConnection() throws JMSException {
        Connection connection = null;
        try {
            connection = super.createConnection();
            if (isDurable && !isSharedSubscription) {
                connection.setClientID(clientId);
            }
            return connection;
        } catch (JMSException e) {
            // Need to close the Connection in the case if durable subscriptions
            if (null != connection) {
                try {
                    connection.close();
                } catch (JMSException ex) {
                    logger.error("Error while closing the connection. ", ex);
                }
            }
            throw e;
        }
    }

    /**
     * Create a message consumer for particular session and destination.
     *
     * @param session     JMS Session to create the consumer.
     * @param destination JMS destination which the consumer should listen to.
     * @return Message Consumer, who is listening in particular destination with the given session.
     * @throws JMSConnectorException Thrown when creating jms message consumer.
     */
    public MessageConsumer createMessageConsumer(Session session, Destination destination)
            throws JMSConnectorException {
        if (logger.isDebugEnabled()) {
            logger.debug("Creating a JMS Message Consumer on: " + getConnectionFactoryString());
        }
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
                if (JMSConstants.JMSDestinationType.QUEUE.equals(destinationType)) {
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
     * To get the destination of the particular session.
     *
     * @param session JMS session that we need to find the destination.
     * @return destination the particular is related with.
     * @throws JMSConnectorException Thrown when looking up destination.
     */
    public Destination getDestination(Session session) throws JMSConnectorException {
        if (null != this.destination) {
            return this.destination;
        }
        return createDestination(session, destinationName);
    }
}
