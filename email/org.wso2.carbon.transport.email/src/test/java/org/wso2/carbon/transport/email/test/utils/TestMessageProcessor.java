/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.transport.email.test.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.ClientConnector;
import org.wso2.carbon.messaging.TextCarbonMessage;
import org.wso2.carbon.messaging.TransportSender;

import java.util.concurrent.CountDownLatch;

/**
 * Class implementing Test message processor.
 */
public class TestMessageProcessor implements CarbonMessageProcessor {
    private static final Logger log = LoggerFactory.getLogger(TestMessageProcessor.class);
    private CountDownLatch latch = new CountDownLatch(1);
    public String subject;

    @Override
    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback) throws Exception {

        String content = ((TextCarbonMessage) carbonMessage).getText();
        subject = carbonMessage.getHeader(EmailTestConstant.MAIL_HEADER_SUBJECT);

        if (log.isDebugEnabled()) {
            log.debug("Message received with subject '" + subject + "' content: '" + content
                    + "' from address: '" + carbonMessage.getHeader("From")
                    + "' to address: '" + carbonMessage.getHeader("To")
                    + "' bcc address: '" + carbonMessage.getHeader("Bcc")
                    + "' cc address '" +  carbonMessage.getHeader("Cc") + "'.");
        }

        if (carbonCallback != null) {
            carbonCallback.done(carbonMessage);
        }
        done();

        return false;
    }

    @Override public void setTransportSender(TransportSender transportSender) {

    }

    @Override public void setClientConnector(ClientConnector clientConnector) {

    }

    @Override public String getId() {
        return null;
    }

    /**
     * Wait till the latch is count down by 1.
     * @throws InterruptedException
     */
    public void waitTillDone() throws InterruptedException {
        latch.await();
    }

    private void done() {
        latch.countDown();
    }
}
