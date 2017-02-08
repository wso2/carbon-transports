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

package org.wso2.carbon.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class FileTransportTestCase {
    private static final Logger log = LoggerFactory.getLogger(FileTransportTestCase.class);


    @BeforeClass(description = "")
    public void setUp() {

    }

    @Test(description = "")
    public void filePollingTestCase() {
//        Map fileProperties = new HashMap();
//        fileProperties.put(TRANSPORT_FILE_FILE_URI, "file:///home/dilini/Desktop/myfile.txt");
//        fileProperties.put(POLLING_INTERVAL, "1");
//
//        FileTransportParams fileTransportParams = new FileTransportParams();
//        fileTransportParams.setName("myFileReader");
//        fileTransportParams.setProperties(fileProperties);
//
//        FilePollingTask filePollingTask = null;
//        try {
//            filePollingTask = new FilePollingTask(fileTransportParams, null);
//        } catch (InvalidConfigurationException e) {
//            log.error("Invalid configuration provided.", e);
//        }
//
//        filePollingTask.init();
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            log.error("Thread sleep got interrupted.", e);
//        }
    }

    @AfterClass(description = "")
    public void cleanUp() {

    }

}
