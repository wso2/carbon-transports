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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemConnectorFactory;
import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemEvent;
import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemServerConnector;
import org.wso2.carbon.transport.localfilesystem.server.connector.contractimpl.LocalFileSystemConnectorFactoryImpl;
import org.wso2.carbon.transport.localfilesystem.server.exception.LocalFileSystemServerConnectorException;
import org.wso2.carbon.transport.localfilesystem.server.util.Constants;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertTrue;

/**
 * Test case that tests the file server connector functionality.
 */
public class LocalFileSystemServerConnectorTestCase {

    private File rootDirectory;

    @BeforeClass
    public void init() {
        try {
            Path rootListenFolderPath = Files.createTempDirectory(Paths.get("target"), null);
            rootDirectory = rootListenFolderPath.toFile();
            rootDirectory.deleteOnExit();
        } catch (IOException e) {
            Assert.fail("Unable to create root folder to setup watch.", e);
        }
    }

    @AfterClass
    public void cleanup() {
        if (rootDirectory != null) {
            try {
                Files.walk(rootDirectory.toPath(), FileVisitOption.FOLLOW_LINKS)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException ignore) {
                //Ignore
            }
        }
    }

    @Test(description = "Listen for single file actions.")
    public void listenSingleFileTestCase() throws LocalFileSystemServerConnectorException {
        Map<String, String> parameters = getPropertyMap("create, delete, modify", false);
        int eventCount = 3;
        CountDownLatch latch = new CountDownLatch(1);
        LocalFileSystemConnectorFactory connectorFactory = new LocalFileSystemConnectorFactoryImpl();
        TestRemoteFileSystemListener fileSystemListener = new TestRemoteFileSystemListener(latch, eventCount);
        LocalFileSystemServerConnector testConnector =
                connectorFactory.createServerConnector("TestService", parameters, fileSystemListener);
        testConnector.start();
        try {
            Path tempFile1 = createFile(rootDirectory.getAbsolutePath(), "temp1.txt");
            tempFile1.toFile().setLastModified(System.currentTimeMillis());
            Files.delete(tempFile1);
            latch.await(3, TimeUnit.SECONDS);
            LinkedList<LocalFileSystemEvent> eventQueue = fileSystemListener.getEventQueue();
            if (eventQueue.size() == 0) {
                Assert.fail("File create event didn't triggered.");
            }
            Assert.assertEquals(eventQueue.size(), eventCount, "Generated events count mismatch " +
                    "with the expected.");
            LocalFileSystemEvent firstEvent = eventQueue.pop();
            Assert.assertEquals(firstEvent.getFileName(),
                    rootDirectory.getAbsolutePath() + File.separator + "temp1.txt");
            Assert.assertEquals(firstEvent.getEvent(), Constants.EVENT_CREATE);
            LocalFileSystemEvent secondEvent = eventQueue.pop();
            Assert.assertEquals(secondEvent.getFileName(),
                    rootDirectory.getAbsolutePath() + File.separator + "temp1.txt");
            Assert.assertEquals(secondEvent.getEvent(), Constants.EVENT_MODIFY);
            LocalFileSystemEvent thirdEvent = eventQueue.pop();
            Assert.assertEquals(thirdEvent.getFileName(),
                    rootDirectory.getAbsolutePath() + File.separator + "temp1.txt");
            Assert.assertEquals(thirdEvent.getEvent(), Constants.EVENT_DELETE);
        } catch (IOException e) {
            Assert.fail("Unable to create new files in given location.", e);
        } catch (InterruptedException ignore) {
            // Ignore
        } finally {
            testConnector.stop();
        }
    }

