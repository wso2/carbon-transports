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

package org.wso2.carbon.transport.file.connector.sender;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.BinaryCarbonMessage;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.ClientConnector;
import org.wso2.carbon.messaging.TextCarbonMessage;
import org.wso2.carbon.messaging.exceptions.ClientConnectorException;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * A Client Connector implementation for file systems using the Apache VFS library for file operations
 */
public class VFSClientConnector implements ClientConnector {
    private static final Logger logger = LoggerFactory.getLogger(VFSClientConnector.class);
    private FileSystemOptions opts = new FileSystemOptions();
    private CarbonMessageProcessor carbonMessageProcessor;

    @Override
    public Object init(CarbonMessage cMsg, CarbonCallback callback, Map<String, Object> properties)
            throws ClientConnectorException {
        throw new ClientConnectorException("Method not supported for VFS.");
    }

    @Override
    public boolean send(CarbonMessage carbonMessage, CarbonCallback carbonCallback)
            throws ClientConnectorException {
        return false;
    }

    @Override
    public boolean send(CarbonMessage carbonMessage, CarbonCallback carbonCallback, Map<String, String> map)
            throws ClientConnectorException {
        String fileURI = map.get(Constants.FILE_URI);
        String action = map.get(Constants.ACTION);
        FileType fileType;
        ByteBuffer byteBuffer;
        InputStream is = null;
        OutputStream os = null;
        try {
            FileSystemManager fsManager = VFS.getManager();
            FileObject path = fsManager.resolveFile(fileURI, opts);
            fileType = path.getType();
            switch (action) {

                case Constants.CREATE:
                    boolean isFolder = Boolean.parseBoolean(map.getOrDefault("create-folder", "false"));
                    if (path.exists()) {
                        logger.info("Deleting existing file/folder");
                        path.delete();
                    }
                    if (isFolder) {
                        path.createFolder();
                    } else {
                        path.createFile();
                    }
                    break;
                case Constants.WRITE:
                    if (!path.exists()) {
                        path.createFile();
                        path.refresh();
                        fileType = path.getType();
                    }
                    if (fileType == FileType.FILE) {
                        if (carbonMessage instanceof BinaryCarbonMessage) {
                            BinaryCarbonMessage binaryCarbonMessage = (BinaryCarbonMessage) carbonMessage;
                            byteBuffer = binaryCarbonMessage.readBytes();
                        } else {
                            throw new ClientConnectorException(
                                    "received carbon message " + "isn't a BinaryCarbonMessage");
                        }
                        byte[] bytes = byteBuffer.array();
                        if (map.get(Constants.APPEND) != null) {
                            os = path.getContent().
                                    getOutputStream(Boolean.parseBoolean(map.get(Constants.APPEND)));
                        } else {
                            os = path.getContent().getOutputStream();
                        }
                        os.write(bytes);
                        os.flush();
                    }
                    break;
                case Constants.DELETE:
                    if (path.exists()) {
                        int filesDeleted = path.delete(Selectors.SELECT_ALL);
                        logger.debug(filesDeleted + "files Successfully Deleted");
                    } else {
                        throw new ClientConnectorException("failed to delete file: file " +
                                                           "not found:" + path.getName());
                    }
                    break;
                case Constants.COPY:
                    if (path.exists()) {
                        String destination = map.get("destination");
                        FileObject dest = fsManager.resolveFile(destination, opts);
                        dest.copyFrom(path, Selectors.SELECT_ALL);
                    } else {
                        throw new ClientConnectorException("failed to copy file: file " +
                                                           "not found:" + path.getName());
                    }
                    break;
                case Constants.MOVE:
                    if (path.exists()) {
                        String destination = map.get("destination");
                        FileObject newPath = fsManager.resolveFile(destination, opts);
                        FileObject parent = newPath.getParent();
                        if (parent != null && !parent.exists()) {
                            parent.createFolder();
                        }
                        if (!newPath.exists()) {
                            path.moveTo(newPath);
                        } else {
                            throw new ClientConnectorException("the file at " + newPath.getURL().toString() +
                                                                       " already exists or it is a directory");
                        }
                    } else {
                        throw new ClientConnectorException("failed to move file: file not found:" + path.getName());
                    }
                    break;
                case Constants.READ:
                    if (path.exists()) {
                        is = path.getContent().getInputStream();
                        byte[] bytes = toByteArray(is);
                        BinaryCarbonMessage message = new BinaryCarbonMessage(ByteBuffer.wrap(bytes), true);
                        message.setProperty(org.wso2.carbon.messaging.Constants.DIRECTION,
                                            org.wso2.carbon.messaging.Constants.DIRECTION_RESPONSE);
                        carbonMessageProcessor.receive(message, carbonCallback);
                    } else {
                        throw new ClientConnectorException("failed to read file: file not found:" + path.getName());
                    }
                    break;
                case Constants.EXISTS:
                    TextCarbonMessage message = new TextCarbonMessage(Boolean.toString(path.exists()));
                    message.setProperty(org.wso2.carbon.messaging.Constants.DIRECTION,
                                        org.wso2.carbon.messaging.Constants.DIRECTION_RESPONSE);
                    carbonMessageProcessor.receive(message, carbonCallback);
                    break;
                default:
                    return false;
            }
        } catch (RuntimeException e) {
            throw new ClientConnectorException("Runtime Exception occurred : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ClientConnectorException("Exception occurred while processing file: " + e.getMessage(), e);
        } finally {
            closeQuietly(is);
            closeQuietly(os);
        }
        return true;
    }

    @Override
    public String getProtocol() {
        return Constants.PROTOCOL_NAME;
    }

    /**
     * Obtain a byte[] from an input stream
     *
     * @param input The input stream that the data should be obtained from
     * @return byte[] The byte array of data obtained from the input stream
     * @throws IOException
     */
    private static byte[] toByteArray(InputStream input) throws IOException {
        long count = 0L;
        byte[] buffer = new byte[4096];
        int n1;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (; -1 != (n1 = input.read(buffer)); count += (long) n1) {
            output.write(buffer, 0, n1);
        }
        logger.debug(count + " no. of bytes read");
        byte[] bytes = output.toByteArray();
        closeQuietly(output);
        return bytes;
    }

    /**
     * Closes streams quietly
     * @param closeable The stream that should be closed
     */
    private static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            logger.error("Error occurred when closing stream", e);
        }

    }

    @Override
    public void setMessageProcessor(CarbonMessageProcessor carbonMessageProcessor) {
        this.carbonMessageProcessor = carbonMessageProcessor;
    }
}
