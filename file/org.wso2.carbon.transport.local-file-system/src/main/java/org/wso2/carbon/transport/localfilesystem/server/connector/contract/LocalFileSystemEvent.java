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

package org.wso2.carbon.transport.localfilesystem.server.connector.contract;

import org.wso2.carbon.messaging.CarbonMessage;

/**
 * This class represent the events like create, delete, modify that happen in local file system.
 */
public class LocalFileSystemEvent extends CarbonMessage {

    private final String fileName;
    private final String event;

    public LocalFileSystemEvent(String fileName, String event) {
        this.fileName = fileName;
        this.event = event;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getEvent() {
        return event;
    }
}
