package com.afollestad.polarupgradetool.utils

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * @author Aidan Follestad (afollestad)
 */
object UnzipUtil {

    private val BUFFER_SIZE = 4096

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Throws(IOException::class)
    fun unzip(zipFilePath: String, destDirectory: String) {
        val destDir = File(destDirectory)
        if (!destDir.exists())
            destDir.mkdir()
        val zipIn = ZipInputStream(FileInputStream(zipFilePath))
        var entry: ZipEntry? = zipIn.nextEntry
        // iterates over entries in the zip file
        while (entry != null) {
            val filePath = destDirectory + File.separator + entry.name
            if (!entry.isDirectory) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath)
            } else {
                // if the entry is a directory, make the directory
                val dir = File(filePath)
                dir.mkdir()
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
        zipIn.close()
    }

    @Throws(IOException::class)
    private fun extractFile(zipIn: ZipInputStream, filePath: String) {
        var bos: BufferedOutputStream? = null
        try {
            bos = BufferedOutputStream(FileOutputStream(filePath))
            val bytesIn = ByteArray(BUFFER_SIZE)
            var read: Int
            while (true) {
                read = zipIn.read(bytesIn);
                if (read == -1) break;
                bos.write(bytesIn, 0, read);
            }
        } finally {
            Util.closeQuietely(bos)
        }
    }
}