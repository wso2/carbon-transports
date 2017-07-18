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

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.util.DelegatingFileSystemOptionsBuilder;
import org.wso2.carbon.transport.file.connector.server.exception.FileServerConnectorException;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for File Transport.
 */
public class FileTransportUtils {

    private static final Pattern URL_PATTERN = Pattern.compile("[a-z]+://.*");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(":(?:[^/]+)@");

    /**
     * A utility method for setting the relevant configurations for the file system in question
     *
     * @param options   A map containigthe options to be attached to the file system
     * @param fsManager
     * @return
     * @throws FileServerConnectorException
     */
    public static FileSystemOptions attachFileSystemOptions(
            Map<String, String> options, FileSystemManager fsManager) throws FileServerConnectorException {
        if (options == null) {
            return null;    //returning null as this is not an errorneous case.
        }
        FileSystemOptions opts = new FileSystemOptions();
        DelegatingFileSystemOptionsBuilder delegate = new DelegatingFileSystemOptionsBuilder(fsManager);
        if (Constants.SCHEME_SFTP.equals(options.get(Constants.SCHEME))) {
            Iterator itr = options.entrySet().iterator();

            while (itr.hasNext()) {
                Map.Entry<String, String> entry = (Map.Entry<String, String>) itr.next();
                Constants.SftpFileOption[] array = Constants.SftpFileOption.values();
                int length = array.length;

                for (int i = 0; i < length; ++i) {
                    Constants.SftpFileOption option = array[i];
                    if (entry.getKey().equals(option.toString()) && null != entry.getValue()) {
                        try {
                            delegate.setConfigString(opts, Constants.SCHEME_SFTP,
                                    entry.getKey().toLowerCase(Locale.US), entry.getValue());
                        } catch (FileSystemException e) {
                            throw new FileServerConnectorException(
                                    "Failed to set file transport configuration for scheme: "
                                            + Constants.SCHEME_SFTP + " and option: " + option.toString(), e);
                        }
                    }
                }
            }
        }
        if (options.get(Constants.FILE_TYPE) != null) {
            try {
                delegate.setConfigString(opts, options.get(Constants.SCHEME),
                        Constants.FILE_TYPE,
                        String.valueOf(getFileType(options.get(Constants.FILE_TYPE))));
            } catch (FileSystemException e) {
                throw new FileServerConnectorException(
                        "Failed to set file transport configuration for scheme: " + options.get(Constants.SCHEME) +
                                " and option: " + Constants.FILE_TYPE, e);
            }
        }
        return opts;
    }

    /**
     * A utility method for masking the password in a file URI
     *
     * @param url The file URL in which the password has to be masked.
     * @return The URL with the password masked
     */
    public static String maskURLPassword(String url) {
        Matcher urlMatcher = URL_PATTERN.matcher(url);
        if (urlMatcher.find()) {
            Matcher pwdMatcher = PASSWORD_PATTERN.matcher(url);
            String maskUrl = pwdMatcher.replaceFirst("\":***@\"");
            return maskUrl;
        } else {
            return url;
        }
    }

    private static Integer getFileType(String fileType) {
        fileType = fileType.toUpperCase(Locale.US);
        return Constants.ASCII_TYPE.equals(fileType) ? Integer.valueOf(0) : (
                Constants.BINARY_TYPE.equals(fileType) ? Integer.valueOf(2) : (
                        Constants.EBCDIC_TYPE.equals(fileType) ? Integer.valueOf(1) : (
                                Constants.LOCAL_TYPE.equals(fileType) ? Integer.valueOf(3) : Integer.valueOf(2))));
    }
}
