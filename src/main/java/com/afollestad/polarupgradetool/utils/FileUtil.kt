package com.afollestad.polarupgradetool.utils

import com.afollestad.polarupgradetool.Main
import com.afollestad.polarupgradetool.MainBase
import com.afollestad.polarupgradetool.jfx.UICallback
import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*

/**
 * @author Aidan Follestad (afollestad)
 */
object FileUtil {

    fun readableFileSize(size: Long): String {
        val value: Double
        val unit: String
        if (size < 1000) {
            value = size.toDouble()
            unit = "B"
        } else if (size >= 1000 && size < 1000000) {
            value = size.toDouble() / 1000.toDouble()
            unit = "KB"
        } else if (size >= 1000000 && size < 1000000000) {
            value = size.toDouble() / 1000000.toDouble()
            unit = "MB"
        } else {
            value = size.toDouble() / 1000000000.toDouble()
            unit = "GB"
        }
        return "${Util.round(value)}$unit"
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    fun checkResRename(oldName: String, expectedName: String, uiCallback: UICallback) {
        val valuesFolder = File(MainBase.CURRENT_DIR, Main.VALUES_FOLDER_PATH)
        val source = File(valuesFolder, oldName)
        if (source.exists()) {
            val dest = File(valuesFolder, expectedName)
            if (!dest.exists()) {
                val cleanedSource = MainBase.cleanupPath(source.absolutePath)
                val cleanedDest = MainBase.cleanupPath(dest.absolutePath)
                MainBase.LOG("[RENAME]: $cleanedSource -> $cleanedDest")
                uiCallback.onStatusUpdate("Renaming $cleanedSource -> $cleanedDest")
                if (!source.renameTo(dest)) {
                    MainBase.LOG("[ERROR]: Unable to rename $cleanedSource")
                    uiCallback.onErrorOccurred("Unable to rename: $cleanedSource")
                }
            } else {
                source.delete()
            }
        } else {
            val msg = "$oldName file wasn't found (in ${MainBase.cleanupPath(source.parent)}), assuming $expectedName is used already."
            MainBase.LOG("[INFO] " + msg)
            uiCallback.onStatusUpdate(msg)
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    fun wipe(dir: File): Int {
        var count = 1
        if (dir.isDirectory) {
            val contents = dir.listFiles()
            if (contents != null && contents.size > 0) {
                for (fi in contents)
                    count += wipe(fi)
            }
        }
        dir.delete()
        return count
    }

    @Throws(Exception::class)
    private fun copyFileText(src: File, dst: File, interceptor: CopyInterceptor?) {
        var out: OutputStream? = null
        var writer: BufferedWriter? = null
        try {
            out = FileOutputStream(dst)
            writer = BufferedWriter(OutputStreamWriter(out))

            src.forEachLine(Charset.forName("UTF-8"), {
                val newLine = if (interceptor != null) interceptor.onCopyLine(src, it) else it;
                writer?.write(newLine)
                writer?.newLine()
            })
        } finally {
            Util.closeQuietely(writer)
            Util.closeQuietely(out)
        }
    }

    @Throws(Exception::class)
    private fun copyFileBinary(src: File, dst: File) {
        var out: OutputStream? = null
        try {
            out = FileOutputStream(dst)
            src.forEachBlock { bytes, size ->
                out?.write(bytes, 0, size)
            }
        } finally {
            Util.closeQuietely(out)
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    fun replaceInFile(file: File, find: String, replace: String, uiCallback: UICallback): Boolean {
        try {
            val path = Paths.get(file.absolutePath)
            var content = Files.readAllBytes(path)
            var contentStr = String(content, Charset.forName("UTF-8"))
            contentStr = contentStr.replace(find, replace)
            content = contentStr.toByteArray(Charset.forName("UTF-8"))
            file.delete()
            Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        } catch (t: Throwable) {
            t.printStackTrace()
            MainBase.LOG("[ERROR]: Failed to perform a find and replace in ${MainBase.cleanupPath(file.absolutePath)}: ${t.message}}")
            uiCallback.onErrorOccurred("Failed to perform a find and replace in ${MainBase.cleanupPath(file.absolutePath)}: ${t.message}}")
            return false
        }

        return true
    }

    // Checks for files in the project folder that no longer exist in the latest code
    @SuppressWarnings("ResultOfMethodCallIgnored")
    fun checkDiff(project: File, latest: File, importMode: Boolean, callback: UICallback, interceptor: (File) -> Boolean): Boolean {
        if (importMode) {
            if (project.isDirectory && interceptor(project))
                return true
            if (!project.exists() && latest.exists()) {
                MainBase.LOG("[ADD]: ${MainBase.cleanupPath(latest.absolutePath)} -> ${MainBase.cleanupPath(project.absolutePath)}...")
                val result = copyFolder(latest, project, object : CopyInterceptor {
                    override fun onCopyLine(file: File, line: String): String {
                        return line.replace("com.afollestad.polar", Main.USER_CODE_PACKAGE)
                    }

                    override fun loggingEnabled(): Boolean {
                        return true
                    }

                    override fun skip(file: File): Boolean {
                        return false
                    }
                }, callback)
                if (!result) return false
            }
            if (latest.isDirectory) {
                val files = latest.list()
                var result = true
                for (file in files) {
                    val srcFile = File(project, file)
                    val destFile = File(latest, file)
                    if (!checkDiff(srcFile, destFile, true, callback, interceptor)) {
                        result = false
                        break
                    }
                }
                return result
            }
            return true
        } else {
            if (interceptor(project))
                return true
            if (project.exists() && !latest.exists()) {
                MainBase.LOG("[DELETE]: %${MainBase.cleanupPath(project.absolutePath)}")
                if (project.isDirectory) {
                    wipe(project)
                } else {
                    project.delete()
                }
            } else if (project.isDirectory) {
                val files = project.list()
                var result = true
                for (file in files) {
                    val srcFile = File(project, file)
                    val destFile = File(latest, file)
                    if (!checkDiff(srcFile, destFile, false, callback, interceptor)) {
                        result = false
                    }
                }
                return result
            }
            return true
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    fun copyFolder(source: File, destination: File, interceptor: CopyInterceptor?, callback: UICallback?): Boolean {
        val cleanedSourcePath = MainBase.cleanupPath(source.absolutePath)
        val cleanedDestPath = MainBase.cleanupPath(destination.absolutePath)

        if (interceptor != null && interceptor.skip(source)) {
            if (interceptor.loggingEnabled())
                MainBase.LOG("[SKIP]: $cleanedSourcePath")
            return true
        }

        if (interceptor == null || interceptor.loggingEnabled())
            MainBase.LOG("[COPY]: $cleanedSourcePath -> $cleanedDestPath")
        if (source.isDirectory) {
            if (!destination.exists())
                destination.mkdirs()
            val files = source.list()
            for (file in files) {
                val srcFile = File(source, file)
                val destFile = File(destination, file)
                if (!copyFolder(srcFile, destFile, interceptor, callback))
                    return false
            }
            return true
        } else {
            try {
                val name = source.name.toLowerCase(Locale.getDefault())
                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) {
                    copyFileBinary(source, destination)
                } else {
                    copyFileText(source, destination, interceptor)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                MainBase.LOG("[ERROR]: An error occurred while copying $cleanedSourcePath: ${e.message}")
                callback?.onErrorOccurred("An error occurred while copying $cleanedSourcePath: ${e.message}")
                return false
            }

            return true
        }
    }
}

interface CopyInterceptor {
    fun skip(file: File): Boolean

    fun onCopyLine(file: File, line: String): String

    fun loggingEnabled(): Boolean
}