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
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

import java.util.concurrent.TimeUnit;

/**
 * This class tries to connect to JMS provider until the maximum re-try count meets.
 */
class JMSConnectionRetryHandler {
    /**
     * This {@link JMSMessageConsumer} instance represents the jms receiver that asked for retry.
     */
    private JMSMessageConsumer messageConsumer;

    private static final Logger logger = LoggerFactory.getLogger(JMSConnectionRetryHandler.class);

    /**
     * Current retry interval in milliseconds.
     */
    private long currentRetryInterval;

    /**
     * Initial retry interval in milliseconds.
     */
    private long retryInterval;

    /**
     * Current retry count.
     */
    private int retryCount = 0;

    /**
     * Maximum retry count.
     */
    private int maxRetryCount;


    /**
     * States whether a retrying is in progress.
     */
    private volatile boolean retrying = false;

    /**
     * Creates a jms connection retry handler.
     *
     * @param messageConsumer    JMS message consumer that needs to retry
     * @param retryInterval      Retry interval between
     * @param maxRetryCount      Maximum retries
     */
    JMSConnectionRetryHandler(JMSMessageConsumer messageConsumer, long retryInterval, int maxRetryCount) {
        this.messageConsumer = messageConsumer;
        this.retryInterval = retryInterval;
        this.maxRetryCount = maxRetryCount;

        currentRetryInterval = retryInterval;
    }

    /**
     * To retry the retrying to connect to JMS provider.
     *
     * @return True if retrying was successfull
     * @throws JMSConnectorException JMS Connector Exception
     */
    boolean retry() throws JMSConnectorException {

        if (retrying) {

            if (logger.isDebugEnabled()) {
                logger.debug("Retrying is in progress from a different thread, hence not retrying");
            }

            return false;
        } else {
            retrying = true;
        }

        while (retryCount < maxRetryCount) {
            try {
                retryCount++;
                messageConsumer.startConsuming();
                logger.info("Connected to the message broker after retrying for " + retryCount + " time(s)");
                retryCount = 0;
                currentRetryInterval = retryInterval;
                retrying = false;
                return true;
            } catch (JMSConnectorException e) {
                try {
                    messageConsumer.closeAll();
                } catch (JMSConnectorException ex) {
                    logger.debug("Failed to close erroneous connection. This could be due to a broken connection.", ex);
                }
                if (retryCount < maxRetryCount) {

                    logger.error("Retry connection attempt " + retryCount + " to JMS Provider failed. Retry will be " +
                            "attempted again after " +
                            TimeUnit.SECONDS.convert(currentRetryInterval, TimeUnit.MILLISECONDS) + " seconds");

                    try {
                        TimeUnit.MILLISECONDS.sleep(currentRetryInterval);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }

                    currentRetryInterval = currentRetryInterval * 2;
                }
            }
        }

        retrying = false;

        throw new JMSConnectorException(
                "Connection to the jms provider failed after retrying for " + retryCount + " times");
    }
}
