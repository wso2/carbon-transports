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

package org.wso2.carbon.transport.jms.receiver;

import org.wso2.carbon.transport.jms.contract.JMSServerConnectorFuture;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.factory.JMSConnectionFactory;

/**
 * The {@link JMSMessageConsumerBuilder} used to consume messages in carbon jms transport.
 */
public class JMSMessageConsumerBuilder {
    /**
     * The {@link JMSConnectionFactory} instance which is used to create the consumer.
     */
    private JMSConnectionFactory connectionFactory = null;

    /**
     * Tells to use a message receiver instead of a message listener.
     */
    private boolean useReceiver = false;

    /**
     * The service Id which this consumer belongs to.
     */
    private String serviceId;

    /**
     * The {@link JMSServerConnectorFuture} instance represents the carbon message processor that handles the incoming
     * messages.
     */
    private JMSServerConnectorFuture jmsServerConnectorFuture;

    /**
     * The {@link String} instance represents the jms connection username.
     */
    private String username;
    /**
     * The {@link String} instance represents the jms connection password.
     */
    private String password;

    /**
     * The retry interval (in milli seconds) if the connection is lost or if the connection cannot be established.
     */
    private long retryInterval = 10000;
    /**
     * The maximum retry count, for retrying to establish a jms connection with the jms provider.
     */
    private int maxRetryCount = 5;

    /**
     * Initialize the builder with mandatory properties.
     *
     * @param connectionFactory The connection factory to use when creating the JMS connection
     * @param jmsServerConnectorFuture The message processor who is going to process the consumed messages from this
     * @param serviceId The service Id which invoked this consumer
     */
    public JMSMessageConsumerBuilder(JMSConnectionFactory connectionFactory,
                                     JMSServerConnectorFuture jmsServerConnectorFuture, String serviceId) {
        this.connectionFactory = connectionFactory;
        this.jmsServerConnectorFuture = jmsServerConnectorFuture;
        this.serviceId = serviceId;
    }

    public JMSMessageConsumerBuilder setUseReceiver(boolean useReceiver) {
        this.useReceiver = useReceiver;
        return this;
    }

    public JMSMessageConsumerBuilder setUsername(String username) {
        this.username = username;
        return this;
    }

    public JMSMessageConsumerBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public JMSMessageConsumerBuilder setRetryInterval(long retryInterval) {
        this.retryInterval = retryInterval;
        return this;
    }

    public JMSMessageConsumerBuilder setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
        return this;
    }

    /**
     * Build the {@link JMSMessageConsumer} with the given data.
     * @return the JMS consumer initialized with all required data
     *
     * @throws JMSConnectorException If initializing the consumer fails
     */
    public JMSMessageConsumer build() throws JMSConnectorException {
        return new JMSMessageConsumer(connectionFactory, useReceiver, jmsServerConnectorFuture, serviceId, username,
                password, retryInterval, maxRetryCount);
    }

}
