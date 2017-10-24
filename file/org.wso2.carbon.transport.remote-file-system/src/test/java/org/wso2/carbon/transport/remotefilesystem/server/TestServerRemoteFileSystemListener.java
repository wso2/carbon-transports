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

import org.wso2.carbon.transport.remotefilesystem.listener.RemoteFileSystemListener;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemBaseMessage;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Test {@link RemoteFileSystemListener} implementation for testing purpose.
 */
public class TestServerRemoteFileSystemListener implements RemoteFileSystemListener {

    private CountDownLatch latch;
    private int expectedEventCount;
    private Throwable throwable;
    private int eventCounter = 0;
    private List<String> eventQueue = new ArrayList<>();

    TestServerRemoteFileSystemListener(CountDownLatch latch, int expectedEventCount) {
        this.latch = latch;
        this.expectedEventCount = expectedEventCount;
    }

    @Override
    public void onMessage(RemoteFileSystemBaseMessage remoteFileSystemEvent) {
        eventQueue.add(((RemoteFileSystemEvent) remoteFileSystemEvent).getText());
        if (++eventCounter >= this.expectedEventCount) {
            latch.countDown();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        this.throwable = throwable;
        latch.countDown();
    }

    @Override
    public void done() {
        latch.countDown();
    }

    List<String> getEventList() {
        return eventQueue;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
