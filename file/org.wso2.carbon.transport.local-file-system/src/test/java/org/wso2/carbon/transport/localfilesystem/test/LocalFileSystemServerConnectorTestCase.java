/*
 * Copyright (c) 2017 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.transport.localfilesystem.test;

import org.testng.Assert;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemServerConnector;
import org.wso2.carbon.transport.localfilesystem.server.connector.contractimpl.LocalFileSystemConnectorFactoryImpl;
import org.wso2.carbon.transport.localfilesystem.server.util.Constants;
import org.wso2.carbon.transport.localfilesystem.test.util.TestRemoteFileSystemListener;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Test case that tests the file server connector functionality.
 */
public class LocalFileSystemServerConnectorTestCase {

    //@Test(description = "Testing whether correctly getting the file path.")
    public void filePollingTestCase() throws ServerConnectorException, InterruptedException {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource("test");
        if (url == null) {
            Assert.fail("Unable to load the resource file.");
        }
        String file = url.getFile();
        File testFile = new File(file);
        String fileURI = testFile.getAbsolutePath();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.TRANSPORT_FILE_FILE_URI, fileURI);

        LocalFileSystemConnectorFactoryImpl connectorFactory = new LocalFileSystemConnectorFactoryImpl();
        TestRemoteFileSystemListener fileSystemListener = new TestRemoteFileSystemListener();
        LocalFileSystemServerConnector testConnector =
                connectorFactory.createServerConnector("TestService", parameters, fileSystemListener);
        testConnector.start();
        fileSystemListener.waitTillDone();
        String completeFilePath = fileURI + "/test.txt";
        Assert.assertEquals(completeFilePath, fileSystemListener.getText());
        testConnector.stop();
    }

    //@Test(description = "Testing whether correctly getting the file path with cron expression.")
    public void filePollingCronTestCase() throws ServerConnectorException, InterruptedException {

        ClassLoader classLoader = getClass().getClassLoader();
        String fileURI = new File(classLoader.getResource("fs-cron/").getFile()).getAbsolutePath();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.TRANSPORT_FILE_FILE_URI, fileURI);

        LocalFileSystemConnectorFactoryImpl connectorFactory = new LocalFileSystemConnectorFactoryImpl();
        TestRemoteFileSystemListener fileSystemListener = new TestRemoteFileSystemListener();
        LocalFileSystemServerConnector testConnector =
                connectorFactory.createServerConnector("TestService", parameters, fileSystemListener);
        testConnector.start();
        fileSystemListener.waitTillDone();
        String completeFilePath = fileURI + "/testCron.txt";
        Assert.assertEquals(completeFilePath, fileSystemListener.getText());
        testConnector.stop();
    }
}
