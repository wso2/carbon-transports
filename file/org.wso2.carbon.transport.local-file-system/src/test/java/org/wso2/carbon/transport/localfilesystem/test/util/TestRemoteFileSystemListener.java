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

package org.wso2.carbon.transport.localfilesystem.test.util;

import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemEvent;
import org.wso2.carbon.transport.localfilesystem.server.connector.contract.LocalFileSystemListener;

import java.util.concurrent.CountDownLatch;

/**
 * Test {@link LocalFileSystemListener} implementation for testing purpose.
 */
public class TestRemoteFileSystemListener implements LocalFileSystemListener {

    private CountDownLatch latch = new CountDownLatch(1);
    private String text;

    @Override
    public void onMessage(LocalFileSystemEvent localFileSystemEvent) {
        text = localFileSystemEvent.getFileName();
        done();
    }

    public String getText() {
        return text;
    }


    /**
     * To wait till file reading operation is finished.
     *
     * @throws InterruptedException Interrupted Exception.
     */
    public void waitTillDone() throws InterruptedException {
        latch.await();
    }

    /**
     * To make sure the reading the file content is done.
     */
    private void done() {
        latch.countDown();
    }
}
