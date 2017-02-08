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

package org.wso2.carbon.transport.file.connector.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;

import java.util.concurrent.CountDownLatch;

/**
 *  This {@link CarbonCallback} will be called by a message processsor to acknowledge that
 *  it has finished processing the file input stream, so it may be closed from the transport end.
 */
public class FileServerConnectorCallback implements CarbonCallback {

    private static final Log log = LogFactory.getLog(FileServerConnectorCallback.class);

    CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void done(CarbonMessage carbonMessage) {
        if (log.isDebugEnabled()) {
            log.debug("Message processor acknowledgement received.");
        }
        latch.countDown();
    }

    protected void waitTillDone() throws InterruptedException {
        latch.await();
    }
}
