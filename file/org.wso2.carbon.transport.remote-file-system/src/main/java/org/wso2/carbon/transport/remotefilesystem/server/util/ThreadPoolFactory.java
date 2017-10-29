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

package org.wso2.carbon.transport.remotefilesystem.server.util;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A Thread Pool factory for creating Thread Pools required for the File System transport
 */
public class ThreadPoolFactory {
    private ExecutorService executorService;

    public ThreadPoolFactory(int threadPoolSize) {
        if (threadPoolSize == 0) {
            executorService = Executors.newSingleThreadExecutor();
        } else {
            executorService = Executors.newFixedThreadPool(threadPoolSize);
        }
    }

    /**
     * Execute given runnable target.
     *
     * @param runnable RemoteFileSystemProcessor instance.
     */
    public void execute(Runnable runnable) {
        executorService.execute(runnable);
    }

    /**
     * Stop all the executing thread and shutdown the pool.
     */
    public void stop() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}
