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
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.wso2.carbon.transport.jms.factory;

import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.TopicConnection;

/**
 * JMS Connection Factory which pools connections for better performance.
 */
public class PooledJMSConnectionFactory extends JMSConnectionFactory
        implements KeyedPooledObjectFactory<PooledConnectionKey, Connection> {

    private static final Logger log = LoggerFactory.getLogger(PooledJMSConnectionFactory.class);

    private GenericKeyedObjectPool<PooledConnectionKey, Connection> keyedObjectPool =
            new GenericKeyedObjectPool<>(this);

    private ConcurrentHashMap<Connection, PooledConnectionKey> connectionKeyMap = new ConcurrentHashMap<>();


    public PooledJMSConnectionFactory(Properties properties) throws JMSConnectorException {
        super(properties);

        Object maxConcurrentConnections = properties.get(JMSConstants.MAX_CONNECTIONS);

        if (maxConcurrentConnections != null) {
            keyedObjectPool.setMaxTotal(Integer.parseInt((String) maxConcurrentConnections));
        }

    }

    @Override
    public Connection createConnection() throws JMSException {
        PooledConnectionKey key = new PooledConnectionKey(null, null);
        try {
            return keyedObjectPool.borrowObject(key);
        } catch (Exception e) {
            throw new JMSException("Error occurred creating connection : " + e.getMessage());
        }
    }

    @Override
    public Connection createConnection(String userName, String password) throws JMSException {
        PooledConnectionKey key = new PooledConnectionKey(userName, password);
        try {
            return keyedObjectPool.borrowObject(key);
        } catch (Exception e) {
            throw new JMSException("Error occurred creating connection : " + e.getMessage());
        }
    }

    @Override
    public QueueConnection createQueueConnection() throws JMSException {
        return (QueueConnection) createConnection();
    }

    @Override
    public QueueConnection createQueueConnection(String userName, String password) throws JMSException {
        return (QueueConnection) createConnection(userName, password);
    }

    @Override
    public TopicConnection createTopicConnection() throws JMSException {
        return (TopicConnection) createConnection();
    }

    @Override
    public TopicConnection createTopicConnection(String userName, String password) throws JMSException {
        return (TopicConnection) createConnection(userName, password);
    }

    @Override
    public void closeConnection(Connection connection) throws JMSConnectorException {
        PooledConnectionKey key = connectionKeyMap.get(connection);

        if (key != null) {
            try {
                keyedObjectPool.returnObject(key, connection);
            } catch (Exception e) {
                throw new JMSConnectorException("Failed to return the connection back to the pool", e);
            }
        }
    }

    // PooledObjectFactory implementations

    /**
     * This is invoked when a new connection object has to be made.
     *
     * @param key The connection key
     * @return Wrapped connection object
     * @throws Exception Any exception thrown when creating a JMS connection will be thrown
     */
    @Override
    public PooledObject<Connection> makeObject(PooledConnectionKey key) throws Exception {
        Connection connection;

        String username = key.getUsername();
        String password = key.getPassword();

        if (username == null && password == null) {
            connection = super.createConnection();
        } else {

            connection = super.createConnection(key.getUsername(), key.getPassword());
        }

        connectionKeyMap.put(connection, key);

        return new DefaultPooledObject<>(connection);
    }

    /**
     * This is called when an connection object is invalidated.
     *
     * @param key                    The connection key
     * @param pooledConnectionObject The invalidated wrapped connection object.
     * @throws Exception Any exception thrown when closing a JMS connection will be thrown
     */
    @Override
    public void destroyObject(PooledConnectionKey key, PooledObject<Connection> pooledConnectionObject) throws
            Exception {
        Connection connection = pooledConnectionObject.getObject();

        connectionKeyMap.remove(connection);
        connection.close();
    }

    /**
     * Validates a connection object and verifies it can be used further.
     *
     * @param key                    The connection key
     * @param pooledConnectionObject The wrapped connection object
     * @return True if the object is valid
     */
    @Override
    public boolean validateObject(PooledConnectionKey key, PooledObject<Connection> pooledConnectionObject) {

        boolean valid = false;

        try {
            pooledConnectionObject.getObject().start();
            valid = true;
        } catch (JMSException e) {
            log.warn("Connection with key " + key + " is not valid anymore");
        }

        return valid;
    }

    /**
     * Activates a suspended connection.
     *
     * @param key                    The connection key
     * @param pooledConnectionObject Wrapped suspended connection object.
     * @throws Exception Any exception thrown when starting a JMS connection will be thrown
     */
    @Override
    public void activateObject(PooledConnectionKey key, PooledObject<Connection> pooledConnectionObject) throws
            Exception {
        pooledConnectionObject.getObject().start();
    }

    /**
     * Suspend a connection.
     *
     * @param key                    The connection key
     * @param pooledConnectionObject The wrapped connection object that needs to be suspended.
     * @throws Exception Any exception thrown when stopping a JMS connection will be thrown
     */
    @Override
    public void passivateObject(PooledConnectionKey key, PooledObject<Connection> pooledConnectionObject) throws
            Exception {
        pooledConnectionObject.getObject().stop();
    }

}
