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

package org.wso2.carbon.transport.jms.test.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.factory.JMSConnectionResourceFactory;
import org.wso2.carbon.transport.jms.test.util.JMSBrokerService;
import org.wso2.carbon.transport.jms.test.util.JMSTestConstants;
import org.wso2.carbon.transport.jms.test.util.JMSTestUtils;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

import java.util.Map;
import javax.jms.Connection;
import javax.jms.JMSException;

/**
 * A test class for JMS connection creation methods with credentials
 * {@link JMSConnectionResourceFactory}
 */
public class JMSResourceCreationWithCredentialsTestCase {
    private static final Logger logger = LoggerFactory.getLogger(JMSResourceCreationWithCredentialsTestCase.class);
    //JMS broker credentials
    private final String username = "user";
    private final String password = "password";
    private JMSBrokerService jmsBrokerService;

    private JMSConnectionResourceFactory jmsConnectionResourceFactory;
    private JMSConnectionResourceFactory jmsConnectionXAResourceFactory;
    private JMSConnectionResourceFactory jmsConnectionTopicResourceFactory;
    private JMSConnectionResourceFactory jmsConnectionQueueResourceFactory;
    private JMSConnectionResourceFactory jmsConnectionXATopicResourceFactory;
    private JMSConnectionResourceFactory jmsConnectionXAQueueResourceFactory;
    private Connection connection = null;

    @BeforeClass(groups = "jmsResources",
                 description = "Setting up the server and carbon message to be sent")
    public void setUp() throws Exception {

        // Creation of ConnectionFactories
        Map<String, String> config = JMSTestUtils
                .createJMSParameterMap(JMSTestConstants.QUEUE_NAME_1, JMSTestConstants.QUEUE_CONNECTION_FACTORY,
                        JMSConstants.DESTINATION_TYPE_QUEUE, JMSConstants.AUTO_ACKNOWLEDGE_MODE);
        setCredentials(config);
        jmsConnectionResourceFactory = new JMSConnectionResourceFactory(
                JMSTestUtils.convertStringsToProperties(config));

        Map<String, String> xaConfig = JMSTestUtils
                .createJMSParameterMap(JMSTestConstants.QUEUE_NAME_1, JMSTestConstants.XA_CONNECTION_FACTORY,
                        JMSConstants.DESTINATION_TYPE_QUEUE, JMSConstants.XA_TRANSACTED_MODE);
        setCredentials(xaConfig);
        jmsConnectionXAResourceFactory = new JMSConnectionResourceFactory(
                JMSTestUtils.convertStringsToProperties(xaConfig));

        Map<String, String> topicConfig = JMSTestUtils
                .createJMSParameterMap(JMSTestConstants.TOPIC_NAME_1, JMSTestConstants.TOPIC_CONNECTION_FACTORY,
                        JMSConstants.DESTINATION_TYPE_TOPIC, JMSConstants.AUTO_ACKNOWLEDGE_MODE);
        topicConfig.put(JMSConstants.PARAM_JMS_SPEC_VER, JMSConstants.JMS_SPEC_VERSION_1_0);
        setCredentials(topicConfig);
        jmsConnectionTopicResourceFactory = new JMSConnectionResourceFactory(
                JMSTestUtils.convertStringsToProperties(topicConfig));

        Map<String, String> queueConfig = JMSTestUtils
                .createJMSParameterMap(JMSTestConstants.QUEUE_NAME_1, JMSTestConstants.QUEUE_CONNECTION_FACTORY,
                        JMSConstants.DESTINATION_TYPE_QUEUE, JMSConstants.AUTO_ACKNOWLEDGE_MODE);
        queueConfig.put(JMSConstants.PARAM_JMS_SPEC_VER, JMSConstants.JMS_SPEC_VERSION_1_0);
        setCredentials(queueConfig);
        jmsConnectionQueueResourceFactory = new JMSConnectionResourceFactory(
                JMSTestUtils.convertStringsToProperties(queueConfig));

        Map<String, String> topicXAConfig = JMSTestUtils
                .createJMSParameterMap(JMSTestConstants.TOPIC_NAME_1, JMSTestConstants.XA_CONNECTION_FACTORY,
                        JMSConstants.DESTINATION_TYPE_TOPIC, JMSConstants.XA_TRANSACTED_MODE);
        topicXAConfig.put(JMSConstants.PARAM_JMS_SPEC_VER, JMSConstants.JMS_SPEC_VERSION_1_0);
        setCredentials(topicXAConfig);
        jmsConnectionXATopicResourceFactory = new JMSConnectionResourceFactory(
                JMSTestUtils.convertStringsToProperties(topicXAConfig));

        Map<String, String> queueXAConfig = JMSTestUtils
                .createJMSParameterMap(JMSTestConstants.QUEUE_NAME_1, JMSTestConstants.XA_CONNECTION_FACTORY,
                        JMSConstants.DESTINATION_TYPE_QUEUE, JMSConstants.XA_TRANSACTED_MODE);
        queueXAConfig.put(JMSConstants.PARAM_JMS_SPEC_VER, JMSConstants.JMS_SPEC_VERSION_1_0);
        setCredentials(queueXAConfig);
        jmsConnectionXAQueueResourceFactory = new JMSConnectionResourceFactory(
                JMSTestUtils.convertStringsToProperties(queueXAConfig));

        jmsBrokerService = new JMSBrokerService(JMSTestConstants.ACTIVEMQ_PROVIDER_URL_2);
        jmsBrokerService.startBroker();
    }

