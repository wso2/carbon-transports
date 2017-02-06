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

package org.wso2.carbon.transport.jms.jndi.listener;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.ServerConnector;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.jms.jndi.exception.JMSServerConnectorException;
import org.wso2.carbon.transport.jms.jndi.factory.JMSConnectionFactory;
import org.wso2.carbon.transport.jms.jndi.utils.JMSConstants;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.Session;

/**
 * This is a transport listener for JMS
 */
public class JMSServerConnector extends ServerConnector {
    private static final Log logger = LogFactory.getLog(JMSServerConnector.class.getName());
    private CarbonMessageProcessor carbonMessageProcessor;
    private JMSConnectionFactory jmsConnectionFactory = null;
    private Connection connection;
    private Session session;
    private Destination destination;
    private MessageConsumer messageConsumer;
    private Properties properties;
    private int retryInterval = 1;
    private int maxRetryCount = 5;

    public JMSServerConnector(String id) {
        super(id);
    }
    public JMSServerConnector() {
        super("jms");
    }

    @SuppressFBWarnings({"REC_CATCH_EXCEPTION"})
    void createMessageListener() throws JMSServerConnectorException {
        try {
            jmsConnectionFactory = new JMSConnectionFactory(properties);
            connection = jmsConnectionFactory.createConnection();
            jmsConnectionFactory.start(connection);
            session = jmsConnectionFactory.createSession(connection);
            destination = jmsConnectionFactory.getDestination(session);
            messageConsumer = jmsConnectionFactory.createMessageConsumer(session, destination);
            messageConsumer.setMessageListener(
                    new JMSMessageListener(carbonMessageProcessor, id, session.getAcknowledgeMode(), session));
        } catch (Exception e) {
            logger.error("Error while creating the connection from connection factory. " + e.getMessage());
            throw new JMSServerConnectorException("Error while creating the connection from connection factory", e);
        }

    }

    /** Close the connection, session and consumer
     * @throws JMSServerConnectorException Exception that can be thrown when trying to close the connection, session
     * and message consumer
     */
    void closeAll() throws JMSServerConnectorException {
        jmsConnectionFactory.closeConnection(connection);
        jmsConnectionFactory.closeSession(session);
        jmsConnectionFactory.closeMessageConsumer(messageConsumer);
    }

    @Override
    public void setMessageProcessor(CarbonMessageProcessor carbonMessageProcessor) {
        this.carbonMessageProcessor = carbonMessageProcessor;
    }

    @Override
    public void init() throws ServerConnectorException {
        // not needed
    }

    @Override
    public void destroy() throws JMSServerConnectorException {
        closeAll();
    }


    @Override
    public void stop() throws JMSServerConnectorException {
       closeAll();
    }

    @Override
    protected void beginMaintenance() throws JMSServerConnectorException {
        jmsConnectionFactory.stop(connection);
    }

    @Override
    protected void endMaintenance() throws JMSServerConnectorException {
        jmsConnectionFactory.start(connection);
    }

    @Override
    public void start(Map<String, String> map) throws ServerConnectorException {
        properties = new Properties();
        Set<Map.Entry<String, String>> set = map.entrySet();
        for (Map.Entry<String, String> entry : set) {
            String mappedParameter = JMSConstants.MAPPING_PARAMETERS.get(entry.getKey());
            if (mappedParameter != null) {
                properties.put(mappedParameter, entry.getValue());
            }
        }

        String retryInterval = map.get(JMSConstants.RETRY_INTERVAL);
        if (retryInterval != null) {
            try {
                this.retryInterval = Integer.parseInt(retryInterval);
            } catch (NumberFormatException ex) {
                logger.error("Provided value for retry interval is invalid, using the default retry interval value "
                        + retryInterval);
            }
        }

        String maxRetryCount = map.get(JMSConstants.MAX_RETRY_COUNT);
        if (maxRetryCount != null) {
            try {
                this.maxRetryCount = Integer.parseInt(maxRetryCount);
            } catch (NumberFormatException ex) {
                logger.error("Provided value for max retry count is invalid, using the default max retry count "
                        + maxRetryCount);
            }
        }

        try {
            createMessageListener();
        } catch (JMSServerConnectorException e) {
            if (jmsConnectionFactory == null) {
                throw new JMSServerConnectorException(
                        "Cannot create the jms connection factory. please check the connection"
                                + " properties and re-deploy the jms service. " + e.getMessage());
            } else if (connection != null) {
                closeAll();
                throw e;
            }
            closeAll();
            JMSConnectionRetryHandler jmsConnectionRetryHandler = new JMSConnectionRetryHandler(this, this
                    .retryInterval, this.maxRetryCount);
            jmsConnectionRetryHandler.start();

        }

    }
}
