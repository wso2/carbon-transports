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

package org.wso2.carbon.connector.framework.server.polling;

/**
 * Constants for polling server connectors.
 */
public final class Constants {
    private Constants() {
    }

    public static final String POLLING_INTERVAL = "pollingInterval";
    public static final String CRON_EXPRESSION = "cronExpression";
    public static final String SCHEDULER_INSTANCE_NAME = "org.quartz.scheduler.instanceName";
    public static final String SCHEDULER_RMI_EXPORT = "org.quartz.scheduler.rmi.export";
    public static final String SCHEDULER_RMI_PROXY = "org.quartz.scheduler.rmi.proxy";
    public static final String SCHEDULER_WRAP_JOB_EXE_IN_USER_TRANSACTION =
            "org.quartz.scheduler.wrapJobExecutionInUserTransaction";
    public static final String THREAD_POOL_CLASS = "org.quartz.threadPool.class";
    public static final String THREAD_POOL_THREAD_COUNT = "org.quartz.threadPool.threadCount";
    public static final String THREAD_POOL_THREAD_PRIORITY = "org.quartz.threadPool.threadPriority";
    public static final String JOB_STORE_MISFIRE_THRESHOLD = "org.quartz.jobStore.misfireThreshold";
    public static final String THREAD_INHERIT_CONTEXT_CLASSLOADER_OF_INIT_THREAD =
            "org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread";
    public static final String JOB_STORE_CLASS = "org.quartz.jobStore.class";
}
