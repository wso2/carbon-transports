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

package org.wso2.carbon.transport.jms.receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.kernel.utils.StringUtils;
import org.wso2.carbon.transport.jms.contract.JMSServerConnector;
import org.wso2.carbon.transport.jms.contract.JMSServerConnectorFuture;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.factory.CachedJMSConnectionFactory;
import org.wso2.carbon.transport.jms.factory.JMSConnectionFactory;
import org.wso2.carbon.transport.jms.factory.PooledJMSConnectionFactory;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This is the transport receiver for JMS.
 */
public class JMSServerConnectorImpl implements JMSServerConnector {
    private static final Logger logger = LoggerFactory.getLogger(JMSServerConnectorImpl.class);

    private String serviceId;

    private JMSServerConnectorFuture jmsServerConnectorFuture;

    /**
     * The {@link JMSConnectionFactory} instance represents the jms connection factory related with this server
     * connector.
     */
    private JMSConnectionFactory jmsConnectionFactory = null;

    /**
     * The number of concurrent consumers that needs to be created.
     */
    private int numOfConcurrentConsumers = 1;

    /**
     * List of {@link JMSMessageConsumer} instances that are created for this connector instance.
     */
    private List<JMSMessageConsumer> messageConsumers;
    /**
     * The {@link String} instance represents the jms connection user-name.
     */
    private String userName;
    /**
     * The {@link String} instance represents the jms connection password.
     */
    private String password;
    /**
     * The {@link Properties} instance represents the jms connection properties.
     */
    private Properties properties;

    /**
     * The retry interval (in milli seconds) if the connection is lost or if the connection cannot be established.
     */
    private long retryInterval = 10000;
    /**
     * The maximum retry count, for retrying to establish a jms connection with the jms provider.
     */
    private int maxRetryCount = 5;

    /**
     * Tells to use a message receiver instead of a message listener.
     */
    private boolean useReceiver = false;

    /**
     * The nature of the connection factory to use.
     */
    private String connectionFactoryNature = JMSConstants.DEFAULT_CONNECTION_FACTORY;

    public JMSServerConnectorImpl(String serviceId, Map<String, String> connectorConfig,
                                  JMSServerConnectorFuture jmsServerConnectorFuture) throws JMSConnectorException {
        this.jmsServerConnectorFuture = jmsServerConnectorFuture;
        this.serviceId = serviceId;
        init(connectorConfig);
    }

    private void init(Map<String, String> connectorConfig) throws JMSConnectorException {
        properties = new Properties();
        properties.putAll(connectorConfig);

        userName = connectorConfig.get(JMSConstants.CONNECTION_USERNAME);
        password = connectorConfig.get(JMSConstants.CONNECTION_PASSWORD);

        String retryIntervalParam = connectorConfig.get(JMSConstants.RETRY_INTERVAL);
        if (retryIntervalParam != null) {
            try {
                this.retryInterval = Long.parseLong(retryIntervalParam);
            } catch (NumberFormatException ex) {
                logger.error("Provided value for retry interval is invalid, using the default retry interval value "
                        + this.retryInterval);
            }
        }

        String maxRetryCountParam = connectorConfig.get(JMSConstants.MAX_RETRY_COUNT);
        if (maxRetryCountParam != null) {
            try {
                this.maxRetryCount = Integer.parseInt(maxRetryCountParam);
            } catch (NumberFormatException ex) {
                logger.error("Provided value for max retry count is invalid, using the default max retry count "
                        + this.maxRetryCount);
            }
        }

        String useReceiverParam = connectorConfig.get(JMSConstants.USE_RECEIVER);
        if (useReceiverParam != null) {
            useReceiver = Boolean.parseBoolean(useReceiverParam);
        }

        String concurrentConsumers = connectorConfig.get(JMSConstants.CONCURRENT_CONSUMERS);
        if (concurrentConsumers != null) {
            try {
                numOfConcurrentConsumers = Integer.parseInt(concurrentConsumers);
            } catch (NumberFormatException e) {
                logger.error("Provided value for " + JMSConstants.CONCURRENT_CONSUMERS +
                        " is invalid. Using the default value of " + numOfConcurrentConsumers);
            }
        }

        String connectionFactoryType = connectorConfig.get(JMSConstants.PARAM_CONNECTION_FACTORY_TYPE);
        if (connectionFactoryType != null) {
            if (JMSConstants.DESTINATION_TYPE_TOPIC.equalsIgnoreCase(connectionFactoryType)) {
                boolean isDurable = !StringUtils.isNullOrEmptyAfterTrim(
                        connectorConfig.get(JMSConstants.PARAM_SUB_DURABLE));

                if (isDurable && numOfConcurrentConsumers > 1) {
                    // If this is a non durable topic subscription then concurrent consumers should not be allowed
                    // since each subscription will get a duplicate of the same message
                    throw new JMSConnectorException("Concurrent consumers are not allowed for non-durable topic " +
                            "connections");
                }
            }
        }

        String connectionFacNatureParam = connectorConfig.get(JMSConstants.CONNECTION_FACTORY_NATURE);
        if (connectionFacNatureParam != null) {
            connectionFactoryNature = connectionFacNatureParam;
        }
    }

    @Override
    public void start() throws JMSConnectorException {
        startConsuming();
    }

    @Override
    public boolean stop() throws JMSConnectorException {
        closeAll();
        return true; //todo decide what to return
    }

    /**
     * Close the connection, session and consumers.
     *
     * @throws JMSConnectorException Exception that can be thrown when trying to close the connection, session
     *                               and message consumer
     */
    void closeAll() throws JMSConnectorException {
        JMSConnectorException exception = null;
        for (JMSMessageConsumer messageConsumer : messageConsumers) {
            try {
                messageConsumer.closeAll();
            } catch (JMSConnectorException e) {
                if (exception == null) {
                    exception = new JMSConnectorException("Error closing the consumers for service ", e);
                } else {
                    exception.addSuppressed(e);
                }
            }
        }

        messageConsumers = null;
        if (exception != null) {
            throw exception;
        }
    }

    /**
     * Start message consuming threads.
     *
     * @throws JMSConnectorException when consumer creation is failed due to a JMS layer error
     */
    void startConsuming() throws JMSConnectorException {
        try {
            if (jmsConnectionFactory == null) {
                switch (connectionFactoryNature) {
                    case JMSConstants.CACHED_CONNECTION_FACTORY :
                        jmsConnectionFactory = new CachedJMSConnectionFactory(properties);
                        break;
                    case JMSConstants.POOLED_CONNECTION_FACTORY :
                        jmsConnectionFactory = new PooledJMSConnectionFactory(properties);
                        break;
                    default :
                        jmsConnectionFactory = new JMSConnectionFactory(properties);
                }
            }

            messageConsumers = new ArrayList<>();
            for (int i = 0; i < numOfConcurrentConsumers; i++) {
                JMSMessageConsumerBuilder consumerBuilder = new JMSMessageConsumerBuilder(jmsConnectionFactory,
                        jmsServerConnectorFuture, serviceId);
                consumerBuilder.setUseReceiver(useReceiver)
                        .setUsername(userName)
                        .setPassword(password)
                        .setRetryInterval(retryInterval)
                        .setMaxRetryCount(maxRetryCount);
                messageConsumers.add(consumerBuilder.build());
            }
        } catch (JMSConnectorException e) {
            if (null == jmsConnectionFactory) {
                throw new JMSConnectorException("Cannot create the jms connection factory. please check the connection"
                        + " properties and re-deploy the jms service.", e);
            }
            throw e;
        }
    }
}
