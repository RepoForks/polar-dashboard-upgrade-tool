package com.afollestad.polarupgradetool

import com.afollestad.polarupgradetool.jfx.UICallback
import java.io.File
import java.nio.charset.Charset
import java.util.*
import java.util.regex.Pattern

/**
 * @author Aidan Follestad (afollestad)
 */
class AttributeExtractor(private val mFile: File, private val mAttributeNames: Array<String>, private val mMode: Int, private val uiCallback: UICallback?) {

    fun find(): HashMap<String, String>? {
        if (!mFile.exists()) {
            MainBase.LOG("[ERROR]: File ${mFile.absolutePath} does not exist.")
            uiCallback?.onErrorOccurred("File does not exist: ${mFile.absolutePath}.")
            return null
        }

        var patterns: Array<Pattern>? = null
        if (mMode == MODE_XML) {
            patterns = Array<Pattern>(mAttributeNames.size, {
                it ->
                Pattern.compile(XML_REGEX.format(mAttributeNames[it]))
            })
        }

        val results = HashMap<String, String>(mAttributeNames.size)

        try {
            mFile.forEachLine(Charset.forName("UTF-8"), {
                if (patterns != null) {
                    // XML
                    for (pattern in patterns!!) {
                        val matcher = pattern.matcher(it)
                        if (matcher.find()) {
                            var result = it.substring(matcher.start(), matcher.end())
                            val name = result.substring(0, result.indexOf('='))
                            result = result.substring(result.indexOf('=') + 1, result.length)
                            if (result.startsWith("\"") && result.endsWith("\"") || result.startsWith("'") && result.endsWith("'")) {
                                result = result.substring(1, result.length - 1)
                            }
                            results.put(name, result)
                            break
                        }
                    }
                } else {
                    // Gradle
                    for (attr in mAttributeNames) {
                        val start = it.indexOf(attr + " ")
                        if (start == -1) continue
                        var result = it.substring(start, it.length).trim { it <= ' ' }
                        val name = result.substring(0, result.indexOf(' '))
                        result = result.substring(result.indexOf(' ') + 1)
                        if (result.startsWith("\"") && result.endsWith("\"") || result.startsWith("'") && result.endsWith("'")) {
                            result = result.substring(1, result.length - 1)
                        }
                        results.put(name, result)
                    }
                }
            })
        } catch (e: Exception) {
            MainBase.LOG("[ERROR] Failed to read ${mFile.absolutePath}: ${e.message}")
            uiCallback?.onErrorOccurred("Failed to read ${mFile.absolutePath}: ${e.message}")
            e.printStackTrace()
            return null
        }
        return results
    }

    companion object {

        private val XML_REGEX = "%s=[\"']?((?:.(?![\"']?\\s+(?:\\S+)=|[>\"']))+.)[\"']?"

        val MODE_XML = 1
        val MODE_GRADLE = 2

        fun getAttributeValue(name: String, tag: String): String? {
            val pattern = Pattern.compile(XML_REGEX.format(name))
            val matcher = pattern.matcher(tag)
            if (matcher.find()) {
                var result = tag.substring(matcher.start(), matcher.end())
                result = result.substring(result.indexOf('=') + 1, result.length)
                if (result.startsWith("\"") && result.endsWith("\""))
                    result = result.substring(1, result.length - 1)
                return result
            }
            return null
        }

        fun getElementValue(tag: String): String? {
            try {
                val start = tag.indexOf('>')
                if (start < 0) return null
                val end = tag.lastIndexOf("</")
                if (end < 0) return null
                return tag.substring(start + 1, end)
            } catch (t: Throwable) {
                return null
            }
        }
    }
}