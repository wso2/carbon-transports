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

package org.wso2.carbon.transport.remotefilesystem.client.connector.contractimpl;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.exceptions.ClientConnectorException;
import org.wso2.carbon.transport.remotefilesystem.Constants;
import org.wso2.carbon.transport.remotefilesystem.client.connector.contract.VFSClientConnector;
import org.wso2.carbon.transport.remotefilesystem.client.connector.contract.VFSClientConnectorFuture;
import org.wso2.carbon.transport.remotefilesystem.listener.RemoteFileSystemListener;
import org.wso2.carbon.transport.remotefilesystem.message.RemoteFileSystemMessage;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Implementation for {@link VFSClientConnector} interface.
 */
public class VFSClientConnectorImpl implements VFSClientConnector {

    private static final Logger logger = LoggerFactory.getLogger(VFSClientConnectorImpl.class);

    private String serviceId;
    private Map<String, String> connectorConfig;
    private RemoteFileSystemListener remoteFileSystemListener;
    private FileSystemOptions opts = new FileSystemOptions();

    public VFSClientConnectorImpl(String serviceId, Map<String, String> connectorConfig,
                                  RemoteFileSystemListener remoteFileSystemListener) {
        this.serviceId = serviceId;
        this.connectorConfig = connectorConfig;
        this.remoteFileSystemListener = remoteFileSystemListener;

        if (Constants.PROTOCOL_FTP.equals(connectorConfig.get("PROTOCOL"))) {
            connectorConfig.forEach((property, value) -> {
                // TODO: Add support for other FTP related configurations
                if (Constants.FTP_PASSIVE_MODE.equals(property)) {
                    FtpFileSystemConfigBuilder.getInstance().setPassiveMode(opts, Boolean.parseBoolean(value));
                }
            });
        }
    }


    @Override
    public VFSClientConnectorFuture send(RemoteFileSystemMessage message) {
        VFSClientConnectorFuture connectorFuture = new VFSClientConnectorFutureImpl();
        connectorFuture.setFileSystemListener(this.remoteFileSystemListener);

        FtpFileSystemConfigBuilder.getInstance().setPassiveMode(opts, true);
        String fileURI = this.connectorConfig.get(Constants.URI);
        String action = this.connectorConfig.get(Constants.ACTION);
        FileType fileType;
        ByteBuffer byteBuffer;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        FileObject path = null;
        try {
            FileSystemManager fsManager = VFS.getManager();
            path = fsManager.resolveFile(fileURI, opts);
            fileType = path.getType();
            switch (action) {
                case Constants.CREATE:
                    boolean isFolder = Boolean.parseBoolean(this.connectorConfig.getOrDefault("create-folder",
                            "false"));
                    if (path.exists()) {
                        throw new ClientConnectorException("File already exists: " + path.getName().getURI());
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
                        byteBuffer = message.getBytes();
                        byte[] bytes = byteBuffer.array();
                        if (this.connectorConfig.get(Constants.APPEND) != null) {
                            outputStream = path.getContent().getOutputStream(
                                    Boolean.parseBoolean(this.connectorConfig.get(Constants.APPEND)));
                        } else {
                            outputStream = path.getContent().getOutputStream();
                        }
                        outputStream.write(bytes);
                        outputStream.flush();
                    }
                    break;
                case Constants.DELETE:
                    if (path.exists()) {
                        int filesDeleted = path.delete(Selectors.SELECT_ALL);
                        if (logger.isDebugEnabled()) {
                            logger.debug(filesDeleted + " files successfully deleted");
                        }
                    } else {
                        throw new ClientConnectorException(
                                "Failed to delete file: " + path.getName().getURI() + " not found");
                    }
                    break;
                case Constants.COPY:
                    if (path.exists()) {
                        String destination = this.connectorConfig.get("destination");
                        FileObject dest = fsManager.resolveFile(destination, opts);
                        dest.copyFrom(path, Selectors.SELECT_ALL);
                    } else {
                        throw new ClientConnectorException(
                                "Failed to copy file: " + path.getName().getURI() + " not found");
                    }
                    break;
                case Constants.MOVE:
                    if (path.exists()) {
                        //TODO: Improve this to fix issue #331
                        String destination = this.connectorConfig.get("destination");
                        FileObject newPath = fsManager.resolveFile(destination, opts);
                        FileObject parent = newPath.getParent();
                        if (parent != null && !parent.exists()) {
                            parent.createFolder();
                        }
                        if (!newPath.exists()) {
                            path.moveTo(newPath);
                        } else {
                            throw new ClientConnectorException("The file at " + newPath.getURL().toString() +
                                    " already exists or it is a directory");
                        }
                    } else {
                        throw new ClientConnectorException(
                                "Failed to move file: " + path.getName().getURI() + " not found");
                    }
                    break;
                case Constants.READ:
                    if (path.exists()) {
                        //TODO: Do not assume 'path' always refers to a file
                        inputStream = path.getContent().getInputStream();
                        byte[] bytes = toByteArray(inputStream);
                        RemoteFileSystemMessage fileContent = new RemoteFileSystemMessage(ByteBuffer.wrap(bytes));
                        message.setProperty(org.wso2.carbon.messaging.Constants.DIRECTION,
                                org.wso2.carbon.messaging.Constants.DIRECTION_RESPONSE);
                        connectorFuture.notifyFileSystemListener(fileContent);
                    } else {
                        throw new ClientConnectorException(
                                "Failed to read file: " + path.getName().getURI() + " not found");
                    }
                    break;
                case Constants.EXISTS:
                    RemoteFileSystemMessage fileContent = new RemoteFileSystemMessage(Boolean.toString(path.exists()));
                    message.setProperty(org.wso2.carbon.messaging.Constants.DIRECTION,
                            org.wso2.carbon.messaging.Constants.DIRECTION_RESPONSE);
                    connectorFuture.notifyFileSystemListener(fileContent);
                    break;
                default:
                    break;
            }
        } catch (ClientConnectorException | IOException e) {
            connectorFuture.notifyFileSystemListener(e);
        } finally {
            if (path != null) {
                try {
                    path.close();
                } catch (FileSystemException e) {
                    //Do nothing.
                }
            }
            closeQuietly(inputStream);
            closeQuietly(outputStream);
        }
        return connectorFuture;
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
        if (logger.isDebugEnabled()) {
            logger.debug(count + " bytes read");
        }
        byte[] bytes = output.toByteArray();
        closeQuietly(output);
        return bytes;
    }

    /**
     * Closes streams quietly
     *
     * @param closeable The stream that should be closed
     */
    private static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            // Do nothing.
        }
    }
}
