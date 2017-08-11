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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.TextCarbonMessage;
import org.wso2.carbon.transport.jms.contract.JMSClientConnector;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.sender.JMSClientConnectorImpl;
import org.wso2.carbon.transport.jms.test.util.JMSServer;
import org.wso2.carbon.transport.jms.test.util.JMSTestConstants;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;

/**
 * A test class for testing queue listening
 */
public class SendMessageTestCase {
    private JMSServer jmsServer;
    private CarbonMessage carbonMessage;
    private Map<String, String> properties;
    private static final Logger logger = LoggerFactory.getLogger(SendMessageTestCase.class);

    @BeforeClass(groups = "queueSending", description = "Setting up the server and carbon message to be sent")
    public void setUp() {
        carbonMessage = new TextCarbonMessage("Hello World");
        properties = new HashMap();
        properties.put(JMSConstants.PARAM_DESTINATION_NAME, JMSTestConstants.QUEUE_NAME_1);
        properties.put(JMSConstants.PARAM_CONNECTION_FACTORY_JNDI_NAME, JMSTestConstants.QUEUE_CONNECTION_FACTORY);
        properties.put(JMSConstants.PARAM_NAMING_FACTORY_INITIAL, JMSTestConstants.ACTIVEMQ_FACTORY_INITIAL);
        properties.put(JMSConstants.PARAM_PROVIDER_URL, JMSTestConstants.ACTIVEMQ_PROVIDER_URL);
        properties.put(JMSConstants.PARAM_CONNECTION_FACTORY_TYPE, JMSConstants.DESTINATION_TYPE_QUEUE);
        properties.put(JMSConstants.TEXT_DATA, "Hello World");
        properties.put(JMSConstants.JMS_MESSAGE_TYPE, JMSConstants.TEXT_MESSAGE_TYPE);
        jmsServer = new JMSServer();
        jmsServer.startServer();
    }

    @Test(groups = "queueSending", description = "Testing whether queue sending is working correctly without any " +
            "exceptions")
    public void queueListeningTestCase() throws InterruptedException, JMSException, JMSConnectorException {
        logger.info("JMS Transport Sender is sending a message to the queue " +
                    JMSTestConstants.QUEUE_NAME_1);
        JMSClientConnector sender = new JMSClientConnectorImpl();
        jmsServer.receiveMessagesFromQueue();
        sender.send(carbonMessage, properties);
        sender.send(carbonMessage, properties);
    }
}
