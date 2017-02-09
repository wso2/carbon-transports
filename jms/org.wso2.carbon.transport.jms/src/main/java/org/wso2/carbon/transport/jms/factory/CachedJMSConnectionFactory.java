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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

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
    private static final Log logger = LogFactory.getLog(CachedJMSConnectionFactory.class);

    /**
     * Indicates cache level given by the user.
     */
    private int cacheLevel = 0;

    /**
     * Holds the cached jms connection instance.
     */
    private Connection cachedConnection = null;

    /**
     * Holds the cached jms session instance.
     */
    private Session cachedSession = null;

    /**
     * Holds the cached jms message consumer instance.
     */
    private MessageConsumer cachedMessageConsumer = null;

    /**
     * Holds the cached jms message producer instance.
     */
    private MessageProducer cachedMessageProducer = null;

    /**
     * Constructor to create CachedJMSConnectionFactory instance.
     *
     * @param properties    Property values required to initialize the connection factory
     * @throws JMSConnectorException
     */
    public CachedJMSConnectionFactory(Properties properties) throws JMSConnectorException {
        super(properties);
        setValues(properties);
    }

    private void setValues(Properties properties) {
        String cacheLevel = properties.getProperty(JMSConstants.PARAM_CACHE_LEVEL);
        if (null != cacheLevel && !cacheLevel.isEmpty()) {
            this.cacheLevel = Integer.parseInt(cacheLevel);
        } else {
            this.cacheLevel = JMSConstants.CACHE_NONE;
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
    public Connection getConnection() throws JMSException {
        return getConnection(null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection getConnection(String userName, String password) throws JMSException {
        Connection connection;
        if (cachedConnection == null) {
            connection = createConnection(userName, password);
        } else {
            connection = cachedConnection;
        }
        try {
            connection.start();
        } catch (JMSException e) {
            if (cachedConnection != null) {
                resetCache();
                getConnection(userName, password);
            } else {
                logger.error(
                        "JMS Exception while starting connection for factory '" + this.connectionFactoryString + "' " +
                        e.getMessage());
                throw e;
            }

        }
        return connection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection createConnection(String userName, String password) throws JMSException {
        Connection connection;
        if (userName == null || password == null) {
            connection = super.createConnection();
        } else {
            connection = super.createConnection(userName, password);
        }
        if (this.cacheLevel >= JMSConstants.CACHE_CONNECTION) {
            cachedConnection = connection;
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
        Session session = super.createSession(connection);
        if (this.cacheLevel >= JMSConstants.CACHE_SESSION) {
            cachedSession = session;
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
        MessageConsumer messageConsumer = super.createMessageConsumer(session, destination);
        if (this.cacheLevel >= JMSConstants.CACHE_CONSUMER) {
            cachedMessageConsumer = messageConsumer;
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
        if (cachedMessageConsumer == null) {
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
        MessageProducer messageProducer = super.createMessageProducer(session, destination);
        if (this.cacheLevel >= JMSConstants.CACHE_PRODUCER) {
            cachedMessageProducer = messageProducer;
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
            if (this.cacheLevel < JMSConstants.CACHE_CONNECTION) {
                connection.close();
            }
        } catch (JMSException e) {
            throw new JMSConnectorException("JMS Exception while closing the connection.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeMessageConsumer(MessageConsumer messageConsumer) throws JMSConnectorException {
        try {
            if (this.cacheLevel < JMSConstants.CACHE_CONSUMER) {
                messageConsumer.close();
            }
        } catch (JMSException e) {
            throw new JMSConnectorException("JMS Exception while closing the consumer.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeMessageProducer(MessageProducer messageProducer) throws JMSConnectorException {
        try {
            if (this.cacheLevel < JMSConstants.CACHE_PRODUCER) {
                messageProducer.close();
            }
        } catch (JMSException e) {
            throw new JMSConnectorException("JMS Exception while closing the producer.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeSession(Session session) throws JMSConnectorException {
        try {
            if (this.cacheLevel < JMSConstants.CACHE_SESSION) {
                session.close();
            }
        } catch (JMSException e) {
            throw new JMSConnectorException("JMS Exception while closing the session.", e);
        }
    }

    private void resetCache() throws JMSException {
        if (cachedMessageConsumer != null) {
            try {
                cachedMessageConsumer.close();
            } catch (JMSException e) {
                throw e;
            }
            cachedMessageConsumer = null;
        }
        if (cachedMessageProducer != null) {
            try {
                cachedMessageProducer.close();
            } catch (JMSException e) {
                throw e;
            }
            cachedMessageProducer = null;
        }
        if (cachedSession != null) {
            try {
                cachedSession.close();
            } catch (JMSException e) {
                throw e;
            }
            cachedSession = null;
        }
        if (cachedConnection != null) {
            try {
                cachedConnection.close();
            } catch (JMSException e) {
                throw e;
            }
            cachedConnection = null;
        }
    }

}
