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
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.jms.jndi.listener.JMSServerConnector;
import org.wso2.carbon.transport.jms.jndi.utils.JMSConstants;
import org.wso2.carbon.transport.jms.test.util.JMSServer;
import org.wso2.carbon.transport.jms.test.util.JMSTestConstants;
import org.wso2.carbon.transport.jms.test.util.MessageProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * Test case for queue topic listening in client ack mode.
 */
public class QueueTopicClientAckListeningTestCase {
    private JMSServer jmsServer;
    private MessageProcessor queueMessageProcessor;
    private MessageProcessor topicMessageProcessor;
    private static final Logger logger = LoggerFactory.getLogger(QueueTopicAutoAckListeningTestCase.class);

    @BeforeClass(groups = "jmsListening", description = "Setting up the server, JMS listener and message processor")
    public void setUp() throws ServerConnectorException {
        Map<String, String> queueListeningParametes = new HashMap<>();
        queueListeningParametes.put(JMSConstants.DESTINATION_PARAM_NAME, JMSTestConstants.QUEUE_NAME_2);
        queueListeningParametes
                .put(JMSConstants.CONNECTION_FACTORY_JNDI_PARAM_NAME, JMSTestConstants.QUEUE_CONNECTION_FACTORY);
        queueListeningParametes
                .put(JMSConstants.NAMING_FACTORY_INITIAL_PARAM_NAME, JMSTestConstants.ACTIVEMQ_FACTORY_INITIAL);
        queueListeningParametes.put(JMSConstants.PROVIDER_URL_PARAM_NAME, JMSTestConstants.ACTIVEMQ_PROVIDER_URL);
        queueListeningParametes
                .put(JMSConstants.CONNECTION_FACTORY_TYPE_PARAM_NAME, JMSConstants.DESTINATION_TYPE_QUEUE);
        queueListeningParametes.put(JMSConstants.SESSION_ACK_MODE_PARAM_NAME, JMSConstants.CLIENT_ACKNOWLEDGE_MODE);

        Map<String, String> topicListeningParametes = new HashMap<>();
        topicListeningParametes.put(JMSConstants.DESTINATION_PARAM_NAME, JMSTestConstants.TOPIC_NAME_1);
        topicListeningParametes
                .put(JMSConstants.CONNECTION_FACTORY_JNDI_PARAM_NAME, JMSTestConstants.TOPIC_CONNECTION_FACTORY);
        topicListeningParametes
                .put(JMSConstants.NAMING_FACTORY_INITIAL_PARAM_NAME, JMSTestConstants.ACTIVEMQ_FACTORY_INITIAL);
        topicListeningParametes.put(JMSConstants.PROVIDER_URL_PARAM_NAME, JMSTestConstants.ACTIVEMQ_PROVIDER_URL);
        topicListeningParametes
                .put(JMSConstants.CONNECTION_FACTORY_TYPE_PARAM_NAME, JMSConstants.DESTINATION_TYPE_TOPIC);
        topicListeningParametes.put(JMSConstants.SESSION_ACK_MODE_PARAM_NAME, JMSConstants.CLIENT_ACKNOWLEDGE_MODE);
        jmsServer = new JMSServer();
        jmsServer.startServer();

        // Create a queue transport listener
        JMSServerConnector jmsQueueTransportListener = new JMSServerConnector("1");
        queueMessageProcessor = new MessageProcessor();
        jmsQueueTransportListener.setMessageProcessor(queueMessageProcessor);
        jmsQueueTransportListener.start(queueListeningParametes);
        jmsQueueTransportListener.init();

        // Create a topic transport listener
        JMSServerConnector jmsTopicTransportListener = new JMSServerConnector("2");
        topicMessageProcessor = new MessageProcessor();
        jmsTopicTransportListener.setMessageProcessor(topicMessageProcessor);
        jmsTopicTransportListener.start(topicListeningParametes);
        jmsTopicTransportListener.init();
    }

    @Test(groups = "jmsListening", description = "Testing whether queue listening is working correctly without any "
            + "exceptions in auto ack mode")
    public void queueListeningTestCase() {
        try {
            logger.info("JMS Transport Listener is starting to listen to the queue " + JMSTestConstants.QUEUE_NAME_2);
            jmsServer.publishMessagesToQueue(JMSTestConstants.QUEUE_NAME_2);
            Assert.assertTrue(queueMessageProcessor.getCount() > 10, "Expected message count is not received when "
                    + "listing to queue " + JMSTestConstants.QUEUE_NAME_2);
        } catch (Exception e) {
            Assert.fail("Error while listing to queue");
        }
    }

    @Test(groups = "jmsListening", description = "Testing whether topic listening is working correctly without any "
            + "exceptions in auto ack mode")
    public void topicListeningTestCase() {
        try {
            logger.info("JMS Transport Listener is starting to listen to the topic " + JMSTestConstants.TOPIC_NAME_1);
            jmsServer.publishMessagesToTopic(JMSTestConstants.TOPIC_NAME_1);
            Assert.assertTrue(topicMessageProcessor.getCount() > 10, "Expected message count is not received when "
                    + "listening to topic " +  JMSTestConstants.TOPIC_NAME_1);
        } catch (Exception e) {
            Assert.fail("Error while listing to topic");
        }
    }

}
