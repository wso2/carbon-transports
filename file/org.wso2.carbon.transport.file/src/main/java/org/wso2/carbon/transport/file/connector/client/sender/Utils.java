package org.wso2.carbon.transport.file.connector.client.sender;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.exceptions.ClientConnectorException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * File Client Connector Utils
 */
public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(FileClientConnector.class);
    private static final byte[] bytes = new byte[4096];

    public static boolean fileCompress(FileObject fileObj, FileObject destObj) throws ClientConnectorException {
        boolean resultStatus = false;
        try {
            if (fileObj.exists()) {
                if (fileObj.getType() == FileType.FOLDER) {
                    List<FileObject> fileList = new ArrayList<FileObject>();
                    getAllFiles(fileObj, fileList);
                    writeZipFiles(fileObj, destObj, fileList);
                } else {
                    ZipOutputStream outputStream = null;
                    InputStream fileIn = null;
                    try {
                        outputStream = new ZipOutputStream(destObj.getContent().getOutputStream());
                        fileIn = fileObj.getContent().getInputStream();
                        ZipEntry zipEntry = new ZipEntry(fileObj.getName().getBaseName());
                        outputStream.putNextEntry(zipEntry);
                        int length;
                        while ((length = fileIn.read(bytes)) != -1) {
                            outputStream.write(bytes, 0, length);
                        }
                    } catch (Exception e) {
                        logger.error("Unable to compress a file." + e.getMessage());
                    } finally {
                        try {
                            if (outputStream != null) {
                                outputStream.close();
                            }
                        } catch (IOException e) {
                            logger.error("Error while closing ZipOutputStream: " + e.getMessage(), e);
                        }
                        try {
                            if (fileIn != null) {
                                fileIn.close();
                            }
                        } catch (IOException e) {
                            logger.error("Error while closing InputStream: " + e.getMessage(), e);
                        }
                        IOUtils.closeQuietly(outputStream);
                    }
                }
                resultStatus = true;

                if (logger.isDebugEnabled()) {
                    logger.debug("File archiving completed." + destObj.getURL());
                }
            } else {
                logger.error("The File location does not exist.");
                resultStatus = false;
            }
        } catch (IOException e) {
            throw new ClientConnectorException("Unable to process the zip file", e);
        }
        return resultStatus;
    }

    public static void getAllFiles(FileObject dir, List<FileObject> fileList) throws ClientConnectorException {
        try {
            FileObject[] children = dir.getChildren();
            for (FileObject child : children) {
                fileList.add(child);
                if (child.getType() == FileType.FOLDER) {
                    getAllFiles(child, fileList);
                }
            }
        } catch (IOException e) {
            throw new ClientConnectorException("Unable to get all files", e);
        }
    }

    /**
     * @param fileObj        source fileObject
     * @param directoryToZip destination fileObject
     * @param fileList       list of files to be compressed
     * @throws IOException
     */
    public static void writeZipFiles(FileObject fileObj, FileObject directoryToZip, List<FileObject> fileList)
            throws IOException, ClientConnectorException {
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(directoryToZip.getContent().getOutputStream());
            for (FileObject file : fileList) {
                if (file.getType() == FileType.FILE) {
                    addToZip(fileObj, file, zos);
                }
            }
        } catch (IOException e) {
            throw new ClientConnectorException("Error occur in writing files", e);
        } finally {
            if (zos != null) {
                zos.close();
            }
        }
    }

    /**
     * @param fileObject   Source fileObject
     * @param file         The file inside source folder
     * @param outputStream ZipOutputStream
     */
    public static void addToZip(FileObject fileObject, FileObject file, ZipOutputStream outputStream) {
        InputStream fin = null;
        try {
            fin = file.getContent().getInputStream();
            String name = file.getName().toString();
            String entry = name.substring(fileObject.getName().toString().length() + 1, name.length());
            ZipEntry zipEntry = new ZipEntry(entry);
            outputStream.putNextEntry(zipEntry);
            int length;
            while ((length = fin.read(bytes)) != -1) {
                outputStream.write(bytes, 0, length);
            }
        } catch (IOException e) {
            logger.error("Unable to add a file into the zip file directory." + e.getMessage());
        } finally {
            try {
                outputStream.closeEntry();
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException e) {
                logger.error("Error while closing InputStream: " + e.getMessage(), e);
            }
        }
    }
}
