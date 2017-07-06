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

package org.wso2.carbon.transport.file.test;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.messaging.ServerConnector;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.file.connector.server.FileServerConnectorProvider;
import org.wso2.carbon.transport.file.connector.server.util.Constants;
import org.wso2.carbon.transport.file.test.util.FileMessageProcessor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Test case that tests the file server connector functionality.
 */
public class FileServerConnectorTestCase {

    @Test(description = "Testing the scenario: reading a file and asserting whether its content " +
            "is equal to what is expected.")
    public void filePollingTestCase() throws ServerConnectorException, InterruptedException {
        FileServerConnectorProvider provider = new FileServerConnectorProvider();
        ClassLoader classLoader = getClass().getClassLoader();
        String fileURI = new File(classLoader.getResource("test.txt").getFile()).getAbsolutePath();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.TRANSPORT_FILE_FILE_URI, fileURI);
        parameters.put(Constants.SEEK, "0");
        parameters.put(org.wso2.carbon.connector.framework.server.polling.Constants.POLLING_INTERVAL, "1000");
        ServerConnector connector = provider.createConnector("testService", parameters);

        FileMessageProcessor messageProcessor = new FileMessageProcessor();
        connector.setMessageProcessor(messageProcessor);

        connector.start();
        messageProcessor.waitTillDone();
        Assert.assertEquals(messageProcessor.getFileContent(3), "four", "The read content does not match" +
                                                                          " the actual content of the file");
        Assert.assertEquals(messageProcessor.getFileContent(6), "seven", "The read content does not match" +
                                                                           " the actual content of the file");
        Assert.assertEquals(messageProcessor.getFileContent(10), "eleven", "The read content does not match" +
                                                                             " the actual content of the file");
        Assert.assertEquals(messageProcessor.getFileContent(16), "seventeen", "The read content does not match " +
                                                                            "the actual content of the file");
        Assert.assertEquals(messageProcessor.getFileContent(19), "twenty", "The read content does not match " +
                                                                             "the actual content of the file");
        connector.stop();
    }
}
