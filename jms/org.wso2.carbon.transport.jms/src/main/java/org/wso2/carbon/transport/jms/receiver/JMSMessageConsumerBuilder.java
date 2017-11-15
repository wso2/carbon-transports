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

import org.wso2.carbon.transport.jms.contract.JMSListener;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.factory.JMSServerConnectionFactory;

/**
 * The {@link JMSMessageConsumerBuilder} used to consume messages in carbon jms transport.
 */
public class JMSMessageConsumerBuilder {
    /**
     * The {@link JMSServerConnectionFactory} instance which is used to create the consumer.
     */
    private JMSServerConnectionFactory connectionFactory = null;

    /**
     * Tells to use a message receiver instead of a message listener.
     */
    private boolean useReceiver = false;

    /**
     * The service Id which this consumer belongs to.
     */
    private String serviceId;

    /**
     * The {@link JMSListener} instance represents the message listener that handles the incoming
     * messages.
     */
    private JMSListener jmsListener;

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
     * @param connectionFactory The connection factory to use when creating the JMS connection.
     * @param jmsListener The message listener who is going to process the consumed messages from this.
     * @param serviceId The service Id which invoked this consumer.
     */
    public JMSMessageConsumerBuilder(JMSServerConnectionFactory connectionFactory,
                                     JMSListener jmsListener, String serviceId) {
        this.connectionFactory = connectionFactory;
        this.jmsListener = jmsListener;
        this.serviceId = serviceId;
    }

    public JMSMessageConsumerBuilder setUseReceiver(boolean useReceiver) {
        this.useReceiver = useReceiver;
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
     * @return the JMS consumer initialized with all required data.
     *
     * @throws JMSConnectorException If initializing the consumer fails.
     */
    public JMSMessageConsumer build() throws JMSConnectorException {
        return new JMSMessageConsumer(connectionFactory, useReceiver, jmsListener, serviceId,
                retryInterval, maxRetryCount);
    }

}
