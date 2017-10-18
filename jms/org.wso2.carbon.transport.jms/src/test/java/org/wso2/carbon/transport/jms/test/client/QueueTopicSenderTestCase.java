/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.transport.jms.test.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.transport.jms.contract.JMSClientConnector;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.impl.JMSConnectorFactoryImpl;
import org.wso2.carbon.transport.jms.sender.JMSClientConnectorImpl;
import org.wso2.carbon.transport.jms.test.server.QueueTopicAutoAckListeningTestCase;
import org.wso2.carbon.transport.jms.test.util.JMSServer;
import org.wso2.carbon.transport.jms.test.util.JMSTestConstants;
import org.wso2.carbon.transport.jms.test.util.JMSTestUtils;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

import java.util.Map;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

/**
 * Test case for queue topic sending
 */
public class QueueTopicSenderTestCase {
    private static final Logger logger = LoggerFactory.getLogger(QueueTopicAutoAckListeningTestCase.class);
    private final String queueName = "jmsQueue";
    private final String topicName = "jmsTopic";

    private JMSServer jmsServer;

    private Map<String, String> queueListeningParameters;
    private Map<String, String> topicListeningParameters;

    private JMSClientConnector jmsClientConnectorQueue;
    private JMSClientConnector jmsClientConnectorTopic;

    /**
     * Starts the JMS Server, and create two jms client connectors.
     *
     * @throws JMSConnectorException Client Connector Exception
     */
    @BeforeClass(groups = "jmsListening",
                 description = "Setting up the server, JMS senders")
    public void setUp() throws JMSConnectorException {
        queueListeningParameters = JMSTestUtils
                .createJMSParameterMap(queueName, JMSTestConstants.QUEUE_CONNECTION_FACTORY,
                        JMSConstants.DESTINATION_TYPE_QUEUE, JMSConstants.AUTO_ACKNOWLEDGE_MODE);
        topicListeningParameters = JMSTestUtils
                .createJMSParameterMap(topicName, JMSTestConstants.TOPIC_CONNECTION_FACTORY,
                        JMSConstants.DESTINATION_TYPE_TOPIC, JMSConstants.AUTO_ACKNOWLEDGE_MODE);
        jmsServer = new JMSServer();
        jmsServer.startServer();

        jmsClientConnectorQueue = new JMSConnectorFactoryImpl().createClientConnector(queueListeningParameters);
        jmsClientConnectorTopic = new JMSConnectorFactoryImpl().createClientConnector(topicListeningParameters);
    }

    /**
     * This test will publish set of messages to a queue and then validate whether the same number of messages
     * consumed by the listener
     *
     * @throws JMSConnectorException  Error when sending messages through JMS transport connector
     * @throws InterruptedException   Interruption when thread sleep
     * @throws JMSException           Error when running the test message consumer
     * @throws NoSuchFieldException   Error when accessing the private field
     * @throws IllegalAccessException Error when accessing the private field
     */
    @Test(groups = "jmsSending",
          description = "Queue sending test case")
    public void queueSendingTestCase()
            throws InterruptedException, JMSException, JMSConnectorException, NoSuchFieldException,
            IllegalAccessException {

        // Run message consumer and publish messages
        int receivedMsgCount = performPublishAndConsume(jmsClientConnectorQueue, queueName, false, this.jmsServer);

        JMSTestUtils.closeResources((JMSClientConnectorImpl) jmsClientConnectorQueue);

        Assert.assertEquals(receivedMsgCount, 5,
                "Session queue sender expected message count " + 5 + " , received message count " + receivedMsgCount);
    }

    /**
     * This test will publish set of messages to a queue and then validate whether the same number of messages
     * consumed by the listener
     *
     * @throws JMSConnectorException  Error when sending messages through JMS transport connector
     * @throws InterruptedException   Interruption when thread sleep
     * @throws JMSException           Error when running the test message consumer
     * @throws NoSuchFieldException   Error when accessing the private field
     * @throws IllegalAccessException Error when accessing the private field
     */
    @Test(groups = "jmsSending",
          description = "Topic sending test case")
    public void topicSendingTestCase()
            throws InterruptedException, JMSException, JMSConnectorException, NoSuchFieldException,
            IllegalAccessException {

        // Run message consumer and publish messages
        int receivedMsgCount = performPublishAndConsume(jmsClientConnectorTopic, topicName, true, this.jmsServer);

        JMSTestUtils.closeResources((JMSClientConnectorImpl) jmsClientConnectorTopic);

        Assert.assertEquals(receivedMsgCount, 5,
                "Session topic sender expected message count " + 5 + " , received message count " + receivedMsgCount);
    }

    /**
     * Run a message Listener, perform the message publish and wait for a pre-defined time interval until it consumes
     * all the messages from the destination
     *
     * @return int instance
     * @throws JMSException          Error when consuming messages
     * @throws InterruptedException  interruption occurred while sleeping
     * @throws JMSConnectorException Error when publishing the messages
     */
    protected int performPublishAndConsume(JMSClientConnector clientConnector, String destinationName, boolean isTopic,
            JMSServer jmsServer) throws JMSException, InterruptedException, JMSConnectorException {

        Connection connection = null;
        Session session = null;
        MessageConsumer messageConsumer = null;

        try {
            connection = jmsServer.createConnection(null, null);
            session = jmsServer.createSession(connection, false, Session.AUTO_ACKNOWLEDGE);

            Destination destination = isTopic ?
                    session.createTopic(destinationName) :
                    session.createQueue(destinationName);
            messageConsumer = session.createConsumer(destination);
            MessageListenerCounter messageListenerCounter = new MessageListenerCounter();
            messageConsumer.setMessageListener(messageListenerCounter);

            // Start to consume messages
            connection.start();

            // preform the send through JMS transport implementation
            for (int i = 0; i < 5; i++) {
                clientConnector.send(clientConnector.createMessage(JMSConstants.TEXT_MESSAGE_TYPE), destinationName);
            }

            // Wait for a timedout, until messages are consumed
            Thread.sleep(2000);
            // Stop consumer
            connection.stop();

            return messageListenerCounter.getReceivedMsgCount();
        } finally {
            //close resources
            if (messageConsumer != null) {
                messageConsumer.close();
            }
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    /**
     * Sample Message Listener to count the number of consumed messages
     */
    class MessageListenerCounter implements MessageListener {

        private int receivedMsgCount = 0;

        @Override
        public void onMessage(Message message) {
            receivedMsgCount++;
        }

        public int getReceivedMsgCount() {
            return receivedMsgCount;
        }
    }

}
