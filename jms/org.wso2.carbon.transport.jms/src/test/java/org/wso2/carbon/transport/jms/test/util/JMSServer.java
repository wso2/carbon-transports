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

package org.wso2.carbon.transport.jms.test.util;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;

/**
 * A simple jms server using Activemq embedded broker.
 */
public class JMSServer {
    private Logger logger = LoggerFactory.getLogger(JMSServer.class);
    private ConnectionFactory connectionFactory;

    /**
     * To start the embedded activemq server.
     */
    public void startServer() {
        connectionFactory = new ActiveMQConnectionFactory(JMSTestConstants.ACTIVEMQ_PROVIDER_URL);
    }

    /**
     * To publish the messages to a queue.
     *
     * @throws JMSException         JMS Exception
     * @throws InterruptedException Interrupted exception while waiting in between messages
     */
    public void publishMessagesToQueue(String queueName) throws JMSException, InterruptedException {
        QueueConnection queueConn = (QueueConnection) connectionFactory.createConnection();
        queueConn.start();
        QueueSession queueSession = queueConn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = queueSession.createQueue(queueName);
        MessageProducer queueSender = queueSession.createProducer(destination);
        queueSender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        for (int index = 0; index < 10; index++) {
            String queueText = "Queue Message : " + (index + 1);
            TextMessage queueMessage = queueSession.createTextMessage(queueText);
            queueSender.send(queueMessage);
            Thread.sleep(1000);
            logger.info("Publishing " + queueText + " to queue " + queueName);
        }
        queueConn.close();
        queueSession.close();
        queueSender.close();
    }

    /**
     * To publish the messages to a topic.
     *
     * @throws JMSException         JMS Exception
     * @throws InterruptedException Interrupted exception while waiting in between messages
     */
    public void publishMessagesToTopic(String topicName) throws JMSException, InterruptedException {
        TopicConnection topicConnection = (TopicConnection) connectionFactory.createConnection();
        topicConnection.start();
        TopicSession topicSession = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = topicSession.createTopic(topicName);
        MessageProducer topicSender = topicSession.createProducer(destination);
        topicSender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        for (int index = 0; index < 10; index++) {
            String topicText = "Topic Message : " + (index + 1);
            TextMessage topicMessage = topicSession.createTextMessage(topicText);
            topicSender.send(topicMessage);
            logger.info("Publishing " + topicText + " to topic " + topicName);
            Thread.sleep(1000);
        }
        topicConnection.close();
        topicSession.close();
        topicSender.close();
    }

    /**
     * To receive a message from a queue.
     *
     * @throws JMSException         JMS Exception
     * @throws InterruptedException Interrupted exception while waiting in between messages
     */
    public void receiveMessagesFromQueue() throws JMSException, InterruptedException {
        QueueConnection queueConn = (QueueConnection) connectionFactory.createConnection();
        QueueSession queueSession = queueConn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = queueSession.createQueue(JMSTestConstants.QUEUE_NAME_1);
        MessageConsumer queueReceiver = queueSession.createConsumer(destination);
        MessageListener listener = message -> {
            try {
                if (message instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) message;
                    logger.info("Message text received : " + (textMessage.getText()));
                }
            } catch (JMSException e) {
                logger.info("JMS exception occurred.");
            }
        };
        queueReceiver.setMessageListener(listener);
        queueConn.start();
    }

    public Connection createConnection(String username, String password) throws JMSException {
        if (username != null && password != null) {
            return connectionFactory.createConnection(username, password);
        }
        return connectionFactory.createConnection();
    }

    public Session createSession(Connection connection, boolean isTransacted, int ackMode) throws JMSException {
        return connection.createSession(isTransacted, ackMode);
    }
}
