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
import org.wso2.carbon.transport.jms.clientfactory.ConnectionWrapper;
import org.wso2.carbon.transport.jms.clientfactory.ExtendedJMSClientConnectionFactory;
import org.wso2.carbon.transport.jms.clientfactory.SessionWrapper;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.factory.JMSImprovedConnectionFactory;

import java.util.List;
import javax.jms.JMSException;

public class SessionPoolFactory extends BasePooledObjectFactory<SessionWrapper> {

    private static final Logger logger = LoggerFactory.getLogger(SessionPoolFactory.class);
    private JMSImprovedConnectionFactory jmsConnectionFactory;

    public SessionPoolFactory(JMSImprovedConnectionFactory jmsConnectionFactory) {
        this.jmsConnectionFactory = jmsConnectionFactory;
    }

    @Override
    public synchronized SessionWrapper create() throws Exception {
        List<ConnectionWrapper> connectionWrappers = ((ExtendedJMSClientConnectionFactory) jmsConnectionFactory)
                .getConnections();
        ConnectionWrapper connectionWrapper = null;
        for (int i = 0; i < connectionWrappers.size(); i++) {
            if (connectionWrappers.get(i).getSessionCount().get() < ExtendedJMSClientConnectionFactory
                    .getMaxSessionsPerConnection()) {
                connectionWrapper = connectionWrappers.get(i);
                break;
            }
        }
        if (connectionWrapper == null) {
            connectionWrapper = new ConnectionWrapper(jmsConnectionFactory.createConnection());
            logger.info("creating connection " + connectionWrappers.size());
            connectionWrappers.add(connectionWrapper);
        }
        SessionWrapper sessionWrapper = new SessionWrapper(connectionWrapper.getConnection()
                .createSession(jmsConnectionFactory.isTransactedSession(), jmsConnectionFactory.getSessionAckMode()));
        logger.info("creating session");
        connectionWrapper.incrementSessionCount();
        return sessionWrapper;
    }

    @Override
    public PooledObject<SessionWrapper> wrap(SessionWrapper sessionWrapper) {
        return new DefaultPooledObject<SessionWrapper>(sessionWrapper);
    }

    @Override
    public void destroyObject(PooledObject<SessionWrapper> sessionWrapper) throws Exception {
        try {
            logger.info("Closing sessions/ producers");
            sessionWrapper.getObject().getMessageProducer().close();
            sessionWrapper.getObject().getSession().close();
        } catch (JMSException e) {
            throw new JMSConnectorException("Error when closing the JMS session/producer", e);
        }
    }

}
