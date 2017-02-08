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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.util.DelegatingFileSystemOptionsBuilder;
import org.wso2.carbon.transport.file.connector.server.LockParamsDTO;
import org.wso2.carbon.transport.file.connector.server.exception.FileServerConnectorException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for File Transport.
 */
public class FileTransportUtils {

    private static final Log log = LogFactory.getLog(FileTransportUtils.class);
    private static final Pattern URL_PATTERN = Pattern.compile("[a-z]+://.*");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(":(?:[^/]+)@");

    public static FileSystemOptions attachFileSystemOptions(
            Map<String, String> options, FileSystemManager fsManager) throws FileServerConnectorException {
        if (options == null) {
            return null;
        } else {
            FileSystemOptions opts = new FileSystemOptions();
            DelegatingFileSystemOptionsBuilder delegate = new DelegatingFileSystemOptionsBuilder(fsManager);
            if (Constants.SCHEME_SFTP.equals(options.get(Constants.SCHEME))) {
                Iterator itr = options.entrySet().iterator();

                while (itr.hasNext()) {
                    Map.Entry<String, String> entry = (Map.Entry<String, String>) itr.next();
                    Constants.SftpFileOption[] array = Constants.SftpFileOption.values();
                    int length = array.length;

                    for (int i = 0; i < length; ++i) {
                        Constants.SftpFileOption o = array[i];
                        if (entry.getKey().equals(o.toString()) && null != entry.getValue()) {
                            try {
                                delegate.setConfigString(opts, Constants.SCHEME_SFTP,
                                        entry.getKey().toLowerCase(Locale.US), entry.getValue());
                            } catch (FileSystemException e) {
                                throw new FileServerConnectorException(
                                        "Failed to set file transport configuration for scheme: "
                                                + Constants.SCHEME_SFTP + " and option: " + o.toString(), e);
                            }
                        }
                    }
                }
            }
            if (options.get(Constants.FILE_TYPE) != null) {
                try {
                    delegate.setConfigString(opts, options.get(Constants.SCHEME),
                            Constants.FILE_TYPE, String.valueOf(getFileType(
                                    options.get(Constants.FILE_TYPE))));
                } catch (FileSystemException e) {
                    throw new FileServerConnectorException(
                            "Failed to set file transport configuration for scheme: "
                                    + options.get(Constants.SCHEME) + " and option: "
                                    + Constants.FILE_TYPE, e);
                }
            }
            return opts;
        }
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

    public static boolean isFailRecord(FileSystemManager fsManager, FileObject fo) {
        try {
            String e = fo.getName().getURI();
            int pos = e.indexOf("?");
            if (pos > -1) {
                e = e.substring(0, pos);
            }
            FileObject failObject = fsManager.resolveFile(e + ".fail");
            if (failObject.exists()) {
                return true;
            }
        } catch (FileSystemException var5) {
            log.error("Couldn\'t release the fail for the file : " +
                    maskURLPassword(fo.getName().getURI()));
        }
        return false;
    }

    public static synchronized boolean acquireLock(FileSystemManager fsManager, FileObject fo,
                                                   LockParamsDTO paramDTO,
                                                   FileSystemOptions fso, boolean isListener) {
        Random random = new Random();
        String strLockValue = String.valueOf(random.nextLong());

        try {
            strLockValue = strLockValue + ":" + InetAddress.getLocalHost().getHostName();
            strLockValue = strLockValue + ":" + InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException var24) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to get the Hostname or IP.");
            }
        }

        strLockValue = strLockValue + ":" + (new Date()).getTime();
        byte[] lockValue = strLockValue.getBytes(Charset.defaultCharset());
        FileObject lockObject = null;

        try {
            String fse = fo.getName().getURI();
            int e = fse.indexOf("?");
            if (e != -1) {
                fse = fse.substring(0, e);
            }

            lockObject = fsManager.resolveFile(fse + ".lock", fso);
            if (lockObject.exists()) {
                log.debug("There seems to be an external lock, aborting the processing of the file "
                        + maskURLPassword(fo.getName().getURI()) +
                        ". This could possibly be due to some other party already "
                        + "processing this file or the file is still being uploaded");
                if (paramDTO != null && paramDTO.isAutoLockRelease()) {
                    releaseLock(lockValue, strLockValue, lockObject,
                            Boolean.valueOf(paramDTO.isAutoLockReleaseSameNode()),
                            paramDTO.getAutoLockReleaseInterval());
                }
            } else {
                if (isListener) {
                    FileObject stream = fsManager.resolveFile(fse, fso);
                    if (!stream.exists()) {
                        return false;
                    }
                }

                lockObject.createFile();
                OutputStream stream1 = lockObject.getContent().getOutputStream();

                label128:
                {
                    boolean var13;
                    try {
                        stream1.write(lockValue);
                        stream1.flush();
                        stream1.close();
                        break label128;
                    } catch (IOException var21) {
                        lockObject.delete();
                        log.error("Couldn\'t create the lock file before processing the file "
                                + maskURLPassword(fse), var21);
                        var13 = false;
                    } finally {
                        lockObject.close();
                    }

                    return var13;
                }

                FileObject verifyingLockObject = fsManager.resolveFile(fse + ".lock", fso);
                if (verifyingLockObject.exists() && verifyLock(lockValue, verifyingLockObject)) {
                    return true;
                }
            }
        } catch (FileSystemException var23) {
            log.error("Cannot get the lock for the file : "
                    + maskURLPassword(fo.getName().getURI()) + " before processing", var23);
            if (lockObject != null) {
                try {
                    fsManager.closeFileSystem(lockObject.getParent().getFileSystem());
                } catch (FileSystemException var20) {
                    log.warn("Unable to close the lockObject parent file system");
                }
            }
        }

