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

package org.wso2.carbon.transport.file.connector.client.sender;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.ClientConnector;
import org.wso2.carbon.messaging.exceptions.ClientConnectorException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * FileClientConnector
 */
public class FileClientConnector implements ClientConnector {
    private static final Logger logger = LoggerFactory.getLogger(FileClientConnector.class);
    private String fileURI;
    private FileSystemManager fsManager;
    private FileSystemOptions opts = new FileSystemOptions();

    @Override public boolean send(CarbonMessage carbonMessage, CarbonCallback carbonCallback)
            throws ClientConnectorException {
        return false;
    }

    @Override public boolean send(CarbonMessage carbonMessage, CarbonCallback carbonCallback, Map<String, String> map)
            throws ClientConnectorException {
        fileURI = map.get("uri");
        String action = map.get("action");
        FileType fileType;
        InputStream is = null;
        OutputStream os = null;
        try {
            fsManager = VFS.getManager();
            FileObject path = fsManager.resolveFile(fileURI, opts);

            fileType = path.getType();
            if (action.equalsIgnoreCase("send")) {
                if (fileType == FileType.IMAGINARY) {
                    path.createFile();
                    is = carbonMessage.getInputStream();
                    os = path.getContent().getOutputStream();
                    long bytesCopied = copy(is, os, new byte[4096]);

                    if (logger.isDebugEnabled()) {
                        String bytes = Long.toString(bytesCopied);
                        logger.info(bytes + " bytes sent");
                    }
                }
            } else if (action.equalsIgnoreCase("delete")) {
                if (fileType == FileType.FILE_OR_FOLDER) {
                    boolean isDeleted = path.delete();
                    if (logger.isDebugEnabled() && isDeleted) {
                        logger.info("File Successfully Deleted");
                    }
                }
            } else if (action.equalsIgnoreCase("append")) {
                if (fileType == FileType.FILE) {
                    path.createFile();
                    is = carbonMessage.getInputStream();
                    os = path.getContent().getOutputStream(true);
                    long bytesCopied = copy(is, os, new byte[4096]);

                    if (logger.isDebugEnabled()) {
                        String bytes = Long.toString(bytesCopied);
                        logger.info(bytes + " bytes appended");
                    }
                }
            }
        } catch (IOException e) {
            throw new ClientConnectorException("Exception occurred while sending the message", e);
        } finally {
            closeQuietly(is);
            closeQuietly(os);
        }
        return false;
    }

    @Override public String getProtocol() {
        return "file";
    }

    @Override public void setMessageProcessor(CarbonMessageProcessor carbonMessageProcessor) {

    }

    public static long copy(InputStream input, OutputStream output, byte[] buffer) throws IOException {
        long count = 0L;

        int n1;
        for (boolean n = false; -1 != (n1 = input.read(buffer)); count += (long) n1) {
            output.write(buffer, 0, n1);
        }

        return count > 2147483647L ? -1 : (int) count;
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException var2) {

        }

    }
}
