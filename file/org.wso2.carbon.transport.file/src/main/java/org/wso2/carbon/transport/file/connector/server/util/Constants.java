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

package org.wso2.carbon.transport.file.connector.server.util;

/**
 * This class contains the constants related to File transport.
 */
public final class Constants {

    public static final String PROTOCOL_NAME = "file";

    //transport properties
    public static final String FILE_TRANSPORT_PROPERTY_SERVICE_NAME = "TRANSPORT_FILE_SERVICE_NAME";

    //file connector parameters
    public static final String TRANSPORT_FILE_FILE_URI = "fileURI";
    public static final String READ_FILE_FROM_BEGINNING = "readFileFromBeginning";

    public static final String SCHEME = "VFS_SCHEME";
    public static final String SFTP_PREFIX = "sftp";
    public static final String SCHEME_SFTP = "sftp";
    public static final String SCHEME_FTP = "ftp";

    public static final String FILE_TYPE = "filetype";
    public static final String BINARY_TYPE = "BINARY";
    public static final String LOCAL_TYPE = "LOCAL";
    public static final String ASCII_TYPE = "ASCII";
    public static final String EBCDIC_TYPE = "EBCDIC";
    public static final String FILE_UPDATE = "FILE_UPDATE";
    public static final String FILE_ROTATE = "FILE_ROTATE";
    public static final String FILE_TRANSPORT_EVENT_NAME = "FILE_TRANSPORT_EVENT_NAME";

    private Constants() {
    }

    /**
     * Enum for SFTP file options.
     */
    public enum SftpFileOption {
        Identities,
        UserDirIsRoot,
        IdentityPassPhrase;

        private SftpFileOption() {
        }
    }
}
