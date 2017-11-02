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
import org.wso2.carbon.transport.jms.sender.wrappers.XASessionWrapper;
import org.wso2.carbon.transport.jms.test.server.QueueTopicAutoAckListeningTestCase;
import org.wso2.carbon.transport.jms.test.util.DistributedTxManagerProvider;
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
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

/**
 * Test case for queue topic sending in xa transacted mode.
 */
public class QueueTopicXATransactedSenderTestCase {
    private static final Logger logger = LoggerFactory.getLogger(QueueTopicAutoAckListeningTestCase.class);
    private final String xaQueueName = "XAQueue";
    private final String xaTopicName = "XATopic";

    private JMSServer jmsServer;

    private Map<String, String> queueListeningParameters;
    private Map<String, String> topicListeningParameters;

    private int receivedMsgCount = 0;
    private JMSClientConnector jmsClientConnectorQueue;
    private JMSClientConnector jmsClientConnectorTopic;

    /**
     * Starts the JMS Server, and create two jms client connectors.
     *
     * @throws JMSConnectorException Client Connector Exception.
     */
    @BeforeClass(groups = "jmsListening",
                 description = "Setting up the server, JMS senders")
    public void setUp() throws JMSConnectorException {
        queueListeningParameters = JMSTestUtils
                .createJMSParameterMap(xaQueueName, JMSTestConstants.XA_CONNECTION_FACTORY,
                        JMSConstants.DESTINATION_TYPE_QUEUE, JMSConstants.XA_TRANSACTED_MODE);
        topicListeningParameters = JMSTestUtils
                .createJMSParameterMap(xaTopicName, JMSTestConstants.XA_CONNECTION_FACTORY,
                        JMSConstants.DESTINATION_TYPE_TOPIC, JMSConstants.XA_TRANSACTED_MODE);
        jmsServer = new JMSServer();
        jmsServer.startServer();

        jmsClientConnectorQueue = new JMSConnectorFactoryImpl().createClientConnector(queueListeningParameters);
        jmsClientConnectorTopic = new JMSConnectorFactoryImpl().createClientConnector(topicListeningParameters);
    }

    /**
     * This test will publish set of messages through XA transaction and then rollback, then publish another set of
     * messages. All this time a consumer will be running on the particular Queue, and total of the consumed messages
     * should be equal to the total number of committed messages.
     *
     * @throws JMSConnectorException      Error when sending messages through JMS transport connector.
     * @throws InterruptedException       Interruption when thread sleep.
     * @throws JMSException               Error when running the test message consumer.
     * @throws SystemException            XA Transaction exceptions.
     * @throws NotSupportedException      Transaction exceptions.
     * @throws RollbackException          Transaction exceptions.
     * @throws HeuristicRollbackException Transaction exceptions.
     * @throws HeuristicMixedException    Transaction exceptions.
     * @throws NoSuchFieldException       Error when accessing the private field.
     * @throws IllegalAccessException     Error when accessing the private field.
     */
    @Test(groups = "jmsSending",
          description = "XA transacted queue sending tested case")
    public void queueSendingTestCase()
            throws JMSConnectorException, InterruptedException, JMSException, SystemException, NotSupportedException,
            RollbackException, HeuristicRollbackException, HeuristicMixedException, NoSuchFieldException,
            IllegalAccessException {

        // Run message consumer and publish messages
        performPublishAndConsume(jmsClientConnectorQueue, xaQueueName, false);

        JMSTestUtils.closeResources((JMSClientConnectorImpl) jmsClientConnectorQueue);

        Assert.assertEquals(receivedMsgCount, 5,
                "XA transacted queue sender expected message count " + 5 + " , received message count "
                        + receivedMsgCount);
    }

    /**
     * This test will publish set of messages through XA transaction and then rollback, then publish another set of
     * messages. All this time a consumer will be running on the particular Topic, and total of the consumed messages
     * should be equal to the total number of committed messages.
     *
     * @throws JMSConnectorException      Error when sending messages through JMS transport connector.
     * @throws InterruptedException       Interruption when thread sleep.
     * @throws JMSException               Error when running the test message consumer.
     * @throws SystemException            XA Transaction exceptions.
     * @throws NotSupportedException      Transaction exceptions.
     * @throws RollbackException          Transaction exceptions.
     * @throws HeuristicRollbackException Transaction exceptions.
     * @throws HeuristicMixedException    Transaction exceptions.
     * @throws NoSuchFieldException       Error when accessing the private field.
     * @throws IllegalAccessException     Error when accessing the private field.
     */
    @Test(groups = "jmsSending",
          description = "XA transacted topic sending tested case")
    public void topicSendingTestCase()
            throws JMSConnectorException, InterruptedException, JMSException, SystemException, NotSupportedException,
            RollbackException, HeuristicRollbackException, HeuristicMixedException, NoSuchFieldException,
            IllegalAccessException {

        // Run message consumer and publish messages
        performPublishAndConsume(jmsClientConnectorTopic, xaTopicName, true);

        JMSTestUtils.closeResources((JMSClientConnectorImpl) jmsClientConnectorTopic);

        Assert.assertEquals(receivedMsgCount, 5,
                "XA transacted topic sender expected message count " + 5 + " , received message count "
                        + receivedMsgCount);
    }

    private void performXATransactedSend(JMSClientConnector jmsClientConnector, String xaDestinationName)
            throws JMSConnectorException, SystemException, NotSupportedException, RollbackException,
            HeuristicRollbackException, HeuristicMixedException {

        SessionWrapper sessionWrapper;
        sessionWrapper = jmsClientConnector.acquireSession();

        // Get XAResource and JMS session from the XASession. XAResource is given to
        // the TM and JMS Session is given to the AP.
        XAResource xaResource = ((XASessionWrapper) sessionWrapper).getXASession().getXAResource();

        DistributedTxManagerProvider distributedTxManagerProvider = DistributedTxManagerProvider.getInstance();
        distributedTxManagerProvider.getTransactionManager().begin();

        Transaction transaction = distributedTxManagerProvider.getTransactionManager().getTransaction();
        transaction.enlistResource(xaResource);

        /* Publish 5 messages and rollback and then publish another 5 messages and commit.
           At the end there should only be total of 5 messages in the queue */

        // Send message using the received XASessionsWrapper
        for (int i = 0; i < 5; i++) {
            jmsClientConnector.sendTransactedMessage(jmsClientConnector.createMessage(JMSConstants.TEXT_MESSAGE_TYPE),
                    xaDestinationName, sessionWrapper);
        }

        distributedTxManagerProvider.getTransactionManager().rollback();
        distributedTxManagerProvider.getTransactionManager().begin();

        for (int i = 0; i < 5; i++) {
            jmsClientConnector.sendTransactedMessage(jmsClientConnector.createMessage(JMSConstants.TEXT_MESSAGE_TYPE),
                    xaDestinationName, sessionWrapper);
        }

        distributedTxManagerProvider.getTransactionManager().commit();
    }

    /**
     * Run a message Listener, perform the message publish and wait for a pre-defined time interval until it consumes
     * all the messages from the destination.
     *
     * @throws JMSException         Error when consuming messages.
     * @throws InterruptedException interruption occurred while sleeping.
     */
    private void performPublishAndConsume(JMSClientConnector clientConnector, String destinationName, boolean isTopic)
            throws JMSException, HeuristicMixedException, RollbackException, SystemException, JMSConnectorException,
            HeuristicRollbackException, NotSupportedException, InterruptedException {

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
            performXATransactedSend(clientConnector, destinationName);

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
