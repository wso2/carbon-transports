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

package org.wso2.carbon.transport.localfilesystem.server.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemMessage;
import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemServerConnectorFuture;
import org.wso2.carbon.transport.localfilesystem.server.connector.exception.LocalFileSystemServerConnectorException;
import org.wso2.carbon.transport.localfilesystem.server.connector.util.Constants;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * {@link DirectoryListener} instance will listen to given directory and notify the files updates.
 */
public class DirectoryListener implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(DirectoryListener.class);

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private final boolean recursive;
    private final String serviceName;
    // TODO: remove boolean to control loop
    private boolean continueWatch = true;
    private LocalFileSystemServerConnectorFuture connectorFuture;
    private ExecutorService executorService;
    private final WatchEvent.Kind[] registeredEvents;

    public DirectoryListener(String id, Map<String, String> properties,
                             LocalFileSystemServerConnectorFuture connectorFuture)
            throws LocalFileSystemServerConnectorException {

        this.serviceName = id;
        this.connectorFuture = connectorFuture;
        String path = properties.get(Constants.TRANSPORT_FILE_FILE_URI);
        if (path == null || path.isEmpty()) {
            throw new LocalFileSystemServerConnectorException("Directory path[dirURI] property empty or " +
                    "not available for service: " + serviceName);
        }
        String eventProperty = properties.get(Constants.DIRECTORY_WATCH_EVENTS);
        if (eventProperty == null || eventProperty.isEmpty()) {
            throw new LocalFileSystemServerConnectorException("Listener events are not specified in 'events' property");
        }
        this.registeredEvents = getEventArray(eventProperty);
        try {
            this.watcher = FileSystems.getDefault().newWatchService();
            this.keys = new HashMap<>();
            this.recursive = Boolean.parseBoolean(properties.get(Constants.DIRECTORY_WATCH_RECURSIVE));
            Path dir = Paths.get(path);
            if (recursive) {
                registerAll(dir);
            } else {
                register(dir);
            }
        } catch (IOException e) {
            throw new LocalFileSystemServerConnectorException("Unable to get a watch directory " +
                    "for service: " + serviceName, e);
        }
    }

    /**
     * Start listen to the given directory for file changes. This will start single threaded {@link ExecutorService}
     * for the watch.
     */
    public void start() {
        this.executorService = Executors.newSingleThreadExecutor();
        this.executorService.execute(this);
        if (log.isDebugEnabled()) {
            log.debug("Successfully start directory listen for service: " + serviceName);
        }
    }

    /**
     * This method will directory listen and shutdown the {@link ExecutorService}.
     *
     * @throws LocalFileSystemServerConnectorException if unable to stop and cleanup all the resources properly.
     */
    public void stop() throws LocalFileSystemServerConnectorException {
        this.continueWatch = false;
        try {
            this.watcher.close();
            this.executorService.shutdown();
            try {
                if (!this.executorService.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    this.executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                this.executorService.shutdownNow();
            }
        } catch (IOException e) {
            throw new LocalFileSystemServerConnectorException("Unable to stop watching for service: " + serviceName, e);
        }
        if (log.isDebugEnabled()) {
            log.debug("Successfully stop directory listen for service: " + serviceName);
        }
    }

    @Override
    public void run() {
        startWatch();
    }

    private void registerAll(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attr)
                    throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void register(final Path dir) throws IOException {
        WatchKey key = dir.register(this.watcher, this.registeredEvents);
        this.keys.put(key, dir);
    }

    private WatchEvent.Kind[] getEventArray(String eventProperty) {
        List<WatchEvent.Kind> events = new ArrayList<>(3);
        String[] eventArray = eventProperty.split("\\s*,\\s*");
        for (String event : eventArray) {
            switch (event.toLowerCase(Locale.getDefault())) {
                case "create":
                    events.add(ENTRY_CREATE);
                    break;
                case "delete":
                    events.add(ENTRY_DELETE);
                    break;
                case "modify":
                    events.add(ENTRY_MODIFY);
                    break;
                default:
                    log.warn("Unidentified event type try to register: " + event);
            }
        }
        return events.toArray(new WatchEvent.Kind[events.size()]);
    }

    private void startWatch() {
        while (this.continueWatch) {
            WatchKey key;
            try {
                // wait for key to be signalled
                key = this.watcher.take();
            } catch (InterruptedException x) {
                return;
            }
            Path dir = this.keys.get(key);
            if (dir == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Received [" + key + "] for unknown directory.");
                }
                continue;
            }
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();
                // TODO: how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    if (log.isDebugEnabled()) {
                        log.debug("OVERFLOW event received for service: " + serviceName);
                    }
                    continue;
                }
                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path child = dir.resolve(ev.context());
                notifyToListener(event, child);
                // if directory is created, and watching recursively, then register it and its sub-directories
                if (this.recursive && (kind == ENTRY_CREATE)) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
                    } catch (IOException e) {
                        log.warn("Unable listen to the newly created directory: " + child +
                                " in service " + this.serviceName, e);
                    }
                }
            }
            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);
            }
        }
    }

    private void notifyToListener(WatchEvent<?> event, Path child) {
        LocalFileSystemMessage message = new LocalFileSystemMessage(child.toString(), event.kind().name());
        message.setProperty(Constants.FILE_TRANSPORT_PROPERTY_SERVICE_NAME, this.serviceName);
        this.connectorFuture.notifyFileSystemListener(message);
    }

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }
}
