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

package org.wso2.carbon.transport.jms.clientfactory;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.jms.clientfactory.sessionpool.SessionPoolFactory;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.factory.JMSConnectionResourceFactory;
import org.wso2.carbon.transport.jms.utils.JMSConstants;
import org.wso2.carbon.transport.jms.wrappers.ConnectionWrapper;
import org.wso2.carbon.transport.jms.wrappers.SessionWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

/**
 * Extended class to handle Client side Connection Factory requirements
 */
public class JMSClientConnectionFactory extends JMSConnectionResourceFactory {

    private static final Logger logger = LoggerFactory.getLogger(JMSClientConnectionFactory.class);
    /**
     * Size of the Connection list
     */
    private static final int maxNumberOfConnections = 2;

    /**
     * Number of session per Connection
     */
    private static final int maxSessionsPerConnection = 5;

    /**
     * Wait timeout for the Session pool
     */
    private static final int poolWaitTimeout = 30 * 1000;

    /**
     * Indicates cache level given by the user.
     */
    private boolean isClientCaching = true;

    /**
     * List of Connection wrapper objects
     */
    private List<ConnectionWrapper> connections = null;

    /**
     * Pool of Session wrapper objects
     */
    private GenericObjectPool<SessionWrapper> sessionPool = null;

    /**
     * Constructor
     *
     * @param properties JMS properties
     * @throws JMSConnectorException
     */
    public JMSClientConnectionFactory(Properties properties) throws JMSConnectorException {
        super(properties);
        setCache(properties);
        connections = new ArrayList<>();
        initSessionPool();
    }

    /**
     * Initialize the session pool with provided configuration
     */
    private void initSessionPool() {
        //todo: make parameters configurable
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

    private void setCache(Properties properties) {
        String cacheLevel = properties.getProperty(JMSConstants.PARAM_JMS_CACHING);
        if (null != cacheLevel && !cacheLevel.isEmpty()) {
            this.isClientCaching = Boolean.parseBoolean(cacheLevel);
        } else {
            this.isClientCaching = Boolean.TRUE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void notifyError() {
        closeJMSResources();
        initSessionPool();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionFactory getConnectionFactory() throws JMSConnectorException {
        return super.getConnectionFactory();
    }

    /**
     *
     * @return Connections list
     */
    public List<ConnectionWrapper> getConnections() {
        return connections;
    }

    /**
     * Borrow an object from the session pool
     * @return SessionWrapper instance
     * @throws Exception
     */
    public SessionWrapper getSessionWrapper() throws Exception {
        //        Systemm.out.println("Session pool | get session wrapper object");
        return sessionPool.borrowObject();
    }

    /**
     * Return an object to the Session pool
     * @param sessionWrapper
     */
    public void returnSessionWrapper(SessionWrapper sessionWrapper) {
        //Systemm.out.println("Session pool | return session wrapper object");
        sessionPool.returnObject(sessionWrapper);
    }

    public static int getMaxNumberOfConnections() {
        return maxNumberOfConnections;
    }

    public static int getMaxSessionsPerConnection() {
        return maxSessionsPerConnection;
    }

    /**
     * Close the JMS resources allocated for this Connection Factory
     */
    private void closeJMSResources() {
        sessionPool.clear();
        connections.forEach(connection -> {
            try {
                connection.getConnection().close();
                logger.info("Closing connections");
            } catch (JMSException e) {
                logger.error("Error closing the connection" + e);
            }
        });
        connections.clear();
    }
}
