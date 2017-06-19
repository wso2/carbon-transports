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

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * The {@link PollingTaskRunner} which executes a poll based on provided CRON expression or polling interval.
 */
public class PollingTaskRunner {
    private static final Logger log = LoggerFactory.getLogger(PollingTaskRunner.class);
    private static final Random RANDOM = new Random();

    private final PollingServerConnector connector;
    private Scheduler scheduler;

    public PollingTaskRunner(PollingServerConnector connector) {
        this.connector = connector;
    }

    public void start() {
        String group = "carbon.connector.quartz";
        JobDetail job = JobBuilder.newJob(PollingJob.class)
                                  .withIdentity("Job - " + RANDOM.nextLong(), group).build();

        Trigger trigger;
        if (connector.cronExpression != null) {
            // Trigger the job to run on the next round minute
            trigger = TriggerBuilder.newTrigger().withIdentity("Trigger - " + RANDOM.nextLong(), group)
                                    .withSchedule(CronScheduleBuilder.cronSchedule(connector.cronExpression)).build();
        } else {
            // Trigger the job to run on the next round minute
            trigger = TriggerBuilder.newTrigger().withIdentity("scheduledPoll", "group1").withSchedule(
                    SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(connector.interval)
                                         .repeatForever()).build();
        }

        // Schedule the job
        try {
            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.getContext().put("connector", connector);
            scheduler.start();
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            log.error("Exception occurred while scheduling job");
        }

    }

    /**
     * Exit the running while loop and terminate the thread.
     */
    protected void terminate() {
        try {
            scheduler.shutdown();
        } catch (SchedulerException e) {
            log.error("Exception occurred when shutting down scheduler");
        }
    }
}
