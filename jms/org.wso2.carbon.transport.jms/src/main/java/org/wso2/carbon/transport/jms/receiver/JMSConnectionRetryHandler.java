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
     * This {@link JMSServerConnector} instance represents the jms receiver that asked for retry.
     */
    private JMSServerConnector jmsServerConnector;
    private static final Logger logger = LoggerFactory.getLogger(JMSConnectionRetryHandler.class);
    /**
     * Retry Interval in milli seconds.
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
     * @param jmsServerConnector JMS Server Connector
     * @param retryInterval      Retry interval between
     * @param maxRetryCount      Maximum retries
     */
    JMSConnectionRetryHandler(JMSServerConnector jmsServerConnector, long retryInterval, int maxRetryCount) {
        this.jmsServerConnector = jmsServerConnector;
        this.retryInterval = retryInterval;
        this.maxRetryCount = maxRetryCount;
    }

    /**
     * To retry the retrying to connect to JMS provider.
     *
     * @return True if retrying was successfull
     * @throws JMSConnectorException JMS Connector Exception
     */
    boolean retry() throws JMSConnectorException {

        if (logger.isDebugEnabled()) {
            logger.debug("Re-connection will be attempted after " + retryInterval + " milli-seconds.");
        }

        if (retrying) {

            if (logger.isDebugEnabled()) {
                logger.debug("Retrying is in progress from a different thread, hence not retrying");
            }

            return false;
        } else {
            retrying = true;
        }

        try {
            TimeUnit.MILLISECONDS.sleep(retryInterval);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        while (retryCount < maxRetryCount) {
            try {
                retryCount++;
                jmsServerConnector.startConsuming();
                logger.info("Connected to the message broker after retrying for " + retryCount + " time(s)");
                return true;
            } catch (JMSConnectorException ex) {
                if (null != jmsServerConnector.getConnection()) {
                    jmsServerConnector.closeAll();
                    throw new JMSConnectorException("JMS Connection succeeded but exception has occurred while "
                            + "creating, session or consumer from the connection");
                }
                jmsServerConnector.closeAll();
                if (retryCount < maxRetryCount) {
                    logger.error("Retry connection attempt " + retryCount + " to JMS Provider failed. Retry will be "
                            + "attempted ");
                    retryInterval = retryInterval * 2;
                    try {
                        TimeUnit.MILLISECONDS.sleep(retryInterval);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        retrying = false;

        throw new JMSConnectorException(
                "Connection to the jms provider failed after retrying for " + retryCount + " times");
    }
}
