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
import org.wso2.carbon.transport.jms.factory.JMSImprovedConnectionFactory;
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
public class ExtendedJMSClientConnectionFactory extends JMSImprovedConnectionFactory {

    /**
     * Parameter for passing in cache level.
     */
    public static final String PARAM_JMS_CACHING = "transport.jms.caching";
    private static final Logger logger = LoggerFactory.getLogger(ExtendedJMSClientConnectionFactory.class);
    private static final int maxNumberOfConnections = 2;
    private static final int maxSessionsPerConnection = 5;
    /**
     * Indicates cache level given by the user.
     */
    private boolean isClientCaching = true;

    private List<ConnectionWrapper> connections = null;

    private GenericObjectPool<SessionWrapper> sessionPool = null;

    public ExtendedJMSClientConnectionFactory(Properties properties) throws JMSConnectorException {
        super(properties);
        setValues(properties);
        connections = new ArrayList<>();
        initSessionPool();
    }

    public static int getMaxNumberOfConnections() {
        return maxNumberOfConnections;
    }

    public static int getMaxSessionsPerConnection() {
        return maxSessionsPerConnection;
    }

    private void initSessionPool() {
        //todo: make parameters configurable
        SessionPoolFactory sessionPoolFactory = new SessionPoolFactory(this);
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(maxNumberOfConnections * maxSessionsPerConnection);
        //todo: set the ideal limit and make the idle sessions timedout
        config.setMaxIdle(maxNumberOfConnections * maxSessionsPerConnection);
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

    public List<ConnectionWrapper> getConnections() {
        return connections;
    }

    public SessionWrapper getSessionWrapper() throws Exception {
        //        Systemm.out.println("Session pool | get session wrapper object");
        return sessionPool.borrowObject();
    }

    public void returnSessionWrapper(SessionWrapper sessionWrapper) {
        //Systemm.out.println("Session pool | return session wrapper object");
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
