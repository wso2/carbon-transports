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

import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * {@link JMSConnectionFactory} connector, session, consumer, producer caching implementation. This class can be used
 * if needed to cache connections.
 */
public class CachedJMSConnectionFactory extends JMSConnectionFactory {

    /**
     * Parameter for passing in cache level.
     */
    public static final String PARAM_CACHE_LEVEL = "transport.jms.CacheLevel";

    /**
     * Do not cache any JMS resources between tasks (when sending) or JMS CF's
     * (when sending)
     */
    public static final int CACHE_NONE = 0;
    /**
     * Cache only the JMS connection between tasks (when receiving), or JMS CF's
     * (when sending)
     */
    public static final int CACHE_CONNECTION = 1;
    /**
     * Cache only the JMS connection and Session between tasks (receiving), or
     * JMS CF's (sending)
     */
    public static final int CACHE_SESSION = 2;
    /**
     * Cache the JMS connection, Session and Consumer between tasks when
     * receiving
     */
    public static final int CACHE_CONSUMER = 3;
    /**
     * Cache the JMS connection, Session and Producer within a
     * JMSConnectionFactory when sending
     */
    public static final int CACHE_PRODUCER = 4;

    /**
     * Indicates cache level given by the user.
     */
    private int cacheLevel = 0;

    /**
     * {@link Connection} instance representing cached jms connection instance.
     */
    private Connection cachedConnection = null;

    /**
     * {@link Session} instance representing cached jms session instance.
     */
    private Session cachedSession = null;

    /**
     * {@link MessageConsumer} instance representing cached jms message consumer instance.
     */
    private MessageConsumer cachedMessageConsumer = null;

    /**
     * {@link MessageProducer} instance representing cached jms message producer instance.
     */
    private MessageProducer cachedMessageProducer = null;

    /**
     * Constructor to create CachedJMSConnectionFactory instance.
     *
     * @param properties    Property values required to initialize the connection factory
     * @throws JMSConnectorException Thrown when initial context factory is not found.
     */
    public CachedJMSConnectionFactory(Properties properties) throws JMSConnectorException {
        super(properties);
        setValues(properties);
    }

