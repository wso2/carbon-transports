/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.connector.framework.server.polling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The {@link PollingTaskRunner} which executes a periodic poll.
 */
public class PollingTaskRunner implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(PollingTaskRunner.class);

    private final long interval;
    private final PollingServerConnector connector;
    private ScheduledFuture future;

    public PollingTaskRunner(PollingServerConnector connector) {
        this.connector = connector;
        this.interval = connector.interval;
    }

    public void start() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        future = executor.scheduleAtFixedRate
                (this, interval, interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run() {
        // Run the poll cycles
        log.debug("Executing the polling task for server connector ID: " + connector.getId());
        try {
            connector.poll();
        } catch (Exception e) {
            log.error("Error executing the polling cycle for " +
                    "server connector ID: " + connector.getId(), e);
        }
        log.debug("Exit the polling task running loop for server connector ID: " + connector.getId());
    }

    /**
     * Exit the running while loop and terminate the thread.
     */
    protected void terminate() {
        if (future != null) {
            future.cancel(true);
        }
    }
}
