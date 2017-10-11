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

import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemEvent;
import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemListener;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;

/**
 * Test {@link LocalFileSystemListener} implementation for testing purpose.
 */
public class TestRemoteFileSystemListener implements LocalFileSystemListener {

    private CountDownLatch latch;
    private int eventCounter = 0;
    private int expectedEventCount;
    private LinkedList<LocalFileSystemEvent> eventQueue = new LinkedList<>();

    TestRemoteFileSystemListener(CountDownLatch latch, int expectedEventCount) {
        this.latch = latch;
        this.expectedEventCount = expectedEventCount;
    }

    @Override
    public void onMessage(LocalFileSystemEvent localFileSystemEvent) {
        eventQueue.add(localFileSystemEvent);
        if (++eventCounter >= this.expectedEventCount) {
            latch.countDown();
        }
    }

    LinkedList<LocalFileSystemEvent> getEventQueue() {
        return eventQueue;
    }
}