    private void setValues(Properties properties) {
        String cacheLevel = properties.getProperty(PARAM_CACHE_LEVEL);
        if (null != cacheLevel && !cacheLevel.isEmpty()) {
            this.cacheLevel = Integer.parseInt(cacheLevel);
        } else {
            this.cacheLevel = CACHE_NONE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionFactory getConnectionFactory() throws JMSConnectorException {
        return super.getConnectionFactory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection createConnection() throws JMSException {
        Connection connection;

        if (cacheLevel >= CACHE_CONNECTION) {

            if (cachedConnection == null) {
                connection = super.createConnection();
                cachedConnection = connection;
            } else {
                connection = cachedConnection;
            }
        } else {
            connection = super.createConnection();
        }

        return connection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection createConnection(String userName, String password) throws JMSException {
        Connection connection;
        if (userName == null && password == null) {
            connection = createConnection();
        } else {
            if (cachedConnection == null) {
                connection = super.createConnection(userName, password);
                cachedConnection = connection;
            } else {
                connection = cachedConnection;
            }

            try {
                connection.start();
            } catch (JMSException e) {
                if (cachedConnection != null) {
                    resetCache();
                    connection = createConnection(userName, password);
                } else {
                    throw e;
                }

            }
        }

        return connection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Session getSession(Connection connection) throws JMSConnectorException {
        if (cachedSession == null) {
            return createSession(connection);
        } else {
            return cachedSession;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Session createSession(Connection connection) throws JMSConnectorException {
        Session session;

        if (cacheLevel >= CACHE_SESSION) {

            if (cachedSession == null) {
                session = super.createSession(connection);
                cachedSession = session;
            } else {
                session = cachedSession;
            }

        } else {
            session = super.createSession(connection);
        }
        return session;
    }

    @Override
    public MessageConsumer getMessageConsumer(Session session, Destination destination)
            throws JMSConnectorException {
        MessageConsumer messageConsumer;
        if (cachedMessageConsumer == null) {
            messageConsumer = createMessageConsumer(session, destination);
        } else {
            messageConsumer = cachedMessageConsumer;
        }
        return messageConsumer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageConsumer createMessageConsumer(Session session, Destination destination)
            throws JMSConnectorException {

        MessageConsumer messageConsumer;

        if (cacheLevel >= CACHE_CONSUMER) {

            if (cachedMessageConsumer == null) {
                messageConsumer = super.createMessageConsumer(session, destination);
                cachedMessageConsumer = messageConsumer;
            } else {
                messageConsumer = cachedMessageConsumer;
            }
        } else {
            messageConsumer = super.createMessageConsumer(session, destination);
        }

        return messageConsumer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageProducer getMessageProducer(Session session, Destination destination)
            throws JMSConnectorException {
        MessageProducer messageProducer;
        if (cachedMessageProducer == null) {
            messageProducer = createMessageProducer(session, destination);
        } else {
            messageProducer = cachedMessageProducer;
        }
        return messageProducer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageProducer createMessageProducer(Session session, Destination destination)
            throws JMSConnectorException {
        MessageProducer messageProducer;
        if (this.cacheLevel >= CACHE_PRODUCER) {
            if (cachedMessageProducer == null) {
                messageProducer = super.createMessageProducer(session, destination);
                cachedMessageProducer = messageProducer;
            } else {
                messageProducer = cachedMessageProducer;
            }
        } else {
            messageProducer = super.createMessageProducer(session, destination);
        }

        return messageProducer;
    }

    public void closeConnection() throws JMSException {
        if (cachedConnection != null) {
            cachedConnection.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeConnection(Connection connection) throws JMSConnectorException {
        try {
            connection.close();
        } catch (JMSException e) {
            throw new JMSConnectorException("JMS Exception while closing the connection.", e);
        } finally {
            if (cacheLevel >= CACHE_CONNECTION && cachedConnection != null &&
                    cachedConnection.equals(connection)) {
                cachedConnection = null;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeMessageConsumer(MessageConsumer messageConsumer) throws JMSConnectorException {
        try {
            messageConsumer.close();
        } catch (JMSException e) {
            throw new JMSConnectorException("JMS Exception while closing the consumer.", e);
        } finally {
            if (cacheLevel >= CACHE_CONSUMER && cachedMessageConsumer != null &&
                    cachedMessageConsumer.equals(messageConsumer)) {
                cachedMessageConsumer = null;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeMessageProducer(MessageProducer messageProducer) throws JMSConnectorException {
        try {
            messageProducer.close();
        } catch (JMSException e) {
            throw new JMSConnectorException("JMS Exception while closing the producer.", e);
        } finally {
            if (cacheLevel >= CACHE_PRODUCER && cachedMessageProducer != null &&
                    cachedMessageProducer.equals(messageProducer)) {
                cachedMessageProducer = null;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeSession(Session session) throws JMSConnectorException {
        try {
            session.close();
        } catch (JMSException e) {
            throw new JMSConnectorException("JMS Exception while closing the session.", e);
        } finally {
            if (cacheLevel >= CACHE_SESSION && cachedSession != null && cachedSession.equals(session)) {
                cachedSession = null;
            }
        }
    }

    /**
     * Rests all elements in cache.
     *
     * @throws JMSException Thrown when closing jms connection, session, consumer and producer
     */
    private void resetCache() throws JMSException {
        if (cachedMessageConsumer != null) {
            cachedMessageConsumer.close();
            cachedMessageConsumer = null;
        }
        if (cachedMessageProducer != null) {
            cachedMessageProducer.close();
            cachedMessageProducer = null;
        }
        if (cachedSession != null) {
            cachedSession.close();
            cachedSession = null;
        }
        if (cachedConnection != null) {
            cachedConnection.close();
            cachedConnection = null;
        }
    }

}
