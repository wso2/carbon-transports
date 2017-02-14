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

import org.testng.Assert;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.ClientConnector;
import org.wso2.carbon.messaging.TransportSender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public class TestMessageProcessor implements CarbonMessageProcessor {

    CountDownLatch latch = new CountDownLatch(1);

    @Override
    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback) throws Exception {
        Assert.assertEquals("dilini", getStringFromInputStream(carbonMessage.getInputStream()));
        done();
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

    public void waitTillDone() throws InterruptedException {
        latch.await();
    }

    public void done() {
        latch.countDown();
    }

    private static String getStringFromInputStream(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder(4096);
        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(reader);
        try {
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                sb.append(str);
            }
        } catch (IOException ioe) {
            throw new Exception(ioe.getMessage(), ioe);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // Do nothing.
            }
            try {
                reader.close();
            } catch (IOException e) {
                // Do nothing.
            }
            try {
                bufferedReader.close();
            } catch (IOException e) {
                // Do nothing.
            }
        }
        return sb.toString();
    }
}
