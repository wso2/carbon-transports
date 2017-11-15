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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.factory.JMSConnectionResourceFactory;
import org.wso2.carbon.transport.jms.test.util.JMSServer;
import org.wso2.carbon.transport.jms.test.util.JMSTestConstants;
import org.wso2.carbon.transport.jms.test.util.JMSTestUtils;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

import java.util.HashMap;
import java.util.Map;
import javax.jms.JMSException;
import javax.jms.XAConnection;
import javax.jms.XASession;

/**
 * A test class for JMS resource creation methods with JMS API 1.1 in
 * {@link JMSConnectionResourceFactory}.
 */
public class JMSXAResourceCreationTestCase {
    private static final Logger logger = LoggerFactory.getLogger(JMSXAResourceCreationTestCase.class);
    private JMSServer jmsServer;
    private JMSConnectionResourceFactory jmsConnectionResourceFactory;
    private Map<String, String> properties;

    private XAConnection connection = null;
    private XASession session = null;

    @BeforeClass(groups = "jmsResources",
                 description = "Setting up the server and carbon message to be sent")
    public void setUp() throws JMSConnectorException {
        properties = new HashMap();
        properties.put(JMSConstants.PARAM_DESTINATION_NAME, JMSTestConstants.QUEUE_NAME_1);
        properties.put(JMSConstants.PARAM_CONNECTION_FACTORY_JNDI_NAME, JMSTestConstants.XA_CONNECTION_FACTORY);
        properties.put(JMSConstants.PARAM_NAMING_FACTORY_INITIAL, JMSTestConstants.ACTIVEMQ_FACTORY_INITIAL);
        properties.put(JMSConstants.PARAM_PROVIDER_URL, JMSTestConstants.ACTIVEMQ_PROVIDER_URL);
        properties.put(JMSConstants.PARAM_CONNECTION_FACTORY_TYPE, JMSConstants.DESTINATION_TYPE_QUEUE);
        properties.put(JMSConstants.PARAM_ACK_MODE, JMSConstants.XA_TRANSACTED_MODE);

        jmsConnectionResourceFactory = new JMSConnectionResourceFactory(
                JMSTestUtils.convertStringsToProperties(properties));

        jmsServer = new JMSServer();
        jmsServer.startServer();
    }

    @Test(groups = "jmsResources",
          description =
                  "Test if the createXAConnection, start, stop of JMSConnectionResourceFactory works fine without "
                          + "throwing exceptions")
    public void testCreateXAConnection() {
        Exception exception = null;

        try {
            connection = jmsConnectionResourceFactory.createXAConnection();
            jmsConnectionResourceFactory.start(connection);
            jmsConnectionResourceFactory.stop(connection);
        } catch (JMSException | JMSConnectorException e) {
            exception = e;
        }
        Assert.assertTrue(exception == null, "Error when creating/starting/stopping the xaconnection." + exception);
    }

    @Test(dependsOnMethods = { "testCreateXAConnection" },
          groups = "jmsResources",
          description = "Creates a JMS XA Session")
    public void testCreateXASession() {
        Exception exception = null;
        try {
            session = jmsConnectionResourceFactory.createXASession(connection);
        } catch (JMSConnectorException e) {
            exception = e;
        }
        Assert.assertTrue(exception == null, "Error when creating xasession." + exception);
    }
}