    @Test(description = "Listen only for crate files actions.", dependsOnMethods = "listenSingleFileTestCase")
    public void onlyCreateFilesTestCase() throws LocalFileSystemServerConnectorException {
        Map<String, String> parameters = getPropertyMap("create", false);
        int eventCount = 3;
        CountDownLatch latch = new CountDownLatch(1);
        LocalFileSystemConnectorFactory connectorFactory = new LocalFileSystemConnectorFactoryImpl();
        TestRemoteFileSystemListener fileSystemListener = new TestRemoteFileSystemListener(latch, eventCount);
        LocalFileSystemServerConnector testConnector =
                connectorFactory.createServerConnector("TestService", parameters, fileSystemListener);
        testConnector.start();
        try {
            createFile(rootDirectory.getAbsolutePath(), "temp1.txt");
            createFile(rootDirectory.getAbsolutePath(), "temp2.txt");
            createFile(rootDirectory.getAbsolutePath(), "temp3.txt");
            latch.await(3, TimeUnit.SECONDS);
            LinkedList<LocalFileSystemEvent> eventQueue = fileSystemListener.getEventQueue();
            if (eventQueue.size() == 0) {
                Assert.fail("File create event didn't triggered.");
            }
            Assert.assertEquals(eventQueue.size(), eventCount, "Generated events count mismatch " +
                    "with the expected.");
            LocalFileSystemEvent firstEvent = eventQueue.pop();
            Assert.assertEquals(firstEvent.getFileName(),
                    rootDirectory.getAbsolutePath() + File.separator + "temp1.txt");
            Assert.assertEquals(firstEvent.getEvent(), Constants.EVENT_CREATE);
            LocalFileSystemEvent secondEvent = eventQueue.pop();
            Assert.assertEquals(secondEvent.getFileName(),
                    rootDirectory.getAbsolutePath() + File.separator + "temp2.txt");
            Assert.assertEquals(secondEvent.getEvent(), Constants.EVENT_CREATE);
            LocalFileSystemEvent thirdEvent = eventQueue.pop();
            Assert.assertEquals(thirdEvent.getFileName(),
                    rootDirectory.getAbsolutePath() + File.separator + "temp3.txt");
            Assert.assertEquals(thirdEvent.getEvent(), Constants.EVENT_CREATE);
        } catch (IOException e) {
            Assert.fail("Unable to create new files in given location.", e);
        } catch (InterruptedException ignore) {
            // Ignore
        } finally {
            testConnector.stop();
        }
    }

    @Test(description = "Listen only for modify files actions.", dependsOnMethods = "onlyCreateFilesTestCase")
    public void onlyModifyFilesTestCase() throws LocalFileSystemServerConnectorException {
        Map<String, String> parameters = getPropertyMap("modify", false);
        int eventCount = 2;
        CountDownLatch latch = new CountDownLatch(1);
        LocalFileSystemConnectorFactory connectorFactory = new LocalFileSystemConnectorFactoryImpl();
        TestRemoteFileSystemListener fileSystemListener = new TestRemoteFileSystemListener(latch, eventCount);
        LocalFileSystemServerConnector testConnector =
                connectorFactory.createServerConnector("TestService", parameters, fileSystemListener);
        testConnector.start();
        try {
            Path tempFile1 = Paths.get(rootDirectory.getAbsolutePath(), "temp1.txt");
            Path tempFile2 = Paths.get(rootDirectory.getAbsolutePath(), "temp2.txt");
            tempFile1.toFile().setLastModified(System.currentTimeMillis());
            tempFile2.toFile().setLastModified(System.currentTimeMillis());
            latch.await(3, TimeUnit.SECONDS);
            LinkedList<LocalFileSystemEvent> eventQueue = fileSystemListener.getEventQueue();
            if (eventQueue.size() == 0) {
                Assert.fail("File create event didn't triggered.");
            }
            Assert.assertEquals(eventQueue.size(), eventCount, "Generated events count mismatch " +
                    "with the expected.");
            LocalFileSystemEvent firstEvent = eventQueue.pop();
            Assert.assertEquals(firstEvent.getFileName(),
                    rootDirectory.getAbsolutePath() + File.separator + "temp1.txt");
            Assert.assertEquals(firstEvent.getEvent(), Constants.EVENT_MODIFY);
            LocalFileSystemEvent secondEvent = eventQueue.pop();
            Assert.assertEquals(secondEvent.getFileName(),
                    rootDirectory.getAbsolutePath() + File.separator + "temp2.txt");
            Assert.assertEquals(secondEvent.getEvent(), Constants.EVENT_MODIFY);
        } catch (InterruptedException ignore) {
            // Ignore
        } finally {
            testConnector.stop();
        }
    }

