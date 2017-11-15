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

package org.wso2.carbon.transport.email.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.email.contract.EmailMessageListener;
import org.wso2.carbon.transport.email.contract.message.EmailBaseMessage;
import org.wso2.carbon.transport.email.contract.message.EmailTextMessage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link EmailMessageListener} to test the email server connector.
 */
public class TestEmailMessageListener implements EmailMessageListener {
    private static final Logger log = LoggerFactory.getLogger(TestEmailMessageListener.class);
    private CountDownLatch latch;
    EmailTextMessage emailTextMessage;
    public String subject;
    public String bcc;
    public String to;
    public String from;
    public String cc;
    public String content;
    public int numberOfEventArrived = 0;
    public Long uid = 0L;

    /**
     * Set the number of times countDown in Latch which is equal to the expecting number of events (emails).
     *
     * @param count Number of events expecting by the Listener
     */
    public void setNumberOfEvent(int count) {
        latch = new CountDownLatch(count);
    }

    @Override
    public void onMessage(EmailBaseMessage emailTextMessage) {

        this.emailTextMessage = (EmailTextMessage) emailTextMessage;

        content = this.emailTextMessage.getText();
        subject = this.emailTextMessage.getHeader("Subject");
        bcc = this.emailTextMessage.getHeader("Bcc");
        to = this.emailTextMessage.getHeader("To");
        from = this.emailTextMessage.getHeader("From");
        cc = this.emailTextMessage.getHeader("Cc");

        if (log.isDebugEnabled()) {
            log.debug(
                    "Message received with subject '" + subject + "' content: '" + content + "' from address: '" + from
                            + "' to address: '" + to + "' bcc address: '" + bcc + "' cc address '" + cc + "'.");
        }

        emailTextMessage.sendAck();
        numberOfEventArrived++;
        done();
    }

    /**
     * Wait till the expecting number of events to come.
     *
     * @throws InterruptedException InterruptedException if error is occurred while waiting.
     */
    public void waitForEvent() throws InterruptedException {
       latch.await(600000, TimeUnit.MILLISECONDS);
    }

    /**
     * Countdown latch by 1
     */
    private void done() {
        latch.countDown();
    }
}
