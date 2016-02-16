package com.afollestad.polarupgradetool.utils

import java.io.Closeable
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * @author Aidan Follestad (afollestad)
 */
object Util {

    fun closeQuietely(c: Closeable?) {
        if (c == null) return
        try {
            c.close()
        } catch (ignored: Throwable) {
        }

    }

    fun round(value: Double): String {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(value)
    }

    fun round(value: Float): String {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(value.toDouble())
    }

    fun readableFileSizeMB(size: Long): String {
        val value = size.toDouble() / 1000000
        return "%sMB".format(round(value))
    }

    fun detectCodePackage(folder: File): String {
        var javaFolder = folder
        var pkg = ""

        var contents: Array<out File>? = javaFolder.listFiles() ?: return pkg
        // com
        javaFolder = contents!![0]
        pkg += javaFolder.name

        contents = javaFolder.listFiles()
        if (contents == null) return pkg
        // afollestad
        javaFolder = contents[0]
        pkg += "." + javaFolder.name

        contents = javaFolder.listFiles()
        if (contents == null) return pkg
        // polar
        javaFolder = contents[0]
        pkg += "." + javaFolder.name

        return pkg
    }

    fun skipPackage(folder: File): File {
        var javaFolder = folder
        var contents: Array<out File>? = javaFolder.listFiles() ?: return javaFolder
        // com
        javaFolder = contents!![0]

        contents = javaFolder.listFiles()
        if (contents == null) return javaFolder
        // afollestad
        javaFolder = contents[0]

        contents = javaFolder.listFiles()
        if (contents == null) return javaFolder
        // polar
        javaFolder = contents[0]

        return javaFolder
    }
}