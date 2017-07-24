/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.transport.filesystem.test;

import org.testng.Assert;

import org.testng.annotations.Test;
import org.wso2.carbon.messaging.ServerConnector;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.filesystem.connector.server.FileSystemServerConnectorProvider;
import org.wso2.carbon.transport.filesystem.connector.server.util.Constants;
import org.wso2.carbon.transport.filesystem.test.util.TestMessageProcessor;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Test case that tests the file server connector functionality.
 */
public class FileSystemServerConnectorTestCase {

    @Test(description = "Testing the scenario: reading a file and asserting whether its content " +
            "is equal to what is expected.")
    public void filePollingTestCase() throws ServerConnectorException, InterruptedException {
        FileSystemServerConnectorProvider provider = new FileSystemServerConnectorProvider();
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource("test");
        String file = url.getFile();
        File testFile = new File(file);
        String fileURI = testFile.getAbsolutePath();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.TRANSPORT_FILE_FILE_URI, fileURI);
        parameters.put(org.wso2.carbon.connector.framework.server.polling.Constants.POLLING_INTERVAL, "1000");
        parameters.put(Constants.ACTION_AFTER_PROCESS, Constants.ACTION_NONE);
        ServerConnector connector = provider.createConnector("testService", parameters);

        TestMessageProcessor messageProcessor = new TestMessageProcessor();
        connector.setMessageProcessor(messageProcessor);

        connector.start();
        messageProcessor.waitTillDone();
        Assert.assertEquals(messageProcessor.getFileContent(), "File Server Connector test");
        connector.stop();
    }

    @Test(description = "Testing the scenario: reading a file and asserting whether its content " +
                        "is equal to what is expected with cron expression.")
    public void filePollingCronTestCase() throws ServerConnectorException, InterruptedException {
        FileSystemServerConnectorProvider provider = new FileSystemServerConnectorProvider();
        ClassLoader classLoader = getClass().getClassLoader();
        String fileURI = new File(classLoader.getResource("fs-cron/").getFile()).getAbsolutePath();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.TRANSPORT_FILE_FILE_URI, fileURI);
        parameters.put(org.wso2.carbon.connector.framework.server.polling.Constants.CRON_EXPRESSION, "0/5 * * * * ?");
        ServerConnector connector = provider.createConnector("testService", parameters);

        TestMessageProcessor messageProcessor = new TestMessageProcessor();
        connector.setMessageProcessor(messageProcessor);

        connector.start();
        messageProcessor.waitTillDone();
        Assert.assertEquals(messageProcessor.getFileContent(), "File Server Connector cron test");
        connector.stop();
    }
}
