package com.afollestad.polarupgradetool.utils

import com.afollestad.polarupgradetool.MainBase
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * @author Aidan Follestad (afollestad)
 */
object ZipUtil {

    private fun getAllFiles(dir: File): ArrayList<File> {
        val fileList = ArrayList<File>()
        val files = dir.listFiles() ?: return fileList
        for (file in files) {
            if ((file.name == ".git" || file.name == ".idea" ||
                    file.name == ".gradle" || file.name == "build" ||
                    file.name == ".DS_Store" || file.name.endsWith(".db")) && file.isDirectory) {
                continue
            }
            fileList.add(file)
            if (file.isDirectory)
                fileList.addAll(getAllFiles(file))
        }
        return fileList
    }

    @Throws(Exception::class)
    fun writeZipFile(directoryToZip: File, destZipFile: File) {
        var fos: FileOutputStream? = null
        var zos: ZipOutputStream? = null
        val files = getAllFiles(directoryToZip)
        try {
            fos = FileOutputStream(destZipFile)
            zos = ZipOutputStream(fos)
            for (file in files) {
                if (!file.isDirectory) {
                    // we only zip files, not directories
                    addToZip(directoryToZip, file, zos)
                }
            }
        } finally {
            Util.closeQuietely(zos)
            Util.closeQuietely(fos)
        }
    }

    @Throws(Exception::class)
    private fun addToZip(directoryToZip: File, file: File, zos: ZipOutputStream) {
        // we want the zipEntry's path to be a relative path that is relative
        // to the directory being zipped, so chop off the rest of the path
        val zipFilePath = file.canonicalPath.substring(directoryToZip.canonicalPath.length + 1,
                file.canonicalPath.length)
        MainBase.LOG("[ZIP]: $zipFilePath")
        val zipEntry = ZipEntry(zipFilePath)
        zos.putNextEntry(zipEntry)
        file.forEachBlock { bytes, size ->
            zos.write(bytes, 0, size)
        }
        zos.closeEntry()
    }
}