    @Test(groups = "jmsResources",
          description = "Test Connection creation with the specific connection factory")
    public void testConnection() {
        Exception exception = null;

        try {
            connection = jmsConnectionResourceFactory.createConnection();
            jmsConnectionResourceFactory.start(connection);
            jmsConnectionResourceFactory.stop(connection);
        } catch (JMSException | JMSConnectorException e) {
            exception = e;
        }
        Assert.assertTrue(exception == null, "Error when creating/starting/stopping the connection." + exception);
    }

    @Test(groups = "jmsResources",
          description = "Test Connection creation with the specific connection factory")
    public void testXAConnection() {
        Exception exception = null;

        try {
            connection = jmsConnectionXAResourceFactory.createXAConnection();
            jmsConnectionResourceFactory.start(connection);
            jmsConnectionResourceFactory.stop(connection);
        } catch (JMSException | JMSConnectorException e) {
            exception = e;
        }
        Assert.assertTrue(exception == null, "Error when creating/starting/stopping the xa connection." + exception);
    }

    @Test(groups = "jmsResources",
          description = "Test Connection creation with the specific connection factory")
    public void testTopicConnection() {
        Exception exception = null;

        try {
            connection = jmsConnectionTopicResourceFactory.createConnection();
            jmsConnectionResourceFactory.start(connection);
            jmsConnectionResourceFactory.stop(connection);
        } catch (JMSException | JMSConnectorException e) {
            exception = e;
        }
        Assert.assertTrue(exception == null, "Error when creating/starting/stopping the topic connection." + exception);
    }

    @Test(groups = "jmsResources",
          description = "Test Connection creation with the specific connection factory")
    public void testQueueConnection() {
        Exception exception = null;

        try {
            connection = jmsConnectionQueueResourceFactory.createConnection();
            jmsConnectionResourceFactory.start(connection);
            jmsConnectionResourceFactory.stop(connection);
        } catch (JMSException | JMSConnectorException e) {
            exception = e;
        }
        Assert.assertTrue(exception == null, "Error when creating/starting/stopping the queue connection." + exception);
    }

    @Test(groups = "jmsResources",
          description = "Test Connection creation with the specific connection factory")
    public void testTopicXAConnection() {
        Exception exception = null;

        try {
            connection = jmsConnectionXATopicResourceFactory.createXAConnection();
            jmsConnectionResourceFactory.start(connection);
            jmsConnectionResourceFactory.stop(connection);
        } catch (JMSException | JMSConnectorException e) {
            exception = e;
        }
        Assert.assertTrue(exception == null,
                "Error when creating/starting/stopping the topic xa connection." + exception);
    }

    @Test(groups = "jmsResources",
          description = "Test Connection creation with the specific connection factory")
    public void testQueueXAConnection() {
        Exception exception = null;

        try {
            connection = jmsConnectionXAQueueResourceFactory.createXAConnection();
            jmsConnectionResourceFactory.start(connection);
            jmsConnectionResourceFactory.stop(connection);
        } catch (JMSException | JMSConnectorException e) {
            exception = e;
        }
        Assert.assertTrue(exception == null,
                "Error when creating/starting/stopping the queue xa connection." + exception);
    }

    private void setCredentials(Map<String, String> configuration) {
        configuration.put(JMSConstants.CONNECTION_USERNAME, username);
        configuration.put(JMSConstants.CONNECTION_PASSWORD, password);
        configuration.put(JMSConstants.PARAM_PROVIDER_URL, JMSTestConstants.ACTIVEMQ_PROVIDER_URL_2);
    }

    @AfterClass
    public void cleanUp() throws Exception {
        jmsBrokerService.stopBroker();
    }
}
