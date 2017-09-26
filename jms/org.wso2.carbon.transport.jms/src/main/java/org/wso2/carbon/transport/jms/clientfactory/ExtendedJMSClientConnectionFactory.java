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
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.factory.JMSImprovedConnectionFactory;
import org.wso2.carbon.transport.jms.clientfactory.sessionpool.SessionPoolFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

/**
 * {@link JMSImprovedConnectionFactory} connector, session, consumer, producer caching implementation. This class can be used
 * if needed to cache connections.
 */
public class ExtendedJMSClientConnectionFactory extends JMSImprovedConnectionFactory {

    private static final Logger logger = LoggerFactory.getLogger(ExtendedJMSClientConnectionFactory.class);

    /**
     * Parameter for passing in cache level.
     */
    public static final String PARAM_JMS_CACHING = "transport.jms.caching";

    /**
     * Indicates cache level given by the user.
     */
    private boolean isClientCaching = true;

    /**
     * {@link Connection} instance representing cached jms connection instance.
     */
    private List<ConnectionWrapper> connections = null;

    private static final int maxNumberOfConnections = 2;
    /**
     * {@link Session} instance representing cached jms session instance.
     */
    private GenericObjectPool<SessionWrapper> sessionPool = null;

    private static final int maxSessionsPerConnection = 5;

    public ExtendedJMSClientConnectionFactory(Properties properties) throws JMSConnectorException {
        super(properties);
        setValues(properties);
        connections = new ArrayList<>();
        initSessionPool();
    }

    private void initSessionPool() {
        //todo: make parameters configurable
        SessionPoolFactory sessionPoolFactory = new SessionPoolFactory(this);
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(maxNumberOfConnections*maxSessionsPerConnection);
        //todo: set the ideal limit and make the idle sessions timedout
        config.setMaxIdle(maxNumberOfConnections*maxSessionsPerConnection);
        config.setBlockWhenExhausted(true);
        config.setMaxWaitMillis(30 * 1000);
        sessionPool = new GenericObjectPool<SessionWrapper>(sessionPoolFactory, config);
    }

    private void setValues(Properties properties) {
        String cacheLevel = properties.getProperty(PARAM_JMS_CACHING);
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
    public ConnectionFactory getConnectionFactory() throws JMSConnectorException {
        return super.getConnectionFactory();
    }

    public static int getMaxNumberOfConnections (){
        return maxNumberOfConnections;
    }

    public List<ConnectionWrapper> getConnections() {
        return connections;
    }

    public static int getMaxSessionsPerConnection() {
        return maxSessionsPerConnection;
    }

    public SessionWrapper getSessionWrapper() throws Exception {
//        System.out.println("Session pool | get session wrapper object");
        return sessionPool.borrowObject();
    }

    public void returnSessionWrapper(SessionWrapper sessionWrapper) {
//        System.out.println("Session pool | return session wrapper object");
        sessionPool.returnObject(sessionWrapper);
    }

    public synchronized void notifyError() {
        closeJMSResources();
        initSessionPool();
    }

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
