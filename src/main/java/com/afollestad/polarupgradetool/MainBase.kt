package com.afollestad.polarupgradetool;

import com.afollestad.polarupgradetool.jfx.UICallback
import com.afollestad.polarupgradetool.utils.FileUtil
import com.afollestad.polarupgradetool.utils.UnzipUtil
import com.afollestad.polarupgradetool.utils.Util
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

/**
 * @author Aidan Follestad (afollestad)
 */
open class MainBase {
    companion object {

        private val ARCHIVE_URL = "https://github.com/afollestad/polar-dashboard/archive/master.zip"
        private val ARCHIVE_ROOT = File.separator + "polar-dashboard-master"

        val BUFFER_SIZE = 2048
        var EXTRACTED_ZIP_ROOT: File? = null
        var CURRENT_DIR: File? = null

        fun cleanupPath(path: String): String {
            var from = path
            if (from.startsWith(CURRENT_DIR!!.absolutePath)) {
                from = from.substring(CURRENT_DIR!!.absolutePath.length)
            } else if (from.startsWith(EXTRACTED_ZIP_ROOT!!.absolutePath)) {
                from = from.substring(EXTRACTED_ZIP_ROOT!!.absolutePath.length)
            }
            return from
        }

        public fun LOG(msg: String) {
            println(msg)
        }

        fun PROGRESS(label: String?, read: Long, total: Long): String {
            val percent = Math.ceil(read.toDouble() / total.toDouble() * 100.0).toInt()
            val sb = StringBuilder(13)
            sb.append('\r')
            if (label != null) {
                sb.append(label)
                sb.append("  ")
            }
            sb.append('[')
            val numOfEqual = percent / 10
            val numOfSpace = 10 - numOfEqual
            for (i in 0..numOfEqual - 1) sb.append('=')
            for (i in 0..numOfSpace - 1) sb.append(' ')
            sb.append("]")
            sb.append("   ")
            sb.append(Util.round(percent.toFloat()))
            sb.append("%  ")
            sb.append(Util.readableFileSizeMB(read))
            sb.append('/')
            sb.append(Util.readableFileSizeMB(total))
            print(sb.toString())
            return "%s/%s (%s%%)".format(Util.readableFileSizeMB(read), Util.readableFileSizeMB(total), Util.round(percent.toFloat()))
        }

        var TRIES = 0

        @SuppressWarnings("ResultOfMethodCallIgnored")
        fun downloadArchive(uiCallback: UICallback): Boolean {
            var `is`: InputStream? = null
            var os: FileOutputStream? = null

            if (TRIES == 0) {
                LOG("[INFO]: Contacting GitHub...")
                uiCallback.onStatusUpdate("Contacting GitHub...")
            }

            try {
                val url = URL(ARCHIVE_URL)
                val conn = url.openConnection()
                `is` = conn.inputStream

                val contentLength: Long
                try {
                    val contentLengthStr = conn.getHeaderField("Content-Length")
                    if (contentLengthStr == null || contentLengthStr.trim { it <= ' ' }.isEmpty()) {
                        if (TRIES > 1) {
                            LOG("[ERROR]: No Content-Length header was returned by GitHub. Try running this app again.")
                            uiCallback.onErrorOccurred("GitHub did not report a Content-Length, please try again.")
                            return false
                        }
                        TRIES++
                        Thread.sleep(2000)
                        return downloadArchive(uiCallback)
                    }
                    contentLength = java.lang.Long.parseLong(contentLengthStr)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    LOG("[ERROR]: Failed to get the size of Polar's latest code archive. Please try running this app again.")
                    uiCallback.onArchiveDownloadFailed("Failed to get the size of Polar's latest code archive. Please try running this app again.")
                    return false
                }

                val destZip = File(CURRENT_DIR, "PolarLatest.zip")
                if (destZip.exists()) destZip.delete()
                os = FileOutputStream(destZip)

                val buffer = ByteArray(BUFFER_SIZE)
                var read: Int
                var totalRead = 0

                LOG("[INFO]: Downloading a ZIP of Polar's latest code (${FileUtil.readableFileSize(contentLength)})...")
                uiCallback.onArchiveDownloadStarted(FileUtil.readableFileSize(contentLength))

                while (true) {
                    read = `is`!!.read(buffer);
                    if (read == -1) break;
                    os.write(buffer, 0, read)
                    totalRead += read
                    val progressStr = PROGRESS(null, totalRead.toLong(), contentLength)
                    uiCallback.onArchiveDownloadProgress(progressStr)
                }

                PROGRESS(null, contentLength, contentLength)
                println()
                LOG("[INFO]: Download complete!")
                uiCallback.onArchiveDownloadSuccess()
                os.flush()

                Util.closeQuietely(`is`)
                Util.closeQuietely(os)

                EXTRACTED_ZIP_ROOT = File(CURRENT_DIR, "PolarLatest")
                val destZipPath = destZip.absolutePath
                val extractedPath = EXTRACTED_ZIP_ROOT!!.absolutePath
                if (EXTRACTED_ZIP_ROOT!!.exists()) {
                    val removedCount = FileUtil.wipe(EXTRACTED_ZIP_ROOT!!)
                    LOG("[INFO]: Removed $removedCount files/folders from $extractedPath.")
                    uiCallback.onStatusUpdate("Removed $removedCount files/folders from $extractedPath.");
                }

                LOG("[INFO]: Extracting ${cleanupPath(destZipPath)} to ${cleanupPath(extractedPath)}...")
                uiCallback.onStatusUpdate("Extracting ${cleanupPath(destZipPath)} to ${cleanupPath(extractedPath)}...")
                UnzipUtil.unzip(destZipPath, extractedPath)
                LOG("[INFO]: Extraction complete!\n")
                uiCallback.onStatusUpdate("Extraction complete!")
                destZip.delete()
                EXTRACTED_ZIP_ROOT = File(EXTRACTED_ZIP_ROOT, ARCHIVE_ROOT)
            } catch (e: Exception) {
                LOG("[ERROR]: An error occurred during download or extraction: ${e.message}\n")
                uiCallback.onErrorOccurred("An error occurred during download or extraction: ${e.message}")
                return false
            } finally {
                Util.closeQuietely(`is`)
                Util.closeQuietely(os)
            }
            return true
        }
    }
}
