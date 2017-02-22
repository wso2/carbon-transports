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
import org.wso2.carbon.transport.http.netty.config.TransportsConfiguration;
import org.wso2.carbon.transport.http.netty.config.YAMLTransportConfigurationBuilder;
import org.wso2.carbon.transport.http.netty.listener.HTTPServerConnector;
import org.wso2.carbon.transport.http.netty.util.TestUtil;
import org.wso2.carbon.transport.http.netty.util.client.websocket.WebSocketClient;
import org.wso2.carbon.transport.http.netty.util.client.websocket.WebSocketTestConstants;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.List;

import javax.net.ssl.SSLException;

import static org.testng.Assert.assertTrue;

/**
 * Test class for WebSocket Upgrade
 */
public class WebSocketTestCases {

    Logger logger = LoggerFactory.getLogger(WebSocketTestCases.class);
    private List<HTTPServerConnector> serverConnectors;
    private WebSocketClient mainClient = new WebSocketClient();
    private WebSocketClient secondaryClient = new WebSocketClient();

    @BeforeClass
    public void setup() {
        logger.info("\n-------WebSocket Test Cases-------");
        TransportsConfiguration configuration = YAMLTransportConfigurationBuilder
                .build("src/test/resources/simple-test-config/netty-transports.yml");
        serverConnectors = TestUtil.startConnectors(configuration, new WebSocketMessageProcessor());
    }

    @Test
    public void handshakeTest() throws URISyntaxException, SSLException {
        try {
            assertTrue(mainClient.handhshake());
            logger.info("Handshake test completed.");
        } catch (InterruptedException e) {
            logger.error("Handshake interruption.");
            assertTrue(false);
        }
    }

    @Test
    public void testText() throws URISyntaxException, InterruptedException {
        String text = "test";
        mainClient.sendText(text);
        Thread.sleep(3000);
        String receivedText = mainClient.getReceivedText();
        Assert.assertEquals(receivedText, text, "Not received the same text.");
        logger.info("pushing and receiving text data from server completed.");
    }

    @Test
    public void testBinary() throws InterruptedException {
        byte[] bytes = {1,2,3,4,5};
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        mainClient.sendBinary(buffer);
        Thread.sleep(3000);
        ByteBuffer receivedByteBuffer = mainClient.getReceivedByteBuffer();
        assertTrue(buffer.capacity() == receivedByteBuffer.capacity(),
                   "Buffer capacity is not the same.");
        Assert.assertEquals(receivedByteBuffer, buffer, "Buffers data are not equal.");
        logger.info("pushing and receiving binary data from server completed.");
    }

    @Test
    public void test1ClientConnected() throws InterruptedException, SSLException, URISyntaxException {
        secondaryClient.handhshake();
        Thread.sleep(5000);
        String receivedText = mainClient.getReceivedText();
        logger.info("Received text : " + receivedText);
        Assert.assertEquals(receivedText, WebSocketTestConstants.NEW_CLIENT_CONNECTED,
                            "New Client was not connected.");
        logger.info("New client successfully connected to the server.");
    }

    @Test
    public void test2ClientCloseConnection() throws InterruptedException {
        secondaryClient.shutDown();
        Thread.sleep(5000);
        String receivedText = mainClient.getReceivedText();
        logger.info("Received Text : " + receivedText);
        Assert.assertEquals(receivedText, WebSocketTestConstants.CLIENT_LEFT);
        logger.info("Client left the server successfully.");
    }

    @AfterClass
    public void cleaUp() throws ServerConnectorException, InterruptedException {
        mainClient.shutDown();
        secondaryClient.shutDown();
        serverConnectors.forEach(
                serverConnector -> {
                    serverConnector.stop();
                }
        );
    }
}
