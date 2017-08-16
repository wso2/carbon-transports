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

package org.wso2.carbon.transport.email.callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import java.util.concurrent.CountDownLatch;

/**
 * This {@link CarbonCallback} will be called by a message processor to acknowledge that
 * it has finished processing the mail, so that it can implement given action to
 * the mail and move to next mail to process.
 */
public class EmailServerConnectorCallback implements CarbonCallback {
    private static final Logger log = LoggerFactory.getLogger(EmailServerConnectorCallback.class);

    /**
     * Countdown latch to wait for the acknowledgement from the application layer.
     */
    private CountDownLatch latch = new CountDownLatch(1);

    /**
     * {@inheritDoc}
     */
    @Override
    public void done(CarbonMessage carbonMessage) {
        if (log.isDebugEnabled()) {
            log.debug("Message processor acknowledgement is received.");
        }
        latch.countDown();
    }

    /**
     * Waits until latch is count down by one. (It waits until 'done' is call by the message processor)
     * @throws InterruptedException
     */
    public void waitTillDone() throws InterruptedException {
        latch.await();
    }


}
