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

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.util.DelegatingFileSystemOptionsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.filesystem.connector.server.FileSystemConsumer;
import org.wso2.carbon.transport.filesystem.connector.server.exception.FileSystemServerConnectorException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for File Transport.
 */
public class FileTransportUtils {

    private static final Logger log = LoggerFactory.getLogger(FileSystemConsumer.class);
    private static List<String> processing = new ArrayList<>();

    private static final Pattern URL_PATTERN = Pattern.compile("[a-z]+://.*");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(":(?:[^/]+)@");

    /**
     * A utility method for setting the relevant configurations for the file system in question
     *
     * @param options
     * @param fsManager
     * @return
     * @throws FileSystemServerConnectorException
     */
    public static FileSystemOptions attachFileSystemOptions(
            Map<String, String> options, FileSystemManager fsManager) throws FileSystemServerConnectorException {
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
                            throw new FileSystemServerConnectorException(
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
                        Constants.FILE_TYPE, String.valueOf(getFileType(options.get(Constants.FILE_TYPE))));
            } catch (FileSystemException e) {
                throw new FileSystemServerConnectorException("Failed to set file transport configuration for scheme: "
                                                             + options.get(Constants.SCHEME) + " and option: "
                                                             + Constants.FILE_TYPE, e);
            }
        }
        return opts;
    }

    /**
     * A utility method for masking the password in a file URI
     *
     * @param url
     * @return
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

    /**
     * A utility method for retrieving the type of the file
     *
     * @param fileType
     * @return
     */
    private static Integer getFileType(String fileType) {
        fileType = fileType.toUpperCase(Locale.US);
        return Constants.ASCII_TYPE.equals(fileType) ? Integer.valueOf(0) : (
                Constants.BINARY_TYPE.equals(fileType) ? Integer.valueOf(2) : (
                        Constants.EBCDIC_TYPE.equals(fileType) ? Integer.valueOf(1) : (
                                Constants.LOCAL_TYPE.equals(fileType) ? Integer.valueOf(3) : Integer.valueOf(2))));
    }

    /**
     * Acquire the file level locking.
     *
     * @param fsManager     The file system manager instance
     * @param fileObject    The file object to get the lock from
     * @param fsOpts        The file system options to be used with the file system manager
     * @return              Boolean value whether lock was successful
     */
    public static synchronized boolean acquireLock(FileSystemManager fsManager, FileObject fileObject,
                                                   FileSystemOptions fsOpts) {
        String strContext = fileObject.getName().getURI();

        // When processing a directory list is fetched initially. Therefore
        // there is still a chance of file processed by another process.
        // Need to check the source file before processing.
        try {
            String parentURI = fileObject.getParent().getName().getURI();
            if (parentURI.contains("?")) {
                String suffix = parentURI.substring(parentURI.indexOf("?"));
                strContext += suffix;
            }
            FileObject sourceFile = fsManager.resolveFile(strContext, fsOpts);
            if (!sourceFile.exists()) {
                return false;
            }
        } catch (FileSystemException e) {
            return false;
        }

        FileObject lockObject = null;

        try {
            // check whether there is an existing lock for this item, if so it is assumed
            // to be processed by an another listener (downloading) or a sender (uploading)
            // lock file is derived by attaching the ".lock" second extension to the file name
            String fullPath = fileObject.getName().getURI();
            int pos = fullPath.indexOf("?");
            if (pos != -1) {
                fullPath = fullPath.substring(0, pos);
            }
            lockObject = fsManager.resolveFile(fullPath + ".lock", fsOpts);
            if (lockObject.exists()) {
                log.debug("There seems to be an external lock, aborting the processing of the file " +
                          maskURLPassword(fileObject.getName().getURI()) +
                          ". This could possibly be due to some other party already " +
                          "processing this file or the file is still being uploaded");
            } else if (processing.contains(fullPath)) {
                log.debug(maskURLPassword(fileObject.getName().getURI()) + "is already being processed.");
            } else {
                //Check the original file existence before the lock file to handle concurrent access scenario
                FileObject originalFileObject = fsManager.resolveFile(fullPath, fsOpts);
                if (!originalFileObject.exists()) {
                    return false;
                }
                processing.add(fullPath);
                return true;
            }
        } catch (FileSystemException fse) {
            log.error("Cannot get the lock for the file : " + maskURLPassword(fileObject.getName().getURI()) +
                      " before processing", fse);
            if (lockObject != null) {
                try {
                    fsManager.closeFileSystem(lockObject.getParent().getFileSystem());
                } catch (FileSystemException e) {
                    log.warn("Unable to close the lockObject parent file system");
                }
            }
        }
        return false;
    }

    /**
     * Release a file item lock acquired at the start of processing.
     *
     * @param fileObject    File that needs the lock to be removed
     */
    public static synchronized void releaseLock(FileObject fileObject) {
        String fullPath = fileObject.getName().getURI();
        processing.remove(fullPath);
    }
}
