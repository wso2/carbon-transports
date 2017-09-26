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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * This class represent the file message.
 */
public class LocalFileSystemMessage extends CarbonMessage {

    private final String fileName;
    private final String event;

    public LocalFileSystemMessage(String fileName, String event) {
        this.fileName = fileName;
        this.event = event;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getEvent() {
        return event;
    }

    public InputStream getInputStream() {
        return this.fileName == null ? null : new ByteArrayInputStream(this.fileName.getBytes(StandardCharsets.UTF_8));
    }

    public ByteBuffer getMessageBody() {
        this.setEndOfMsgAdded(true);
        return ByteBuffer.wrap(this.fileName.getBytes(StandardCharsets.UTF_8));
    }
}
