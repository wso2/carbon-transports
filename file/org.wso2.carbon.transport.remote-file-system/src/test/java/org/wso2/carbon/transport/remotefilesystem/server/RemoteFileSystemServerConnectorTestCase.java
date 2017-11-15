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

package org.wso2.carbon.transport.remotefilesystem.server;

import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.remotefilesystem.Constants;
import org.wso2.carbon.transport.remotefilesystem.RemoteFileSystemConnectorFactory;
import org.wso2.carbon.transport.remotefilesystem.exception.RemoteFileSystemConnectorException;
import org.wso2.carbon.transport.remotefilesystem.impl.RemoteFileSystemConnectorFactoryImpl;
import org.wso2.carbon.transport.remotefilesystem.server.connector.contract.RemoteFileSystemServerConnector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test case that tests the file server connector functionality.
 */
public class RemoteFileSystemServerConnectorTestCase {

    private FakeFtpServer ftpServer;
    private FileSystem fileSystem;
    private int serverPort;
    private static String username = "wso2";
    private static String password = "wso2123";
    private static String rootFolder = "/home/wso2";

    @BeforeClass
    public void init() {
        ftpServer = new FakeFtpServer();
        ftpServer.setServerControlPort(0);
        ftpServer.addUserAccount(new UserAccount(username, password, rootFolder));

        fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry(rootFolder));
        fileSystem.add(new FileEntry("/home/wso2/file1.txt", "some content 1234567890"));
        fileSystem.add(new FileEntry("/home/wso2/exe/run.exe", "Test Value"));
        ftpServer.setFileSystem(fileSystem);
        ftpServer.start();
        serverPort = ftpServer.getServerControlPort();
    }

    @Test(description = "Testing whether correctly getting the file path.")
    public void retrieveFileListTestCase() throws ServerConnectorException, InterruptedException {
        int expectedEventCount = 3;
        Map<String, String> parameters = getPropertyMap("NONE", false);
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestServerRemoteFileSystemListener fileSystemListener = new TestServerRemoteFileSystemListener(latch,
                expectedEventCount);
        RemoteFileSystemServerConnector testConnector =
                connectorFactory.createServerConnector("TestService", parameters, fileSystemListener);
        testConnector.start();
        fileSystem.add(new FileEntry("/home/wso2/file2.txt"));
        latch.await(1, TimeUnit.MINUTES);
        List<String> eventList = fileSystemListener.getEventList();
        if (eventList.size() == 0) {
            Assert.fail("File event didn't triggered.");
        }
        Assert.assertEquals(eventList.size(), expectedEventCount, "Generated events count mismatch " +
                "with the expected.");
        Assert.assertTrue(eventList.contains(buildConnectionURL() + "/exe/run.exe"));
        Assert.assertTrue(eventList.contains(buildConnectionURL() + "/file1.txt"));
        Assert.assertTrue(eventList.contains(buildConnectionURL() + "/file2.txt"));
        testConnector.stop();
    }

    @Test(description = "Testing whether correctly move files.", dependsOnMethods = "retrieveFileListTestCase")
    public void retrieveFileListAndMoveTestCase() throws ServerConnectorException, InterruptedException {
        int expectedEventCount = 2;
        fileSystem.add(new DirectoryEntry("/home/wso2/moveFolder"));
        Map<String, String> parameters = getPropertyMap("MOVE", true);
        parameters.put(Constants.MOVE_AFTER_PROCESS, buildConnectionURL() + "/moveFolder");
        parameters.put(Constants.FORCE_CREATE_FOLDER, String.valueOf(true));
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestServerRemoteFileSystemListener fileSystemListener =
                new TestServerRemoteFileSystemListener(latch, expectedEventCount);
        RemoteFileSystemServerConnector testConnector =
                connectorFactory.createServerConnector("TestService", parameters, fileSystemListener);
        testConnector.start();
        latch.await(1, TimeUnit.MINUTES);
        List<String> eventList = fileSystemListener.getEventList();
        if (eventList.size() == 0) {
            Assert.fail("File event didn't triggered.");
        }
        Assert.assertEquals(eventList.size(), expectedEventCount, "Generated events count mismatch " +
                "with the expected.");
        Assert.assertTrue(eventList.contains(buildConnectionURL() + "/exe/run.exe"));
        Assert.assertTrue(eventList.contains(buildConnectionURL() + "/file1.txt"));
        testConnector.stop();
    }

    @Test(description = "Testing whether correctly move files.", dependsOnMethods = "retrieveFileListAndMoveTestCase")
    public void retrieveFileListAndDeleteTestCase() throws ServerConnectorException, InterruptedException {
        int expectedEventCount = 2;
        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry(rootFolder));
        fileSystem.add(new FileEntry("/home/wso2/del1.txt"));
        fileSystem.add(new FileEntry("/home/wso2/del2.txt"));
        ftpServer.setFileSystem(fileSystem);

        Map<String, String> parameters = getPropertyMap("DELETE", false);
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestServerRemoteFileSystemListener fileSystemListener =
                new TestServerRemoteFileSystemListener(latch, expectedEventCount);
        RemoteFileSystemServerConnector testConnector =
                connectorFactory.createServerConnector("TestService", parameters, fileSystemListener);
        testConnector.start();
        latch.await(1, TimeUnit.MINUTES);
        List<String> eventList = fileSystemListener.getEventList();
        if (eventList.size() == 0) {
            Assert.fail("File event didn't triggered.");
        }
        Assert.assertEquals(eventList.size(), expectedEventCount, "Generated events count mismatch " +
                "with the expected.");
        Assert.assertTrue(eventList.contains(buildConnectionURL() + "/del1.txt"));
        Assert.assertTrue(eventList.contains(buildConnectionURL() + "/del2.txt"));
        testConnector.stop();
    }

    @Test(expectedExceptions = RemoteFileSystemConnectorException.class, expectedExceptionsMessageRegExp =
            "Failed to initialize File server connector for Service: TestService")
    public void invalidRootFolderTestCase() throws InterruptedException, RemoteFileSystemConnectorException {
        int expectedEventCount = 1;
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.TRANSPORT_FILE_URI,
                "ftp://" + username + ":" + password + "@localhost:" + serverPort + "/home/wso2/file1.txt");
        parameters.put(Constants.ACTION_AFTER_PROCESS, "NONE");
        parameters.put(org.wso2.carbon.connector.framework.server.polling.Constants.POLLING_INTERVAL, "2000");
        parameters.put(Constants.PARALLEL, String.valueOf(false));

        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestServerRemoteFileSystemListener fileSystemListener =
                new TestServerRemoteFileSystemListener(latch, expectedEventCount);
        try {
            connectorFactory.createServerConnector("TestService", parameters, fileSystemListener);
        } catch (RemoteFileSystemConnectorException e) {
            Assert.assertEquals(e.getCause().getMessage(),
                    "[TestService] File system server connector is used to listen to a folder. " +
                            "But the given path does not refer to a folder.");
            throw e;
        }
        latch.await(1, TimeUnit.MINUTES);
        Assert.assertNotNull(fileSystemListener.getThrowable(), "Expected exception didn't throw.");
    }

    @AfterClass
    public void cleanup() {
        if (ftpServer != null && ftpServer.isStarted()) {
            ftpServer.stop();
        }
    }

    private Map<String, String> getPropertyMap(String action, boolean parallel) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.TRANSPORT_FILE_URI, buildConnectionURL());
        parameters.put(Constants.ACTION_AFTER_PROCESS, action);
        parameters.put(org.wso2.carbon.connector.framework.server.polling.Constants.POLLING_INTERVAL, "2000");
        parameters.put(Constants.PARALLEL, String.valueOf(parallel));
        return parameters;
    }

    private String buildConnectionURL() {
        return "ftp://" + username + ":" + password + "@localhost:" + serverPort + rootFolder;
    }
}
