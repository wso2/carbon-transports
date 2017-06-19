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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
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
    private static List<String> processed = new ArrayList<>();
    //private static List<String> failed = new ArrayList<>();

    private static final Pattern URL_PATTERN = Pattern.compile("[a-z]+://.*");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(":(?:[^/]+)@");

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

    public static synchronized void markFailRecord(FileSystemManager fsManager, FileObject fo) {

        // generate a random fail value to ensure that there are no two parties
        // processing the same file
        byte[] failValue = (Long.toString((new Date()).getTime())).getBytes(Charset.defaultCharset());

        try {
            String fullPath = fo.getName().getURI();
            int pos = fullPath.indexOf("?");
            if (pos != -1) {
                fullPath = fullPath.substring(0, pos);
            }
            FileObject failObject = fsManager.resolveFile(fullPath + "fail");
            if (!failObject.exists()) {
                failObject.createFile();
            }

            // write a lock file before starting of the processing, to ensure that the
            // item is not processed by any other parties

            OutputStream stream = failObject.getContent().getOutputStream();
            try {
                stream.write(failValue);
                stream.flush();
                stream.close();
            } catch (IOException e) {
                failObject.delete();
                log.error("Couldn't create the fail file before processing the file " + maskURLPassword(fullPath), e);
            } finally {
                failObject.close();
            }
        } catch (FileSystemException fse) {
            log.error("Cannot get the lock for the file : " + maskURLPassword(fo.getName().getURI()) +
                      " before processing");
        }
    }

    public static boolean isFailRecord(FileSystemManager fsManager, FileObject fo) {
        try {
            String fullPath = fo.getName().getURI();
            int pos = fullPath.indexOf("?");
            if (pos > -1) {
                fullPath = fullPath.substring(0, pos);
            }
            FileObject failObject = fsManager.resolveFile(fullPath + ".fail");
            if (failObject.exists()) {
                return true;
            }
        } catch (FileSystemException e) {
            log.error("Couldn't release the fail for the file : " + maskURLPassword(fo.getName().getURI()));
        }
        return false;
    }

    public static void releaseFail(FileSystemManager fsManager, FileObject fo) {
        try {
            String fullPath = fo.getName().getURI();
            int pos = fullPath.indexOf("?");
            if (pos > -1) {
                fullPath = fullPath.substring(0, pos);
            }
            FileObject failObject = fsManager.resolveFile(fullPath + ".fail");
            if (failObject.exists()) {
                failObject.delete();
            }
        } catch (FileSystemException e) {
            log.error("Couldn't release the fail for the file : " + maskURLPassword(fo.getName().getURI()));
        }
    }

    /**
     * Acquire the file level locking.
     *
     * @param fsManager     The file system manager instance
     * @param fileObject    The file object to get the lock from
     * @return              Boolean value whether lock was successful
     */
    public static synchronized boolean acquireLock(FileSystemManager fsManager, FileObject fileObject) {
        String strContext = fileObject.getName().getURI();

        if (processed.contains(strContext)) {
            log.debug("The file: " + maskURLPassword(fileObject.getName().getURI()) + "is already processed");
            return false;
        }

        // When processing a directory list is fetched initially. Therefore
        // there is still a chance of file processed by another process.
        // Need to check the source file before processing.
        try {
            String parentURI = fileObject.getParent().getName().getURI();
            if (parentURI.contains("?")) {
                String suffix = parentURI.substring(parentURI.indexOf("?"));
                strContext += suffix;
            }
            FileObject sourceFile = fsManager.resolveFile(strContext);
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
            lockObject = fsManager.resolveFile(fullPath + ".lock");
            if (lockObject.exists()) {
                log.debug("There seems to be an external lock, aborting the processing of the file " +
                          maskURLPassword(fileObject.getName().getURI()) +
                          ". This could possibly be due to some other party already " +
                          "processing this file or the file is still being uploaded");
            } else if (processing.contains(fullPath)) {
                log.debug(maskURLPassword(fileObject.getName().getURI()) + "is already processed.");
            } else {
                //Check the original file existence before the lock file to handle concurrent access scenario
                FileObject originalFileObject = fsManager.resolveFile(fullPath);
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
     * Release a file item lock acquired either by the VFS listener or a sender.
     *
     * @param fo                    representing the processed file
     * @param actionAfterProcess    representing action after processing the file
     */
    public static synchronized void releaseLock(FileObject fo, String actionAfterProcess) {
        String fullPath = fo.getName().getURI();
        processing.remove(fullPath);
        if (actionAfterProcess.equals(Constants.ACTION_NONE)) {
            processed.add(fullPath);
        }
    }
}
