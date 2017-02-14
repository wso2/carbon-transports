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

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.messaging.ServerConnector;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.file.connector.server.FileServerConnectorProvider;
import org.wso2.carbon.transport.file.connector.server.util.Constants;
import org.wso2.carbon.transport.file.test.util.TestMessageProcessor;

import java.util.HashMap;
import java.util.Map;

public class FileServerConnectorTestCase {
    @BeforeClass(description = "")
    public void setUp() throws FileSystemException {
        FileSystemManager fsManager = VFS.getManager();
        FileObject zipFile = fsManager.resolveFile("file:/path/to/the/file.zip");
        zipFile.createFile();
    }

    @Test(description = "Testing the scenario: reading a file and asserting whether its content " +
            "is equal to what is expected.")
    public void filePollingTestCase() throws ServerConnectorException, InterruptedException {
        FileServerConnectorProvider provider = new FileServerConnectorProvider();
        ServerConnector connector = provider.createConnector("testService");

        TestMessageProcessor messageProcessor = new TestMessageProcessor();
        connector.setMessageProcessor(messageProcessor);

        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.TRANSPORT_FILE_FILE_URI, "file:/src/test/java/resources/myfile.txt");
        parameters.put(org.wso2.carbon.connector.framework.server.polling.Constants.POLLING_INTERVAL, "1000");

        connector.start(parameters);

        try {
            messageProcessor.waitTillDone();
        } catch (InterruptedException e) {
            Assert.fail("Could not wait till the message is being read.");
        }

        Thread.sleep(100000);
        connector.stop();
    }

}
