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

package org.wso2.carbon.transport.filesystem.connector.server.util;

/**
 * This class contains the constants related to File transport.
 */
public final class Constants {

    public static final String PROTOCOL_FILE_SYSTEM = "file-system";

    //transport properties
    public static final String FILE_TRANSPORT_PROPERTY_SERVICE_NAME = "TRANSPORT_FILE_SERVICE_NAME";
    public static final String FILE_PATH = "FILE_PATH";
    public static final String FILE_URI = "FILE_URI";
    public static final String FILE_NAME = "FILE_NAME";
    public static final String FILE_LENGTH = "FILE_LENGTH";
    public static final String LAST_MODIFIED = "LAST_MODIFIED";

    //file connector parameters
    public static final String TRANSPORT_FILE_FILE_URI = "fileURI";
    public static final String FILE_SORT_PARAM = "fileSortAttribute";
    public static final String FILE_SORT_VALUE_NAME = "name";
    public static final String FILE_SORT_VALUE_SIZE = "size";
    public static final String FILE_SORT_VALUE_LASTMODIFIEDTIMESTAMP = "lastModifiedTimestamp";
    public static final String FILE_SORT_ORDER = "fileSortAscending";
    public static final String FILE_ACKNOWLEDGEMENT_TIME_OUT = "acknowledgementTimeOut";
    public static final String FILE_DELETE_IF_NOT_ACKNOWLEDGED = "deleteIfNotAcknowledged";
    public static final String FILE_NAME_PATTERN = "fileNamePattern";
    public static final String ACTION_AFTER_PROCESS = "actionAfterProcess";
    public static final String MOVE_TIMESTAMP_FORMAT = "moveTimestampFormat";
    public static final String MOVE_AFTER_PROCESS = "moveAfterProcess";
    public static final String FORCE_CREATE_FOLDER = "createFolder";
    public static final String LOCKING = "locking";

    public static final String ACTION_MOVE = "MOVE";

    public static final String SCHEME = "VFS_SCHEME";
    public static final String SFTP_PREFIX = "sftp";
    public static final String SCHEME_SFTP = "sftp";
    public static final String SCHEME_FTP = "ftp";

    public static final String FILE_TYPE = "filetype";
    public static final String BINARY_TYPE = "BINARY";
    public static final String LOCAL_TYPE = "LOCAL";
    public static final String ASCII_TYPE = "ASCII";
    public static final String EBCDIC_TYPE = "EBCDIC";

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
