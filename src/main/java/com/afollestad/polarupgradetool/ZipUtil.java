package com.afollestad.polarupgradetool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Aidan Follestad (afollestad)
 */
class ZipUtil {

    private static ArrayList<File> getAllFiles(File dir) {
        ArrayList<File> fileList = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return fileList;
        for (File file : files) {
            if ((file.getName().equals(".git") || file.getName().equals(".idea") ||
                    file.getName().equals(".gradle") || file.getName().equals("build")) &&
                    file.isDirectory()) {
                continue;
            }
            fileList.add(file);
            if (file.isDirectory())
                fileList.addAll(getAllFiles(file));
        }
        return fileList;
    }

    public static void writeZipFile(File directoryToZip, File destZipFile) throws Exception {
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        final List<File> files = getAllFiles(directoryToZip);
        try {
            fos = new FileOutputStream(destZipFile);
            zos = new ZipOutputStream(fos);
            for (File file : files) {
                if (!file.isDirectory()) { // we only zip files, not directories
                    addToZip(directoryToZip, file, zos);
                }
            }
        } finally {
            Util.closeQuietely(zos);
            Util.closeQuietely(fos);
        }
    }

    private static void addToZip(File directoryToZip, File file, ZipOutputStream zos) throws Exception {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            // we want the zipEntry's path to be a relative path that is relative
            // to the directory being zipped, so chop off the rest of the path
            final String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1,
                    file.getCanonicalPath().length());
            Main.LOG("[INFO] Zipping %s", zipFilePath);
            ZipEntry zipEntry = new ZipEntry(zipFilePath);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[Main.BUFFER_SIZE];
            int length;
            while ((length = fis.read(bytes)) >= 0)
                zos.write(bytes, 0, length);
            zos.closeEntry();
        } finally {
            Util.closeQuietely(fis);
        }
    }

    private ZipUtil() {
    }
}
