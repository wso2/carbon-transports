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

package org.wso2.carbon.transport.jms.clientfactory.sessionpool;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.jms.clientfactory.JMSClientConnectionFactory;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.factory.JMSConnectionResourceFactory;
import org.wso2.carbon.transport.jms.wrappers.ConnectionWrapper;
import org.wso2.carbon.transport.jms.wrappers.SessionWrapper;
import org.wso2.carbon.transport.jms.wrappers.XASessionWrapper;

import java.util.List;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.XAConnection;
import javax.jms.XASession;

/**
 * Class responsible for creation/deletion of pooled objects
 */
public class SessionPoolFactory extends BasePooledObjectFactory<SessionWrapper> {

    private static final Logger logger = LoggerFactory.getLogger(SessionPoolFactory.class);
    private JMSConnectionResourceFactory jmsConnectionFactory;

    public SessionPoolFactory(JMSConnectionResourceFactory jmsConnectionFactory) {
        this.jmsConnectionFactory = jmsConnectionFactory;
    }

    @Override
    public synchronized SessionWrapper create() throws Exception {
        List<ConnectionWrapper> connectionWrappers;
        ConnectionWrapper connectionWrapper = null;
        SessionWrapper sessionWrapper = null;

        if (jmsConnectionFactory instanceof JMSClientConnectionFactory) {
            connectionWrappers = ((JMSClientConnectionFactory) jmsConnectionFactory).getConnections();

            // see if we can create more sessions on the final Connection created
            if (!connectionWrappers.isEmpty()
                    && connectionWrappers.get(connectionWrappers.size() - 1).getSessionCount().get()
                    < JMSClientConnectionFactory.getMaxSessionsPerConnection()) {
                connectionWrapper = connectionWrappers.get(connectionWrappers.size() - 1);
            }

            // if it needs to create a new connectionWrapper
            if (connectionWrapper == null) {
                if (jmsConnectionFactory.isxATransacted()) {
                    connectionWrapper = new ConnectionWrapper((jmsConnectionFactory.createXAConnection()));
                } else {
                    connectionWrapper = new ConnectionWrapper(jmsConnectionFactory.createConnection());
                }
                connectionWrappers.add(connectionWrapper);
            }

            // Create new SessionWrapper (or XASessionWrapper) accordingly
            if (jmsConnectionFactory.isxATransacted()) {
                XASession xASession = jmsConnectionFactory
                        .createXASession((XAConnection) connectionWrapper.getConnection());
                sessionWrapper = new XASessionWrapper(xASession, xASession.getSession(),
                        jmsConnectionFactory.createMessageProducer(xASession.getSession()));
            } else {
                Session session = jmsConnectionFactory.createSession(connectionWrapper.getConnection());
                sessionWrapper = new SessionWrapper(session, jmsConnectionFactory.createMessageProducer(session));
            }
            connectionWrapper.incrementSessionCount();
        }
        return sessionWrapper;
    }

    @Override
    public PooledObject<SessionWrapper> wrap(SessionWrapper sessionWrapper) {
        return new DefaultPooledObject<SessionWrapper>(sessionWrapper);
    }

    @Override
    public void destroyObject(PooledObject<SessionWrapper> sessionWrapper) throws Exception {
        try {
            sessionWrapper.getObject().getMessageProducer().close();
            sessionWrapper.getObject().getSession().close();
        } catch (JMSException e) {
            throw new JMSConnectorException("Error when closing the JMS session/producer", e);
        }
    }

}