    @Test(description = "Listen only for delete files actions.", dependsOnMethods = "onlyModifyFilesTestCase")
    public void onlyDeleteFilesTestCase() throws LocalFileSystemServerConnectorException {
        Map<String, String> parameters = getPropertyMap("delete", false);
        int eventCount = 3;
        CountDownLatch latch = new CountDownLatch(1);
        LocalFileSystemConnectorFactory connectorFactory = new LocalFileSystemConnectorFactoryImpl();
        TestRemoteFileSystemListener fileSystemListener = new TestRemoteFileSystemListener(latch, eventCount);
        LocalFileSystemServerConnector testConnector =
                connectorFactory.createServerConnector("TestService", parameters, fileSystemListener);
        testConnector.start();
        try {
            Files.deleteIfExists(Paths.get(rootDirectory.getAbsolutePath(), "temp1.txt"));
            Files.deleteIfExists(Paths.get(rootDirectory.getAbsolutePath(), "temp2.txt"));
            Files.deleteIfExists(Paths.get(rootDirectory.getAbsolutePath(), "temp3.txt"));
            latch.await(3, TimeUnit.SECONDS);
            LinkedList<LocalFileSystemEvent> eventQueue = fileSystemListener.getEventQueue();
            if (eventQueue.size() == 0) {
                Assert.fail("File create event didn't triggered.");
            }
            Assert.assertEquals(eventQueue.size(), eventCount, "Generated events count mismatch " +
                    "with the expected.");
            LocalFileSystemEvent firstEvent = eventQueue.pop();
            Assert.assertEquals(firstEvent.getFileName(),
                    rootDirectory.getAbsolutePath() + File.separator + "temp1.txt");
            Assert.assertEquals(firstEvent.getEvent(), Constants.EVENT_DELETE);
            LocalFileSystemEvent secondEvent = eventQueue.pop();
            Assert.assertEquals(secondEvent.getFileName(),
                    rootDirectory.getAbsolutePath() + File.separator + "temp2.txt");
            Assert.assertEquals(secondEvent.getEvent(), Constants.EVENT_DELETE);
            LocalFileSystemEvent thirdEvent = eventQueue.pop();
            Assert.assertEquals(thirdEvent.getFileName(),
                    rootDirectory.getAbsolutePath() + File.separator + "temp3.txt");
            Assert.assertEquals(thirdEvent.getEvent(), Constants.EVENT_DELETE);
        } catch (InterruptedException ignore) {
            // Ignore
        } catch (IOException e) {
            Assert.fail("Unable to delete files in given location.", e);
        } finally {
            testConnector.stop();
        }
    }

    @Test(description = "Listen to single directory with recursive create actions.")
    public void recursiveDirectoryTestCase() throws LocalFileSystemServerConnectorException {
        Map<String, String> parameters = getPropertyMap("create, modify, delete", true);
        int eventCount = 4;
        CountDownLatch latch = new CountDownLatch(1);
        LocalFileSystemConnectorFactory connectorFactory = new LocalFileSystemConnectorFactoryImpl();
        TestRemoteFileSystemListener fileSystemListener = new TestRemoteFileSystemListener(latch, eventCount);
        LocalFileSystemServerConnector testConnector =
                connectorFactory.createServerConnector("TestService", parameters, fileSystemListener);
        testConnector.start();
        try {
            Path newDirectory = createDirectory("NewDirectory");
            Thread.sleep(100);
            Path newFile1 = createFile(newDirectory.toAbsolutePath().toString(), "newFile1.txt");
            newFile1.toFile().setLastModified(System.currentTimeMillis());
            Files.deleteIfExists(newFile1);
            latch.await(3, TimeUnit.SECONDS);
            LinkedList<LocalFileSystemEvent> eventQueue = fileSystemListener.getEventQueue();
            if (eventQueue.size() == 0) {
                Assert.fail("File create event didn't triggered.");
            }
            Assert.assertEquals(eventQueue.size(), eventCount, "Generated events count mismatch " +
                    "with the expected.");
            LocalFileSystemEvent firstEvent = eventQueue.pop();
            Assert.assertEquals(firstEvent.getFileName(), newDirectory.toAbsolutePath().toString());
            Assert.assertEquals(firstEvent.getEvent(), Constants.EVENT_CREATE);
            LocalFileSystemEvent secondEvent = eventQueue.pop();
            Assert.assertEquals(secondEvent.getFileName(),
                    newDirectory.toAbsolutePath() + File.separator + "newFile1.txt");
            Assert.assertEquals(secondEvent.getEvent(), Constants.EVENT_CREATE);
            LocalFileSystemEvent thirdEvent = eventQueue.pop();
            Assert.assertEquals(thirdEvent.getFileName(),
                    newDirectory.toAbsolutePath() + File.separator + "newFile1.txt");
            Assert.assertEquals(thirdEvent.getEvent(), Constants.EVENT_MODIFY);
            LocalFileSystemEvent fourthEvent = eventQueue.pop();
            Assert.assertEquals(fourthEvent.getFileName(),
                    newDirectory.toAbsolutePath() + File.separator + "newFile1.txt");
            Assert.assertEquals(fourthEvent.getEvent(), Constants.EVENT_DELETE);
        } catch (InterruptedException ignore) {
            // Ignore
        } catch (IOException e) {
            Assert.fail("Unable to create/delete files in given location.", e);
        } finally {
            testConnector.stop();
        }
    }

