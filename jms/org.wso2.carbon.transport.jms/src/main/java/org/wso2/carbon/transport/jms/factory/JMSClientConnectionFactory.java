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

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.sender.sessionpool.SessionPoolFactory;
import org.wso2.carbon.transport.jms.sender.wrappers.ConnectionWrapper;
import org.wso2.carbon.transport.jms.sender.wrappers.SessionWrapper;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.XAConnection;

/**
 * Extended class to handle Client side Connection Factory requirements.
 */
public class JMSClientConnectionFactory extends JMSConnectionResourceFactory {

    private static final Logger logger = LoggerFactory.getLogger(JMSClientConnectionFactory.class);
    /**
     * Default Wait timeout (in milliseconds) for the Session pool.
     */
    private static final int poolWaitTimeout = 30 * 1000;
    /**
     * Default Size of the Connection list.
     */
    private int maxNumberOfConnections = 5;
    /**
     * Default Number of session per Connection.
     */
    private int maxSessionsPerConnection = 10;
    /**
     * Indicates cache level given by the user.
     */
    private boolean clientCaching = true;

    /**
     * List of Connection wrapper objects.
     */
    private List<ConnectionWrapper> connections = null;

    /**
     * Pool of Session wrapper objects.
     */
    private GenericObjectPool<SessionWrapper> sessionPool = null;

    /**
     * Constructor.
     *
     * @param properties JMS properties.
     * @throws JMSConnectorException if an error thrown from parents constructor.
     */
    public JMSClientConnectionFactory(Properties properties, boolean isCached) throws JMSConnectorException {
        super(properties);
        this.clientCaching = isCached;

        // session pool configurations
        if (properties.getProperty(JMSConstants.PARAM_MAX_CONNECTIONS) != null) {
            try {
                this.maxNumberOfConnections = Integer
                        .parseInt(properties.getProperty(JMSConstants.PARAM_MAX_CONNECTIONS));
            } catch (NumberFormatException ex) {
                logger.error("Non-integer value configured for JMS Client Connection count", ex);
            }
        }
        if (properties.getProperty(JMSConstants.PARAM_MAX_SESSIONS_ON_CONNECTION) != null) {
            try {
                this.maxSessionsPerConnection = Integer
                        .parseInt(properties.getProperty(JMSConstants.PARAM_MAX_SESSIONS_ON_CONNECTION));
            } catch (NumberFormatException ex) {
                logger.error("Non-integer value configured for JMS Client Sessions count", ex);
            }
        }

        if (clientCaching) {
            connections = new ArrayList<>();
            initSessionPool();
        }
    }

    public int getMaxNumberOfConnections() {
        return maxNumberOfConnections;
    }

    public int getMaxSessionsPerConnection() {
        return maxSessionsPerConnection;
    }

    /**
     * Initialize the session pool with provided configuration.
     */
    private void initSessionPool() {
        SessionPoolFactory sessionPoolFactory = new SessionPoolFactory(this);

        //create pool configurations
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(maxNumberOfConnections * maxSessionsPerConnection);
        //todo: set the ideal limit and make the idle sessions timedout
        config.setMaxIdle(maxNumberOfConnections * maxSessionsPerConnection);
        config.setBlockWhenExhausted(true);
        config.setMaxWaitMillis(poolWaitTimeout);

        //initialize the pool
        sessionPool = new GenericObjectPool<SessionWrapper>(sessionPoolFactory, config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void notifyError(JMSException ex) {
        logger.error("Error occurred in JMS Client Connections. Re-initializing the resources. ", ex);

        try {
            closeJMSResources();
        } catch (JMSConnectorException e) {
            // If an exception was triggered due to a connection issue, closing connection will also fail with a
            // connection exception
            logger.error("Error closing connection after exception", e);
        } finally {
            connections.clear();
        }

        initSessionPool();
    }

    /**
     * @return Connections list.
     */
    public List<ConnectionWrapper> getConnections() {
        return connections;
    }

    /**
     * Borrow an object from the session pool.
     *
     * @return SessionWrapper instance.
     * @throws Exception Exception when borrowing an object from the pool.
     */
    public SessionWrapper getSessionWrapper() throws Exception {
        return sessionPool.borrowObject();
    }

    /**
     * Return an object to the Session pool.
     *
     * @param sessionWrapper SessionWrapper instance.
     */
    public void returnSessionWrapper(SessionWrapper sessionWrapper) {
        sessionPool.returnObject(sessionWrapper);
    }

    /**
     * Close cached JMS resources allocated for this Connection Factory.
     */
    public void closeJMSResources() throws JMSConnectorException {
        if (clientCaching) {
            sessionPool.clear();
            sessionPool.close();
            for (int i = 0; i < connections.size(); i++) {
                try {
                    closeConnection(connections.get(i).getConnection());
                } catch (JMSException e) {
                    throw new JMSConnectorException("Error closing the connection.", e);
                }
            }
        }
    }

    /**
     * Is this Client Connection factory is configured to use caching/pooling.
     *
     * @return isClientCaching set.
     */
    public boolean isClientCaching() {
        return clientCaching;
    }

    @Override
    public Connection createConnection() throws JMSException {
        Connection connection = super.createConnection();
        connection.setExceptionListener(new JMSErrorListener());
        return connection;
    }

    @Override
    public XAConnection createXAConnection() throws JMSException {
        XAConnection xaConnection = super.createXAConnection();
        xaConnection.setExceptionListener(new JMSErrorListener());
        return xaConnection;
    }

    /**
     * JMS Client Connection Error Listener class that implements {@link ExceptionListener} from JMS API.
     */
    private class JMSErrorListener implements ExceptionListener {
        @Override
        public void onException(JMSException e) {
            notifyError(e);
        }
    }
}
