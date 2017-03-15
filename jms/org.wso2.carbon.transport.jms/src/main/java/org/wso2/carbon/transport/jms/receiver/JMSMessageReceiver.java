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
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

/**
 * Message receiver for receiving JMS messages synchronously.
 */
public class JMSMessageReceiver implements Runnable, Thread.UncaughtExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(JMSMessageReceiver.class);

    private MessageConsumer messageConsumer;

    private JMSMessageHandler messageHandler;

    private boolean running = true;

    /**
     * The message receive wait timeout.
     */
    private long timeout = -1;

    /**
     * Initializes the message receiver with consumer and session details.
     *
     * @param messageProcessor The message processor which is going to process the received messages
     * @param serviceId        Id of the service that is interested in particular destination
     * @param session          The session that is used to create the consumer
     * @param messageConsumer  The {@link MessageConsumer} to use to receive messages
     * @throws JMSConnectorException Throws if message handler cannot be initialized
     */
    JMSMessageReceiver(CarbonMessageProcessor messageProcessor, String serviceId, Session session,
                       MessageConsumer messageConsumer) throws JMSConnectorException {

        this.messageConsumer = messageConsumer;

        messageHandler = new JMSMessageHandler(messageProcessor, serviceId, session);
    }

    /**
     * The runnable implementation which is invoked when message receiving is started.
     */
    @Override
    public void run() {
        while (running) {
            try {
                Message message;

                if (timeout < 0) {
                    message = messageConsumer.receive();
                } else {
                    message = messageConsumer.receive(timeout);
                }

                if (message != null) {
                    messageHandler.handle(message);
                } else {
                    logger.warn("Timeout expired or message consumer is concurrently closed");
                }
            } catch (JMSException e) {
                throw new RuntimeException("Error receiving messages", e);
            } catch (JMSConnectorException e) {
                // We're only logging the error here since we do not want to stop message receiving due to this
                logger.error("Error handling the received message", e);
            }
        }
    }

    /**
     * Start message receiving via message consumer.
     */
    public void receive() {
        startReceiverThread();
    }

    /**
     * Start receiving messages with a wait timeout.
     *
     * @param timeout Message receive wait timeout
     */
    public void receive(long timeout) {
        this.timeout = timeout;
        startReceiverThread();
    }

    /**
     * Start message receiving thread.
     */
    private void startReceiverThread() {
        Thread thread = new Thread(this);

        thread.setUncaughtExceptionHandler(this);

        thread.start();
    }

    /**
     * Any exception that was thrown when receiving messages from the receiver thread will be reported here.
     *
     * @param thread The thread which produced the error
     * @param error  The error
     */
    @Override
    public void uncaughtException(Thread thread, Throwable error) {
        running = false;
        logger.error("Unexpected error occurred while receiving messages", error);
    }
}
