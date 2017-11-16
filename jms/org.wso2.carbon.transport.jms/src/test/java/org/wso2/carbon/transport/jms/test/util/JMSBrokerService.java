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

import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.security.JaasAuthenticationPlugin;

/**
 * A simple jms server class using Activemq embedded with authentication support.
 */
public class JMSBrokerService {

    public static final String BROKER_NAME = "activemq-jms-localhost";

    private BrokerService broker;

    public JMSBrokerService(String url) throws Exception {
        System.setProperty(JMSTestConstants.ACTIVEMQ_LOGIN_CONFIG,
                getClass().getClassLoader().getResource(JMSTestConstants.ACTIVEMQ_LOGIN_CONFIG_DIR).getPath());
        broker = new BrokerService();
        broker.setDataDirectory(JMSTestConstants.TEST_LOG_DIR);
        broker.setBrokerName(BROKER_NAME);
        broker.addConnector(url);
        broker.setPlugins(new BrokerPlugin[] { new JaasAuthenticationPlugin() });
    }

    public void startBroker() throws Exception {
        broker.start();
    }

    public void stopBroker() throws Exception {
        broker.stop();
    }
}
