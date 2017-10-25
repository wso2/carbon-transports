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
import org.wso2.carbon.transport.jms.sender.wrappers.SessionWrapper;
import org.wso2.carbon.transport.jms.test.server.QueueTopicAutoAckListeningTestCase;
import org.wso2.carbon.transport.jms.test.util.JMSServer;
import org.wso2.carbon.transport.jms.test.util.JMSTestConstants;
import org.wso2.carbon.transport.jms.test.util.JMSTestUtils;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

import java.util.Map;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

/**
 * Test case for queue topic sending in session transacted mode.
 */
public class QueueTopicTransactedSenderTestCase {
    private static final Logger logger = LoggerFactory.getLogger(QueueTopicAutoAckListeningTestCase.class);
    private final String txQueueName = "txQueue";
    private final String txTopicName = "txTopic";

    private JMSServer jmsServer;

    private Map<String, String> queueListeningParameters;
    private Map<String, String> topicListeningParameters;

    private int receivedMsgCount = 0;
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
                .createJMSParameterMap(txQueueName, JMSTestConstants.QUEUE_CONNECTION_FACTORY,
                        JMSConstants.DESTINATION_TYPE_QUEUE, JMSConstants.SESSION_TRANSACTED_MODE);
        topicListeningParameters = JMSTestUtils
                .createJMSParameterMap(txTopicName, JMSTestConstants.TOPIC_CONNECTION_FACTORY,
                        JMSConstants.DESTINATION_TYPE_TOPIC, JMSConstants.SESSION_TRANSACTED_MODE);
        jmsServer = new JMSServer();
        jmsServer.startServer();

        jmsClientConnectorQueue = new JMSConnectorFactoryImpl().createClientConnector(queueListeningParameters);
        jmsClientConnectorTopic = new JMSConnectorFactoryImpl().createClientConnector(topicListeningParameters);
    }

    /**
     * This test will publish set of messages through session transaction and then rollback, then publish another set of
     * messages. All this time a consumer will be running on the particular Queue, and total of the consumed messages
     * should be equal to the total number of committed messages
     *
     * @throws JMSConnectorException  Error when sending messages through JMS transport connector
     * @throws InterruptedException   Interruption when thread sleep
     * @throws JMSException           Error when running the test message consumer
     * @throws NoSuchFieldException   Error when accessing the private field
     * @throws IllegalAccessException Error when accessing the private field
     */
    @Test(groups = "jmsSending",
          description = "Session transacted queue sending tested case")
    public void queueSendingTestCase()
            throws InterruptedException, JMSException, JMSConnectorException, NoSuchFieldException,
            IllegalAccessException {

        // Run message consumer and publish messages
        performPublishAndConsume(jmsClientConnectorQueue, txQueueName, false);

        JMSTestUtils.closeResources((JMSClientConnectorImpl) jmsClientConnectorQueue);

        Assert.assertEquals(receivedMsgCount, 5,
                "Session transacted queue sender expected message count " + 5 + " , received message count "
                        + receivedMsgCount);
    }

    /**
     * This test will publish set of messages through Session transaction and then rollback, then publish another set of
     * messages. All this time a consumer will be running on the particular Topic, and total of the consumed messages
     * should be equal to the total number of committed messages
     *
     * @throws JMSConnectorException  Error when sending messages through JMS transport connector
     * @throws InterruptedException   Interruption when thread sleep
     * @throws JMSException           Error when running the test message consumer
     * @throws NoSuchFieldException   Error when accessing the private field
     * @throws IllegalAccessException Error when accessing the private field
     */
    @Test(groups = "jmsSending",
          description = "Session transacted topic sending tested case")
    public void topicSendingTestCase()
            throws InterruptedException, JMSException, JMSConnectorException, NoSuchFieldException,
            IllegalAccessException {

        // Run message consumer and publish messages
        performPublishAndConsume(jmsClientConnectorTopic, txTopicName, true);

        JMSTestUtils.closeResources((JMSClientConnectorImpl) jmsClientConnectorTopic);

        Assert.assertEquals(receivedMsgCount, 5,
                "Session transacted topic sender expected message count " + 5 + " , received message count "
                        + receivedMsgCount);
    }

    private void performTransactedSend(JMSClientConnector jmsClientConnector, String txDestinationName)
            throws JMSConnectorException, JMSException {

        SessionWrapper sessionWrapper;
        sessionWrapper = jmsClientConnector.acquireSession();

        // Send message using the received XASessionsWrapper
        for (int i = 0; i < 5; i++) {
            jmsClientConnector.sendTransactedMessage(jmsClientConnector.createMessage(JMSConstants.TEXT_MESSAGE_TYPE),
                    txDestinationName, sessionWrapper);
        }

        sessionWrapper.getSession().rollback();

        for (int i = 0; i < 5; i++) {
            jmsClientConnector.sendTransactedMessage(jmsClientConnector.createMessage(JMSConstants.TEXT_MESSAGE_TYPE),
                    txDestinationName, sessionWrapper);
        }

        sessionWrapper.getSession().commit();
    }

    /**
     * Run a message Listener, perform the message publish and wait for a pre-defined time interval until it consumes
     * all the messages from the destination
     *
     * @throws JMSException          Error when consuming messages
     * @throws InterruptedException  interruption occurred while sleeping
     * @throws JMSConnectorException Error when publishing the messages
     */
    private void performPublishAndConsume(JMSClientConnector clientConnector, String destinationName, boolean isTopic)
            throws JMSException, InterruptedException, JMSConnectorException {

        //init the message count
        receivedMsgCount = 0;

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

            messageConsumer.setMessageListener(message -> receivedMsgCount++);

            // Start to consume messages
            connection.start();

            // preform the transacted send through JMS transport implementation
            performTransactedSend(clientConnector, destinationName);

            // Wait for a timedout, until messages are consumed
            Thread.sleep(2000);
            // Stop consumer
            connection.stop();
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
}
