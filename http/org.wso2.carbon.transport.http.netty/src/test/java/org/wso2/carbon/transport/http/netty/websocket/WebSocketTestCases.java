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
import org.wso2.carbon.transport.http.netty.util.clients.websocket.WebSocketClient;
import org.wso2.carbon.transport.http.netty.util.clients.websocket.WebSocketTestConstants;

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
    private WebSocketClient primaryClient = new WebSocketClient();
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
            assertTrue(primaryClient.handhshake());
            logger.info("Handshake test completed.");
        } catch (InterruptedException e) {
            logger.error("Handshake interruption.");
            assertTrue(false);
        }
    }

    @Test
    public void testText() throws URISyntaxException, InterruptedException, SSLException {
        primaryClient.handhshake();
        String text = "test";
        primaryClient.sendText(text);
        Thread.sleep(3000);
        String receivedText = primaryClient.getReceivedText();
        Assert.assertEquals(receivedText, text, "Not received the same text.");
        logger.info("pushing and receiving text data from server completed.");
        primaryClient.shutDown();
    }

    @Test
    public void testBinary() throws InterruptedException, URISyntaxException, SSLException {
        primaryClient.handhshake();
        byte[] bytes = {1,2,3,4,5};
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        primaryClient.sendBinary(buffer);
        Thread.sleep(3000);
        ByteBuffer receivedByteBuffer = primaryClient.getReceivedByteBuffer();
        assertTrue(buffer.capacity() == receivedByteBuffer.capacity(),
                   "Buffer capacity is not the same.");
        Assert.assertEquals(receivedByteBuffer, buffer, "Buffers data are not equal.");
        logger.info("pushing and receiving binary data from server completed.");
        primaryClient.shutDown();
    }

    @Test
    public void testClientConnected() throws InterruptedException, SSLException, URISyntaxException {
        primaryClient.handhshake();
        Thread.sleep(2000);
        secondaryClient.handhshake();
        Thread.sleep(5000);
        String receivedText = primaryClient.getReceivedText();
        logger.info("Received text : " + receivedText);
        Assert.assertEquals(receivedText, WebSocketTestConstants.NEW_CLIENT_CONNECTED,
                            "New Client was not connected.");
        logger.info("New client successfully connected to the server.");
        secondaryClient.shutDown();
        primaryClient.shutDown();
    }

    @Test
    public void testClientCloseConnection() throws InterruptedException, URISyntaxException, SSLException {
        primaryClient.handhshake();
        Thread.sleep(2000);
        secondaryClient.handhshake();
        Thread.sleep(3000);
        secondaryClient.shutDown();
        Thread.sleep(3000);
        String receivedText = primaryClient.getReceivedText();
        logger.info("Received Text : " + receivedText);
        Assert.assertEquals(receivedText, WebSocketTestConstants.CLIENT_LEFT);
        logger.info("Client left the server successfully.");
        primaryClient.shutDown();
        secondaryClient.shutDown();
    }

    @Test
    public void testPongMessage() throws InterruptedException, SSLException, URISyntaxException {
        primaryClient.handhshake();
        byte[] bytes = {6,7,8,9,10,11};
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        primaryClient.sendPong(byteBuffer);
        Thread.sleep(3000);
        ByteBuffer receivedBuffer = primaryClient.getReceivedByteBuffer();
        Assert.assertEquals(receivedBuffer, byteBuffer, "Didn't receive the correct pong.");
        logger.info("Receiving a pong message is completed.");
    }

    @AfterClass
    public void cleaUp() throws ServerConnectorException, InterruptedException {
        primaryClient.shutDown();
        secondaryClient.shutDown();
        serverConnectors.forEach(
                serverConnector -> {
                    serverConnector.stop();
                }
        );
    }
}