    @Test(expectedExceptions = LocalFileSystemServerConnectorException.class)
    public void dirURIPropertyCheckTestCase() throws LocalFileSystemServerConnectorException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.DIRECTORY_WATCH_EVENTS, "create");
        parameters.put(Constants.DIRECTORY_WATCH_RECURSIVE, String.valueOf(true));
        LocalFileSystemConnectorFactory connectorFactory = new LocalFileSystemConnectorFactoryImpl();
        TestRemoteFileSystemListener fileSystemListener = new TestRemoteFileSystemListener(null, 1);
        LocalFileSystemServerConnector testConnector =
                connectorFactory.createServerConnector("TestService", parameters, fileSystemListener);
    }

    @Test(expectedExceptions = LocalFileSystemServerConnectorException.class)
    public void emptyWatchEventPropertyCheckTestCase() throws LocalFileSystemServerConnectorException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.TRANSPORT_FILE_FILE_URI, rootDirectory.getAbsolutePath());
        LocalFileSystemConnectorFactory connectorFactory = new LocalFileSystemConnectorFactoryImpl();
        TestRemoteFileSystemListener fileSystemListener = new TestRemoteFileSystemListener(null, 1);
        LocalFileSystemServerConnector testConnector =
                connectorFactory.createServerConnector("TestService", parameters, fileSystemListener);
    }

    @Test(expectedExceptions = LocalFileSystemServerConnectorException.class)
    public void invalidWatchEventPropertyCheckTestCase() throws LocalFileSystemServerConnectorException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.TRANSPORT_FILE_FILE_URI, rootDirectory.getAbsolutePath());
        parameters.put(Constants.DIRECTORY_WATCH_EVENTS, "create,delete,invalid");
        LocalFileSystemConnectorFactory connectorFactory = new LocalFileSystemConnectorFactoryImpl();
        TestRemoteFileSystemListener fileSystemListener = new TestRemoteFileSystemListener(null, 1);
        LocalFileSystemServerConnector testConnector =
                connectorFactory.createServerConnector("TestService", parameters, fileSystemListener);
    }

    @Test(expectedExceptions = LocalFileSystemServerConnectorException.class)
    public void invalidPathCheckTestCase() throws LocalFileSystemServerConnectorException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.TRANSPORT_FILE_FILE_URI, "/invalid/path");
        parameters.put(Constants.DIRECTORY_WATCH_EVENTS, "create");
        parameters.put(Constants.DIRECTORY_WATCH_RECURSIVE, String.valueOf(true));
        LocalFileSystemConnectorFactory connectorFactory = new LocalFileSystemConnectorFactoryImpl();
        TestRemoteFileSystemListener fileSystemListener = new TestRemoteFileSystemListener(null, 1);
        LocalFileSystemServerConnector testConnector =
                connectorFactory.createServerConnector("TestService", parameters, fileSystemListener);
    }

    @Test
    public void constructorIsPrivateTestCase() throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<Constants> constructor = Constants.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    private Path createFile(String directory, String fileName) throws IOException {
        Path file = Paths.get(directory, fileName);
        Files.createFile(file);
        return file;
    }

    private Path createDirectory(String directoryName) throws IOException {
        Path directory = Paths.get(rootDirectory.getAbsolutePath(), directoryName);
        Files.createDirectory(directory);
        return directory;
    }

    private Map<String, String> getPropertyMap(String watchEvents, boolean recursive) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.TRANSPORT_FILE_FILE_URI, rootDirectory.getAbsolutePath());
        parameters.put(Constants.DIRECTORY_WATCH_EVENTS, watchEvents);
        parameters.put(Constants.DIRECTORY_WATCH_RECURSIVE, String.valueOf(recursive));
        return parameters;
    }
}
