package com.afollestad.polarupgradetool

import com.afollestad.polarupgradetool.jfx.UICallback
import com.afollestad.polarupgradetool.utils.Util
import java.io.*
import java.nio.charset.Charset
import java.util.*

/**
 * @author Aidan Follestad (afollestad)
 */
class GradleMigrator(private val mProject: File, private val mLatest: File, private val uiCallback: UICallback?) {

    private fun processLineProperty(propertyName: String, line: String, propertyValue: String): String {
        val start = line.indexOf("$propertyName ")
        if (start == -1) return line
        return "        $propertyName $propertyValue"
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    fun process(): Boolean {
        val lines = ArrayList<String>()
        try {
            mLatest.forEachLine(Charset.forName("UTF-8"), {
                var line = it
                line = line.replace("output.outputFile.parent, \"MyPolarPack",
                        "output.outputFile.parent, \"${Main.USER_APPNAME}")
                line = processLineProperty("applicationId", line, "\"${Main.USER_PACKAGE}\"")
                line = processLineProperty("versionName", line, "\"${Main.USER_VERSION_NAME}\"")
                line = processLineProperty("versionCode", line, Main.USER_VERSION_CODE)
                lines.add(line)
            })
        } catch (e: Exception) {
            MainBase.LOG("[ERROR]: Failed to migrate a Gradle file: ${e.message}")
            uiCallback?.onErrorOccurred("Failed to migrate Gradle file: ${e.message}")
            e.printStackTrace()
            return false
        }

        mProject.delete()
        var os: OutputStream? = null
        var writer: BufferedWriter? = null

        try {
            os = FileOutputStream(mProject)
            writer = BufferedWriter(OutputStreamWriter(os))

            for (i in lines.indices) {
                if (i > 0) writer.newLine()
                writer.write(lines[i])
            }
        } catch (e: Exception) {
            MainBase.LOG("[ERROR]: Failed to migrate a Gradle file: ${e.message}")
            uiCallback?.onErrorOccurred("Failed to migrate Gradle file: ${e.message}")
            e.printStackTrace()
            return false
        } finally {
            Util.closeQuietely(writer)
            Util.closeQuietely(os)
        }

        val cleanedPath = MainBase.cleanupPath(mProject.absolutePath)
        MainBase.LOG("[INFO]: Migrated Gradle file $cleanedPath")
        uiCallback?.onStatusUpdate("Migrated Gradle file: $cleanedPath")
        return true
    }
}