        return false;
    }

    public static void releaseLock(FileSystemManager fsManager, FileObject fo, FileSystemOptions fso) {
        String fullPath = fo.getName().getURI();
        try {
            int e = fullPath.indexOf("?");
            if (e > -1) {
                fullPath = fullPath.substring(0, e);
            }
            FileObject lockObject = fsManager.resolveFile(fullPath + ".lock", fso);
            if (lockObject.exists()) {
                lockObject.delete();
            }
        } catch (FileSystemException var6) {
            log.error("Couldn\'t release the lock for the file : "
                    + maskURLPassword(fo.getName().getURI()) + " after processing");
        }
    }

    public static synchronized void markFailRecord(FileSystemManager fsManager, FileObject fo) {
        byte[] failValue = Long.toString((new Date()).getTime()).getBytes(Charset.defaultCharset());
        try {
            String fse = fo.getName().getURI();
            int pos = fse.indexOf("?");
            if (pos != -1) {
                fse = fse.substring(0, pos);
            }
            FileObject failObject = fsManager.resolveFile(fse + ".fail");
            if (!failObject.exists()) {
                failObject.createFile();
            }
            OutputStream stream = failObject.getContent().getOutputStream();
            try {
                stream.write(failValue);
                stream.flush();
                stream.close();
            } catch (IOException var12) {
                failObject.delete();
                log.error("Couldn\'t create the fail file before processing the file " +
                        maskURLPassword(fse), var12);
            } finally {
                failObject.close();
            }
        } catch (FileSystemException var14) {
            log.error("Cannot get the lock for the file : "
                    + maskURLPassword(fo.getName().getURI()) + " before processing");
        }
    }

    public static void releaseFail(FileSystemManager fsManager, FileObject fo) {
        try {
            String e = fo.getName().getURI();
            int pos = e.indexOf("?");
            if (pos > -1) {
                e = e.substring(0, pos);
            }
            FileObject failObject = fsManager.resolveFile(e + ".fail");
            if (failObject.exists()) {
                failObject.delete();
            }
        } catch (FileSystemException var5) {
            log.error("Couldn\'t release the fail for the file : "
                    + maskURLPassword(fo.getName().getURI()));
        }
    }

    private static boolean verifyLock(byte[] lockValue, FileObject lockObject) {
        try {
            InputStream e = lockObject.getContent().getInputStream();
            byte[] val = new byte[lockValue.length];
            e.read(val);
            if (Arrays.equals(lockValue, val) && e.read() == -1) {
                return true;
            } else {
                log.debug("The lock has been acquired by an another party");
                return false;
            }
        } catch (FileSystemException var4) {
            log.error("Couldn\'t verify the lock", var4);
            return false;
        } catch (IOException var5) {
            log.error("Couldn\'t verify the lock", var5);
            return false;
        }
    }

    private static boolean releaseLock(byte[] bLockValue, String sLockValue,
                                       FileObject lockObject, Boolean autoLockReleaseSameNode,
                                       Long autoLockReleaseInterval) {
        try {
            InputStream in = lockObject.getContent().getInputStream();
            byte[] val = new byte[bLockValue.length];
            in.read(val);
            String strVal = new String(val, Charset.defaultCharset());
            String[] arrVal = strVal.split(":");
            String[] arrValNew = sLockValue.split(":");
            if (arrVal.length == 4 && arrValNew.length == 4
                    && (!autoLockReleaseSameNode.booleanValue()
                    || arrVal[1].equals(arrValNew[1]) && arrVal[2].equals(arrValNew[2]))) {
                long lInterval = 0L;

                try {
                    lInterval = Long.parseLong(arrValNew[3]) - Long.parseLong(arrVal[3]);
                } catch (NumberFormatException var21) {
                    ;
                }

                if (autoLockReleaseInterval == null
                        || autoLockReleaseInterval.longValue() <= lInterval) {
                    try {
                        lockObject.delete();
                    } catch (Exception e) {
                        log.warn("Unable to delete the lock file during auto release cycle.", e);
                    } finally {
                        lockObject.close();
                    }

                    return true;
                }
            }

            return false;
        } catch (FileSystemException var22) {
            log.error("Couldn\'t verify the lock", var22);
            return false;
        } catch (IOException var23) {
            log.error("Couldn\'t verify the lock", var23);
            return false;
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
