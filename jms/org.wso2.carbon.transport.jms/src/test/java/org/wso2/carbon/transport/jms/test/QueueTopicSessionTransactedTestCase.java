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

package org.wso2.carbon.transport.jms.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.transport.jms.contract.JMSServerConnectorFuture;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.impl.JMSServerConnectorFutureImpl;
import org.wso2.carbon.transport.jms.receiver.JMSServerConnectorImpl;
import org.wso2.carbon.transport.jms.test.util.JMSServer;
import org.wso2.carbon.transport.jms.test.util.JMSTestConstants;
import org.wso2.carbon.transport.jms.test.util.JMSTestUtils;
import org.wso2.carbon.transport.jms.test.util.TestMessageListener;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

import java.util.Map;
import javax.jms.JMSException;
d
/**
 * Test case for queue topic listening in session transacted mode.
 */
public class QueueTopicSessionTransactedTestCase {
    private JMSServer jmsServer;
    private TestMessageListener queueTestMessageProcessor;
    private TestMessageListener topicTestMessageProcessor;
    private JMSServerConnectorImpl jmsQueueTransportListener;
    private JMSServerConnectorImpl jmsTopicTransportListener;
    private Map<String, String> queueListeningParameters;
    private Map<String, String> topicListeningParameters;
    private static final Logger logger = LoggerFactory.getLogger(QueueTopicAutoAckListeningTestCase.class);

    /**
     * Starts the JMS Server, and create two jms server connectors.
     * Make the server connectors to listen to topic and queue.
     *
     * @throws JMSConnectorException Server Connector Exception
     */
    @BeforeClass(groups = "jmsListening", description = "Setting up the server, JMS receiver and message processor")
    public void setUp() throws JMSConnectorException {
        queueListeningParameters = JMSTestUtils.createJMSListeningParameterMap(JMSTestConstants.QUEUE_NAME_3,
                JMSTestConstants.QUEUE_CONNECTION_FACTORY, JMSConstants.DESTINATION_TYPE_QUEUE, JMSConstants
                        .SESSION_TRANSACTED_MODE);
        topicListeningParameters = JMSTestUtils.createJMSListeningParameterMap(JMSTestConstants.TOPIC_NAME_2,
                JMSTestConstants.TOPIC_CONNECTION_FACTORY, JMSConstants.DESTINATION_TYPE_TOPIC, JMSConstants
                        .SESSION_TRANSACTED_MODE);
        jmsServer = new JMSServer();
        jmsServer.startServer();

        // Create a queue transport listener
        queueTestMessageProcessor = new TestMessageListener();
        JMSServerConnectorFuture queueFuture = new JMSServerConnectorFutureImpl(queueTestMessageProcessor);
        jmsQueueTransportListener = new JMSServerConnectorImpl("1", queueListeningParameters, queueFuture);

        // Create a topic transport listener
        topicTestMessageProcessor = new TestMessageListener();
        JMSServerConnectorFuture topicFuture = new JMSServerConnectorFutureImpl(topicTestMessageProcessor);
        jmsTopicTransportListener = new JMSServerConnectorImpl("3", topicListeningParameters, topicFuture);

    }

    /**
     * JMS Server connector will start listening to a queue in session transacted mode and there will be a publisher
     * publishing to the queue. For some messages call acknowledge, reject some messages so that session recover can
     * be called. After publishing the messages to queue, check whether the number of received messages are greater
     * than the sent messages, since the jms provider need to re-deliver the messages when session rollback is called.
     */
    @Test(groups = "jmsListening", description = "Testing whether queue listening is working correctly without any "
            + "exceptions in client ack mode")
    public void queueListeningTestCase() throws JMSConnectorException, InterruptedException, JMSException {
        jmsQueueTransportListener.start();
        logger.info("JMS Transport Listener is starting to listen to the queue " + JMSTestConstants.QUEUE_NAME_3);
        jmsServer.publishMessagesToQueue(JMSTestConstants.QUEUE_NAME_3);
        Assert.assertTrue(queueTestMessageProcessor.getCount() > 10,
                "Expected message count is not received when " + "listening to queue " +
                        JMSTestConstants.QUEUE_NAME_3);
        jmsQueueTransportListener.stop();
    }

    /**
     * JMS Server connector will start listening to a topic in session transacted mode  and there will be a publisher
     * publishing to the topic. For some messages call session.commit and for some messages, call session.rollback.
     * After publishing the messages to topic, check whether the number of received messages are greater
     * than the sent messages, since the jms provider need to re-deliver the messages when session rollback is called.
     */
    @Test(groups = "jmsListening", description = "Testing whether topic listening is working correctly without any "
            + "exceptions in client ack mode")
    public void topicListeningTestCase() throws JMSConnectorException, InterruptedException, JMSException {
        jmsTopicTransportListener.start();
        logger.info("JMS Transport Listener is starting to listen to the topic " + JMSTestConstants.TOPIC_NAME_2);
        jmsServer.publishMessagesToTopic(JMSTestConstants.TOPIC_NAME_2);
        Assert.assertTrue(topicTestMessageProcessor.getCount() > 10,
                "Expected message count is not received when " + "listening to topic " +
                        JMSTestConstants.TOPIC_NAME_2);
        jmsTopicTransportListener.stop();
    }

}
