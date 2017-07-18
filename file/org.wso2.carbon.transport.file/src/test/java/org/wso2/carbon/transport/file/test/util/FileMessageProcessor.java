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

package org.wso2.carbon.transport.file.test.util;

import org.wso2.carbon.messaging.BinaryCarbonMessage;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.ClientConnector;
import org.wso2.carbon.messaging.TransportSender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Message processor that is used for the testing purposes.
 */
public class FileMessageProcessor implements CarbonMessageProcessor {
    private CountDownLatch latch = new CountDownLatch(1);
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<String> fileContent = new ArrayList<>();

    @Override
    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        executor.execute(new FillFileContent(latch, carbonMessage));
        latch.await();
        if (fileContent.size() == 20) {
            done();
        }
        return false;
    }

    @Override
    public void setTransportSender(TransportSender transportSender) {

    }

    @Override
    public void setClientConnector(ClientConnector clientConnector) {

    }

    @Override
    public String getId() {
        return "test-file-message-processor";
    }

    /**
     * Retrieve the line number of the file given by index
     *
     * @param index
     * @return the content of the line at index-th line of the file
     */
    public String getFileContent(int index) {
        return fileContent.get(index);
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
     * To make sure reading the file content is done.
     */
    private void done() {
        latch.countDown();
    }

    private class FillFileContent implements Runnable {
        private CountDownLatch latch;
        private CarbonMessage carbonMessage;

        public FillFileContent(CountDownLatch latch, CarbonMessage carbonMessage) {
            this.latch = latch;
            this.carbonMessage = carbonMessage;
        }

        @Override
        public void run() {
            byte[] content = ((BinaryCarbonMessage) carbonMessage).readBytes().array();
            fileContent.add(new String(content));
            latch.countDown();
        }
    }
}
