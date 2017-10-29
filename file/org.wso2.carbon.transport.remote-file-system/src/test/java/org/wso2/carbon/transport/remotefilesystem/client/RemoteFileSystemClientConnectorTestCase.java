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

package org.wso2.carbon.transport.remotefilesystem.client;

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
import org.wso2.carbon.transport.remotefilesystem.client.connector.contract.VFSClientConnector;
import org.wso2.carbon.transport.remotefilesystem.exception.RemoteFileSystemConnectorException;
import org.wso2.carbon.transport.remotefilesystem.impl.RemoteFileSystemConnectorFactoryImpl;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemMessage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Test case that tests the FTP client connector functionality.
 */
public class RemoteFileSystemClientConnectorTestCase {

    private FakeFtpServer ftpServer;
    private FileSystem fileSystem;
    private int serverPort;
    private static String username = "wso2";
    private static String password = "wso2123";
    private static String rootFolder = "/home/wso2";
    private static String content = "Test String";

    @BeforeClass
    public void init() {
        ftpServer = new FakeFtpServer();
        ftpServer.setServerControlPort(0);
        ftpServer.addUserAccount(new UserAccount(username, password, rootFolder));
        fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry(rootFolder));
        fileSystem.add(new FileEntry("/home/wso2/file1.txt", content));
        fileSystem.add(new FileEntry("/home/wso2/file10.txt", content));
        fileSystem.add(new FileEntry("/home/wso2/file11.txt", content));
        fileSystem.add(new FileEntry("/home/wso2/file2.txt"));
        fileSystem.add(new DirectoryEntry("/home/wso2/move"));
        fileSystem.add(new DirectoryEntry("/home/wso2/copy"));
        ftpServer.setFileSystem(fileSystem);
        ftpServer.start();
        serverPort = ftpServer.getServerControlPort();
    }

    @Test(description = "Check file exist.")
    public void checkFileExistTestCase() throws ServerConnectorException, InterruptedException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.URI, buildConnectionURL() + "/file1.txt");
        parameters.put(Constants.ACTION, Constants.EXISTS);
        parameters.put(Constants.PROTOCOL, Constants.PROTOCOL_FTP);
        parameters.put(Constants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestExistActionListener fileSystemListener = new TestExistActionListener(latch);
        VFSClientConnector clientConnector =
                connectorFactory.createVFSClientConnector(parameters, fileSystemListener);
        clientConnector.send(null);
        latch.await(3, TimeUnit.SECONDS);
        Assert.assertEquals(fileSystemListener.isFileExist(), true, "Did not identified " +
                "the existing file.");
    }

    @Test(description = "Check file content.")
    public void fileContentReadTestCase() throws ServerConnectorException, InterruptedException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.URI, buildConnectionURL() + "/file1.txt");
        parameters.put(Constants.ACTION, Constants.READ);
        parameters.put(Constants.PROTOCOL, Constants.PROTOCOL_FTP);
        parameters.put(Constants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestReadActionListener fileSystemListener = new TestReadActionListener(latch);
        VFSClientConnector clientConnector =
                connectorFactory.createVFSClientConnector(parameters, fileSystemListener);
        clientConnector.send(null);
        latch.await(3, TimeUnit.SECONDS);
        Assert.assertEquals(fileSystemListener.getContent(), content, "File content invalid.");
    }

    @Test(description = "Content read from non exist file")
    public void fileContentReadFromNonExistFileTestCase() throws ServerConnectorException, InterruptedException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.URI, buildConnectionURL() + "/non-exist.txt");
        parameters.put(Constants.ACTION, Constants.READ);
        parameters.put(Constants.PROTOCOL, Constants.PROTOCOL_FTP);
        parameters.put(Constants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestReadActionListener fileSystemListener = new TestReadActionListener(latch);
        VFSClientConnector clientConnector =
                connectorFactory.createVFSClientConnector(parameters, fileSystemListener);
        clientConnector.send(null);
        latch.await(3, TimeUnit.SECONDS);
        Assert.assertNull(fileSystemListener.getContent(), "File content invalid.");
        Assert.assertTrue(fileSystemListener.getThrowable() instanceof RemoteFileSystemConnectorException,
                "Exception did not throw as expected.");
    }

    @Test(description = "Check file content.")
    public void fileContentWriteTestCase() throws ServerConnectorException, InterruptedException {
        String newContent = "Sample text";
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.URI, buildConnectionURL() + "/file2.txt");
        parameters.put(Constants.ACTION, Constants.WRITE);
        parameters.put(Constants.PROTOCOL, Constants.PROTOCOL_FTP);
        parameters.put(Constants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestReadActionListener fileSystemListener = new TestReadActionListener(latch);
        VFSClientConnector clientConnector =
                connectorFactory.createVFSClientConnector(parameters, fileSystemListener);
        RemoteFileSystemMessage message = new RemoteFileSystemMessage(ByteBuffer.wrap(newContent.getBytes()));
        clientConnector.send(message);
        latch.await(3, TimeUnit.SECONDS);
        FileEntry entry = (FileEntry) fileSystem.getEntry("/home/wso2/file2.txt");
        InputStream inputStream = entry.createInputStream();
        String fileContent = new BufferedReader(new InputStreamReader(inputStream)).lines().
                collect(Collectors.joining("\n"));
        Assert.assertEquals(fileContent, newContent, "File content invalid.");
    }

    @Test(description = "Write content by creating new file")
    public void writeContentToNewFileTestCase() throws ServerConnectorException, InterruptedException {
        String newContent = "Sample text";
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.URI, buildConnectionURL() + "/file4.txt");
        parameters.put(Constants.ACTION, Constants.WRITE);
        parameters.put(Constants.PROTOCOL, Constants.PROTOCOL_FTP);
        parameters.put(Constants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestReadActionListener fileSystemListener = new TestReadActionListener(latch);
        VFSClientConnector clientConnector =
                connectorFactory.createVFSClientConnector(parameters, fileSystemListener);
        RemoteFileSystemMessage message = new RemoteFileSystemMessage(ByteBuffer.wrap(newContent.getBytes()));
        clientConnector.send(message);
        latch.await(3, TimeUnit.SECONDS);
        FileEntry entry = (FileEntry) fileSystem.getEntry("/home/wso2/file4.txt");
        InputStream inputStream = entry.createInputStream();
        String fileContent = new BufferedReader(new InputStreamReader(inputStream)).lines().
                collect(Collectors.joining("\n"));
        Assert.assertEquals(fileContent, newContent, "File content invalid.");
    }

    @Test(description = "Check file content append.", dependsOnMethods = "fileContentReadTestCase")
    public void fileContentAppendTestCase() throws ServerConnectorException, InterruptedException {
        String newContent = " Sample text";
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.URI, buildConnectionURL() + "/file1.txt");
        parameters.put(Constants.ACTION, Constants.WRITE);
        parameters.put(Constants.APPEND, String.valueOf(true));
        parameters.put(Constants.PROTOCOL, Constants.PROTOCOL_FTP);
        parameters.put(Constants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestReadActionListener fileSystemListener = new TestReadActionListener(latch);
        VFSClientConnector clientConnector =
                connectorFactory.createVFSClientConnector(parameters, fileSystemListener);
        RemoteFileSystemMessage message = new RemoteFileSystemMessage(ByteBuffer.wrap(newContent.getBytes()));
        clientConnector.send(message);
        latch.await(3, TimeUnit.SECONDS);
        FileEntry entry = (FileEntry) fileSystem.getEntry("/home/wso2/file1.txt");
        InputStream inputStream = entry.createInputStream();
        String fileContent = new BufferedReader(new InputStreamReader(inputStream)).lines().
                collect(Collectors.joining("\n"));
        Assert.assertEquals(fileContent, content + newContent, "File content invalid.");
    }

    @Test(description = "Create new file.")
    public void fileCreateTestCase() throws ServerConnectorException, InterruptedException {
        String filePath = "/file3.txt";
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.URI, buildConnectionURL() + filePath);
        parameters.put(Constants.ACTION, Constants.CREATE);
        parameters.put(Constants.CREATE_FOLDER, String.valueOf(false));
        parameters.put(Constants.PROTOCOL, Constants.PROTOCOL_FTP);
        parameters.put(Constants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestClientRemoteFileSystemListener fileSystemListener = new TestClientRemoteFileSystemListener(latch);
        VFSClientConnector clientConnector =
                connectorFactory.createVFSClientConnector(parameters, fileSystemListener);
        clientConnector.send(null);
        latch.await(3, TimeUnit.SECONDS);
        Assert.assertTrue(fileSystem.exists(rootFolder + filePath), "File not created.");
    }

    @Test(description = "Trying to create file that already exists.", dependsOnMethods = "fileCreateTestCase")
    public void existingFileCreateTestCase() throws ServerConnectorException, InterruptedException {
        String filePath = "/file3.txt";
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.URI, buildConnectionURL() + filePath);
        parameters.put(Constants.ACTION, Constants.CREATE);
        parameters.put(Constants.CREATE_FOLDER, String.valueOf(false));
        parameters.put(Constants.PROTOCOL, Constants.PROTOCOL_FTP);
        parameters.put(Constants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestClientRemoteFileSystemListener fileSystemListener = new TestClientRemoteFileSystemListener(latch);
        VFSClientConnector clientConnector =
                connectorFactory.createVFSClientConnector(parameters, fileSystemListener);
        clientConnector.send(null);
        latch.await(3, TimeUnit.SECONDS);
        Assert.assertTrue(fileSystemListener.getThrowable() instanceof RemoteFileSystemConnectorException,
                "Exception did not throw as expected.");
    }

    @Test(description = "Create new folder.")
    public void folderCreateTestCase() throws ServerConnectorException, InterruptedException {
        String filePath = "/folder";
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.URI, buildConnectionURL() + filePath);
        parameters.put(Constants.ACTION, Constants.CREATE);
        parameters.put(Constants.CREATE_FOLDER, String.valueOf(true));
        parameters.put(Constants.PROTOCOL, Constants.PROTOCOL_FTP);
        parameters.put(Constants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestClientRemoteFileSystemListener fileSystemListener = new TestClientRemoteFileSystemListener(latch);
        VFSClientConnector clientConnector =
                connectorFactory.createVFSClientConnector(parameters, fileSystemListener);
        clientConnector.send(null);
        latch.await(3, TimeUnit.SECONDS);
        Assert.assertTrue(fileSystem.exists(rootFolder + filePath), "File not created.");
    }

    @Test(description = "Delete file.", dependsOnMethods = "fileCreateTestCase")
    public void fileDeleteTestCase() throws ServerConnectorException, InterruptedException {
        String filePath = "/file3.txt";
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.URI, buildConnectionURL() + filePath);
        parameters.put(Constants.ACTION, Constants.DELETE);
        parameters.put(Constants.PROTOCOL, Constants.PROTOCOL_FTP);
        parameters.put(Constants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestClientRemoteFileSystemListener fileSystemListener = new TestClientRemoteFileSystemListener(latch);
        VFSClientConnector clientConnector =
                connectorFactory.createVFSClientConnector(parameters, fileSystemListener);
        clientConnector.send(null);
        latch.await(3, TimeUnit.SECONDS);
        Assert.assertFalse(fileSystem.exists(rootFolder + filePath), "File not deleted.");
    }

    @Test(description = "Delete non exist file.", dependsOnMethods = "fileCreateTestCase")
    public void deleteNonExistTestCase() throws ServerConnectorException, InterruptedException {
        String filePath = "/non-exit.txt";
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.URI, buildConnectionURL() + filePath);
        parameters.put(Constants.ACTION, Constants.DELETE);
        parameters.put(Constants.PROTOCOL, Constants.PROTOCOL_FTP);
        parameters.put(Constants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestClientRemoteFileSystemListener fileSystemListener = new TestClientRemoteFileSystemListener(latch);
        VFSClientConnector clientConnector =
                connectorFactory.createVFSClientConnector(parameters, fileSystemListener);
        clientConnector.send(null);
        latch.await(3, TimeUnit.SECONDS);
        Assert.assertTrue(fileSystemListener.getThrowable() instanceof RemoteFileSystemConnectorException,
                "Exception did not throw as expected.");
    }

    @Test(description = "Copy file.", dependsOnMethods = "fileContentWriteTestCase")
    public void fileCopyTestCase() throws ServerConnectorException, InterruptedException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.ACTION, Constants.COPY);
        parameters.put(Constants.URI, buildConnectionURL() + "/file2.txt");
        parameters.put(Constants.DESTINATION, buildConnectionURL() + "/copy/file2-copy.txt");
        parameters.put(Constants.PROTOCOL, Constants.PROTOCOL_FTP);
        parameters.put(Constants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestClientRemoteFileSystemListener fileSystemListener = new TestClientRemoteFileSystemListener(latch);
        VFSClientConnector clientConnector =
                connectorFactory.createVFSClientConnector(parameters, fileSystemListener);
        clientConnector.send(null);
        latch.await(3, TimeUnit.SECONDS);
        Assert.assertTrue(fileSystem.exists(rootFolder + "/copy/file2-copy.txt"), "File not copied.");
    }

    @Test(description = "Copy non-exist file.", dependsOnMethods = "fileContentWriteTestCase")
    public void copyNonExistFileTestCase() throws ServerConnectorException, InterruptedException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.ACTION, Constants.COPY);
        parameters.put(Constants.URI, buildConnectionURL() + "/non-exist.txt");
        parameters.put(Constants.DESTINATION, buildConnectionURL() + "/copy/file2-non-copy.txt");
        parameters.put(Constants.PROTOCOL, Constants.PROTOCOL_FTP);
        parameters.put(Constants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestClientRemoteFileSystemListener fileSystemListener = new TestClientRemoteFileSystemListener(latch);
        VFSClientConnector clientConnector =
                connectorFactory.createVFSClientConnector(parameters, fileSystemListener);
        clientConnector.send(null);
        latch.await(3, TimeUnit.SECONDS);
        Assert.assertTrue(fileSystemListener.getThrowable() instanceof RemoteFileSystemConnectorException,
                "Exception did not throw as expected.");
        Assert.assertFalse(fileSystem.exists(rootFolder + "/copy/file2-non-copy.txt"),
                "Non exist file copied.");
    }

    @Test(description = "File move.", dependsOnMethods = "fileCopyTestCase")
    public void fileMoveTestCase() throws ServerConnectorException, InterruptedException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.ACTION, Constants.MOVE);
        parameters.put(Constants.URI, buildConnectionURL() + "/file2.txt");
        parameters.put(Constants.DESTINATION, buildConnectionURL() + "/move/file2-move.txt");
        parameters.put(Constants.PROTOCOL, Constants.PROTOCOL_FTP);
        parameters.put(Constants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestClientRemoteFileSystemListener fileSystemListener = new TestClientRemoteFileSystemListener(latch);
        VFSClientConnector clientConnector =
                connectorFactory.createVFSClientConnector(parameters, fileSystemListener);
        clientConnector.send(null);
        latch.await(3, TimeUnit.SECONDS);
        Assert.assertTrue(fileSystem.exists(rootFolder + "/move/file2-move.txt"), "File not moved.");
        Assert.assertFalse(fileSystem.exists(rootFolder + "/file2.txt"), "File not moved.");
    }

    @Test(description = "File move with creating parent folder.", dependsOnMethods = "fileCopyTestCase")
    public void fileMoveWithCreateParentFolderTestCase() throws ServerConnectorException, InterruptedException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.ACTION, Constants.MOVE);
        parameters.put(Constants.URI, buildConnectionURL() + "/file10.txt");
        parameters.put(Constants.DESTINATION, buildConnectionURL() + "/newMoveFolder/file2-move.txt");
        parameters.put(Constants.PROTOCOL, Constants.PROTOCOL_FTP);
        parameters.put(Constants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestClientRemoteFileSystemListener fileSystemListener = new TestClientRemoteFileSystemListener(latch);
        VFSClientConnector clientConnector =
                connectorFactory.createVFSClientConnector(parameters, fileSystemListener);
        clientConnector.send(null);
        latch.await(3, TimeUnit.SECONDS);
        Assert.assertTrue(fileSystem.exists(rootFolder + "/newMoveFolder/file2-move.txt"),
                "File not moved.");
        Assert.assertFalse(fileSystem.exists(rootFolder + "/file10.txt"), "File not moved.");
    }

    @Test(description = "File move to already existing folder.", dependsOnMethods = "fileMoveTestCase")
    public void fileMoveAlreadyExistingFolderTestCase() throws ServerConnectorException, InterruptedException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.ACTION, Constants.MOVE);
        parameters.put(Constants.URI, buildConnectionURL() + "/file11.txt");
        parameters.put(Constants.DESTINATION, buildConnectionURL() + "/move/file2-move.txt");
        parameters.put(Constants.PROTOCOL, Constants.PROTOCOL_FTP);
        parameters.put(Constants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestClientRemoteFileSystemListener fileSystemListener = new TestClientRemoteFileSystemListener(latch);
        VFSClientConnector clientConnector =
                connectorFactory.createVFSClientConnector(parameters, fileSystemListener);
        clientConnector.send(null);
        latch.await(3, TimeUnit.SECONDS);
        Assert.assertTrue(fileSystem.exists(rootFolder + "/file11.txt"), "File moved.");
        Assert.assertTrue(fileSystemListener.getThrowable() instanceof RemoteFileSystemConnectorException,
                "Exception did not throw as expected.");
    }

    @Test(description = "Trying to move non existing file", dependsOnMethods = "fileMoveTestCase")
    public void moveNonExistingFileTestCase() throws ServerConnectorException, InterruptedException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.ACTION, Constants.MOVE);
        parameters.put(Constants.URI, buildConnectionURL() + "/non-exist.txt");
        parameters.put(Constants.DESTINATION, buildConnectionURL() + "/move/file7-move.txt");
        parameters.put(Constants.PROTOCOL, Constants.PROTOCOL_FTP);
        parameters.put(Constants.FTP_PASSIVE_MODE, Boolean.TRUE.toString());
        CountDownLatch latch = new CountDownLatch(1);
        RemoteFileSystemConnectorFactory connectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        TestClientRemoteFileSystemListener fileSystemListener = new TestClientRemoteFileSystemListener(latch);
        VFSClientConnector clientConnector =
                connectorFactory.createVFSClientConnector(parameters, fileSystemListener);
        clientConnector.send(null);
        latch.await(3, TimeUnit.SECONDS);
        Assert.assertFalse(fileSystem.exists(rootFolder + "/move/file7-move.txt"), "File moved.");
        Assert.assertTrue(fileSystemListener.getThrowable() instanceof RemoteFileSystemConnectorException,
                "Exception did not throw as expected.");
    }

    @AfterClass
    public void cleanup() {
        if (ftpServer != null && ftpServer.isStarted()) {
            ftpServer.stop();
        }
    }

    private String buildConnectionURL() {
        return "ftp://" + username + ":" + password + "@localhost:" + serverPort + rootFolder;
    }
}
