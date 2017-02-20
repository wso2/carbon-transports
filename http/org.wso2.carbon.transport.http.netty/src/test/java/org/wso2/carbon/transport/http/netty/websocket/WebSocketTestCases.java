/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.transport.http.netty.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.http.netty.common.Constants;
import org.wso2.carbon.transport.http.netty.config.TransportsConfiguration;
import org.wso2.carbon.transport.http.netty.config.YAMLTransportConfigurationBuilder;
import org.wso2.carbon.transport.http.netty.listener.HTTPServerConnector;
import org.wso2.carbon.transport.http.netty.util.TestUtil;
import org.wso2.carbon.transport.http.netty.util.client.websocket.WebSocketClient;
import org.wso2.carbon.transport.http.netty.util.server.HTTPServer;

import java.net.URISyntaxException;
import java.util.List;

import static org.testng.Assert.assertTrue;

/**
 * Test class for WebSocket Upgrade
 */
public class WebSocketTestCases {

    Logger logger = LoggerFactory.getLogger(WebSocketTestCases.class);
    private List<HTTPServerConnector> serverConnectors;
    private HTTPServer httpServer;
    private WebSocketClient client = new WebSocketClient();
    private static final String testValue = "Test Message";

    @BeforeClass
    public void setup() {
        TransportsConfiguration configuration = YAMLTransportConfigurationBuilder
                .build("src/test/resources/simple-test-config/netty-transports.yml");
        serverConnectors = TestUtil.startConnectors(configuration, new WebSocketMessageProcessor());
        httpServer = TestUtil.startHTTPServer(TestUtil.TEST_SERVER_PORT, testValue, Constants.TEXT_PLAIN);
    }

    @Test
    public void testHandshake() throws URISyntaxException {
        try {
            assertTrue(client.handhshake(TestUtil.TEST_HOST, TestUtil.TEST_SERVER_PORT));
            logger.info("Handshake test completed.");
        } catch (InterruptedException e) {
            logger.error("Handshake interruption.");
            assertTrue(false);
        }
    }

    @Test
    public void testText() throws URISyntaxException {
        String text = "test";
        Assert.assertTrue(client.sendText(text));
        logger.info("push text to server completed.");
    }

    @AfterClass
    public void cleaUp() throws ServerConnectorException {
        TestUtil.cleanUp(serverConnectors, httpServer);
    }
}
