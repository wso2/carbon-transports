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
import org.wso2.carbon.transport.jms.test.server.QueueTopicAutoAckListeningTestCase;
import org.wso2.carbon.transport.jms.test.util.JMSServer;
import org.wso2.carbon.transport.jms.test.util.JMSTestConstants;
import org.wso2.carbon.transport.jms.test.util.JMSTestUtils;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

import java.util.Map;
import javax.jms.JMSException;

/**
 * Test case for queue topic sending nonCached
 */
public class QueueTopicNonCachedSenderTestCase extends QueueTopicSenderTestCase {
    private static final Logger logger = LoggerFactory.getLogger(QueueTopicAutoAckListeningTestCase.class);
    private final String queueName = "jmsNonCachedQueue";
    private final String topicName = "jmsNonCachedTopic";

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

        //add non-cached parameter
        queueListeningParameters.put(JMSConstants.PARAM_JMS_CACHING, Boolean.FALSE.toString());
        topicListeningParameters.put(JMSConstants.PARAM_JMS_CACHING, Boolean.FALSE.toString());

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
          description = "Non-cached queue sending test case")
    public void queueSendingTestCase()
            throws InterruptedException, JMSException, JMSConnectorException, NoSuchFieldException,
            IllegalAccessException {

        // Run message consumer and publish messages
        int receivedMsgCount = performPublishAndConsume(jmsClientConnectorQueue, queueName, false, this.jmsServer);

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
          description = "Non cached topic sending test case")
    public void topicSendingTestCase()
            throws InterruptedException, JMSException, JMSConnectorException, NoSuchFieldException,
            IllegalAccessException {

        // Run message consumer and publish messages
        int receivedMsgCount = performPublishAndConsume(jmsClientConnectorTopic, topicName, true, this.jmsServer);

        Assert.assertEquals(receivedMsgCount, 5,
                "Session topic sender expected message count " + 5 + " , received message count " + receivedMsgCount);
    }
}
