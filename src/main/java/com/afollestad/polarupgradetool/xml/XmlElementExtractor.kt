package com.afollestad.polarupgradetool.xml

import com.afollestad.polarupgradetool.AttributeExtractor
import com.afollestad.polarupgradetool.MainBase
import com.afollestad.polarupgradetool.jfx.UICallback
import java.io.File
import java.nio.charset.Charset
import java.util.*
import java.util.regex.Pattern

/**
 * @author Aidan Follestad (afollestad)
 */
class XmlElementExtractor(private val mFile: File, tagNames: Array<String>?, names: Array<String>?, private val uiCallback: UICallback?) {

    private val REGEX = "<%s name=\"%s\">[\\s\\S\\n]*<\\/%s>"
    private val mPatterns: Array<Pattern>

    init {
        if (tagNames == null || names == null || tagNames.size != names.size)
            throw IllegalArgumentException("tagNames and names must be non-null matching length arrays.")
        mPatterns = Array<Pattern>(tagNames.size, {
            it ->
            Pattern.compile(REGEX.format(tagNames[it], names[it], tagNames[it]))
        })
    }

    fun find(): HashMap<String, String>? {
        if (!mFile.exists()) {
            MainBase.LOG("[ERROR]: File ${mFile.absolutePath} does not exist.")
            uiCallback?.onErrorOccurred("File does not exist: ${mFile.absoluteFile}")
            return null
        }

        val result = HashMap<String, String>(mPatterns.size)

        try {
            mFile.forEachLine(Charset.forName("UTF-8"), {
                for (p in mPatterns) {
                    val matcher = p.matcher(it)
                    if (matcher.find()) {
                        val name = AttributeExtractor.getAttributeValue("name", it)
                        val value = AttributeExtractor.getElementValue(it)
                        result.put(name!!, value!!)
                        break
                    }
                }
            })
        } catch (e: Exception) {
            MainBase.LOG("[ERROR] Failed to read ${mFile.absolutePath}: ${e.message}")
            uiCallback?.onErrorOccurred("Failed to read ${mFile.absolutePath}: ${e.message}")
            e.printStackTrace()
            return null
        }

        return result
    }
}