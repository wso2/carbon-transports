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

    private int cacheLevel = 0;

    private Connection cachedConnection = null;
    private Session cachedSession = null;
    private MessageConsumer cachedMessageConsumer = null;
    private MessageProducer cachedMessageProducer = null;

    /**
     * Constructor to create CachedJMSConnectionFactory instance.
     *
     * @param properties Property values required to initialize the connection factory
     * @throws JMSConnectorException
     */
    public CachedJMSConnectionFactory(Properties properties) throws JMSConnectorException {
        super(properties);
        setValues(properties);
    }

    private void setValues(Properties properties) {
        String cacheLevel = properties.getProperty(JMSConstants.PARAM_CACHE_LEVEL);
        if (null != cacheLevel && !"".equals(cacheLevel)) {
            this.cacheLevel = Integer.parseInt(cacheLevel);
        } else {
            this.cacheLevel = JMSConstants.CACHE_NONE;
        }
    }

    @Override public ConnectionFactory getConnectionFactory() throws JMSConnectorException {
        return super.getConnectionFactory();
    }

    @Override public Connection getConnection() throws JMSException {
        return getConnection(null, null);
    }

    @Override public Connection getConnection(String userName, String password) throws JMSException {
        Connection connection;
        if (cachedConnection == null) {
            connection = createConnection(userName, password);
        } else {
            connection = cachedConnection;
        }
        if (connection == null) {
            return null;
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

    @Override public Connection createConnection(String userName, String password) throws JMSException {
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

    @Override public Session getSession(Connection connection) throws JMSConnectorException {
        if (cachedSession == null) {
            return createSession(connection);
        } else {
            return cachedSession;
        }
    }

    @Override public Session createSession(Connection connection) throws JMSConnectorException {
        Session session = super.createSession(connection);
        if (this.cacheLevel >= JMSConstants.CACHE_SESSION) {
            cachedSession = session;
        }
        return session;
    }

    @Override public MessageConsumer getMessageConsumer(Session session, Destination destination)
            throws JMSConnectorException {
        MessageConsumer messageConsumer = null;
        if (cachedMessageConsumer == null) {
            messageConsumer = createMessageConsumer(session, destination);
        } else {
            messageConsumer = cachedMessageConsumer;
        }
        return messageConsumer;
    }

    @Override public MessageConsumer createMessageConsumer(Session session, Destination destination)
            throws JMSConnectorException {
        MessageConsumer messageConsumer = super.createMessageConsumer(session, destination);
        if (this.cacheLevel >= JMSConstants.CACHE_CONSUMER) {
            cachedMessageConsumer = messageConsumer;
        }
        return messageConsumer;
    }

    @Override public MessageProducer getMessageProducer(Session session, Destination destination)
            throws JMSConnectorException {
        MessageProducer messageProducer = null;
        if (cachedMessageConsumer == null) {
            messageProducer = createMessageProducer(session, destination);
        } else {
            messageProducer = cachedMessageProducer;
        }
        return messageProducer;
    }

    @Override public MessageProducer createMessageProducer(Session session, Destination destination)
            throws JMSConnectorException {
        MessageProducer messageProducer = super.createMessageProducer(session, destination);
        if (this.cacheLevel >= JMSConstants.CACHE_PRODUCER) {
            cachedMessageProducer = messageProducer;
        }
        return messageProducer;
    }

    public boolean closeConnection() {
        try {
            if (cachedConnection != null) {
                cachedConnection.close();
            }
            return true;
        } catch (JMSException e) {
            logger.error("JMS Exception while closing the connection.");
        }
        return false;
    }

    @Override public void closeConnection(Connection connection) {
        closeConnection(connection, false);
    }

    @Override public void closeMessageConsumer(MessageConsumer messageConsumer) {
        closeConsumer(messageConsumer, false);
    }

    @Override public void closeMessageProducer(MessageProducer messageProducer) {
        closeProducer(messageProducer, false);
    }

    @Override public void closeSession(Session session) {
        closeSession(session, false);
    }

    private boolean closeConnection(Connection connection, boolean forcefully) {
        try {
            if (this.cacheLevel < JMSConstants.CACHE_CONNECTION || forcefully) {
                connection.close();
            }
        } catch (JMSException e) {
            logger.error("JMS Exception while closing the connection.");
        }
        return false;
    }

    private boolean closeConsumer(MessageConsumer messageConsumer, boolean forcefully) {
        try {
            if (this.cacheLevel < JMSConstants.CACHE_CONSUMER || forcefully) {
                messageConsumer.close();
            }
        } catch (JMSException e) {
            logger.error("JMS Exception while closing the consumer.");
        }
        return false;
    }

    private boolean closeProducer(MessageProducer messageProducer, boolean forcefully) {
        try {
            if (this.cacheLevel < JMSConstants.CACHE_PRODUCER || forcefully) {
                messageProducer.close();
            }
        } catch (JMSException e) {
            logger.error("JMS Exception while closing the producer.");
        }
        return false;
    }

    private boolean closeSession(Session session, boolean forcefully) {
        try {
            if (this.cacheLevel < JMSConstants.CACHE_SESSION || forcefully) {
                session.close();
            }
        } catch (JMSException e) {
            logger.error("JMS Exception while closing the consumer.");
        }
        return false;
    }

    private void resetCache() {
        if (cachedMessageConsumer != null) {
            try {
                cachedMessageConsumer.close();
            } catch (JMSException e) {
            }
            cachedMessageConsumer = null;
        }
        if (cachedMessageProducer != null) {
            try {
                cachedMessageProducer.close();
            } catch (JMSException e) {
            }
            cachedMessageProducer = null;
        }
        if (cachedSession != null) {
            try {
                cachedSession.close();
            } catch (JMSException e) {
            }
            cachedSession = null;
        }
        if (cachedConnection != null) {
            try {
                cachedConnection.close();
            } catch (JMSException e) {
            }
            cachedConnection = null;
        }
    }

}
