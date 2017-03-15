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
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
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

    @Override public boolean send(CarbonMessage carbonMessage, CarbonCallback carbonCallback)
            throws ClientConnectorException {
        return false;
    }

    @Override public boolean send(CarbonMessage carbonMessage, CarbonCallback carbonCallback, Map<String, String> map)
            throws ClientConnectorException {
        FileSystemManager fsManager = null;
        FileSystemOptions opts = new FileSystemOptions();
        String fileURI = map.get("uri");
        FileType fileType;
        InputStream is;
        OutputStream os = null;
        is = carbonMessage.getInputStream();
        try {
            fsManager = VFS.getManager();
            FileObject path = fsManager.resolveFile(fileURI, opts);

            fileType = path.getType();
            if (fileType == FileType.IMAGINARY) {
                path.createFile();
                os = path.getContent().getOutputStream();
                long bytesCopied = copy(is, os, new byte[4096]);
            }

        } catch (FileSystemException e) {
            throw new ClientConnectorException("Exception occurred while sending the message", e);
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
