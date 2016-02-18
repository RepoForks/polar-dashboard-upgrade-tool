package com.afollestad.polarupgradetool.xml

import com.afollestad.polarupgradetool.AttributeExtractor
import com.afollestad.polarupgradetool.Main
import com.afollestad.polarupgradetool.MainBase
import com.afollestad.polarupgradetool.jfx.UICallback
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * @author Aidan Follestad (afollestad)
 */
class XmlMigrator(private val mProject: File, private val mLatest: File, private val uiCallback: UICallback?) {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    fun process(): Boolean {
        val cleanedProjectPath = MainBase.cleanupPath(mProject.absolutePath)
        val cleanedLatestPath = MainBase.cleanupPath(mLatest.absolutePath)

        if (!mProject.exists()) {
            MainBase.LOG("[ERROR]: $cleanedProjectPath} doesn't exist.")
            uiCallback?.onErrorOccurred("$cleanedProjectPath doesn't exist.")
            return false
        } else if (!mLatest.exists()) {
            MainBase.LOG("[ERROR]: $cleanedLatestPath doesn't exist.")
            uiCallback?.onErrorOccurred("$cleanedLatestPath doesn't exist.")
            return false
        }

        val mSourceValues = HashMap<String, String>()

        // Read the project (local) file to pull out the user's current configuration
        try {
            val fileRaw = Files.readAllBytes(Paths.get(mProject.absolutePath))
            val fileContent = StringBuilder(String(fileRaw, Charset.forName("UTF-8")))
            val scanner = XmlScanner(fileContent)

            while (!scanner.reachedEnd()) {
                val tag = scanner.nextTag() ?: continue
                val attributeName = AttributeExtractor.getAttributeValue("name", tag)
                val tagValue = scanner.tagValue()
                mSourceValues.put(attributeName!!, tagValue)
            }
        } catch (e: Exception) {
            MainBase.LOG("[ERROR]: Failed to process $cleanedProjectPath for XML migration: ${e.message}")
            uiCallback?.onErrorOccurred("Failed to process $cleanedProjectPath for XML migration: ${e.message}")
            e.printStackTrace()
            return false
        }

        if (mProject.name == "dev_customization.xml") {
            // If project defaults are used in project file, set them to empty/null
            if (!mSourceValues.containsKey("wallpapers_json_url")) {
                mSourceValues.put("wallpapers_json_url", "")
            }
            if (!mSourceValues.containsKey("icon_request_email") || mSourceValues["icon_request_email"] == "fake-email@fake-website.com") {
                mSourceValues.put("icon_request_email", "")
            }
            if (!mSourceValues.containsKey("donate_license_key")) {
                mSourceValues.put("donate_license_key", "")
            }
            if (!mSourceValues.containsKey("feedback_email") || mSourceValues["feedback_email"] == "fake-email@fake-website.com") {
                mSourceValues.put("feedback_email", mSourceValues["icon_request_email"]!!)
            }
            if (!mSourceValues.containsKey("homepage_landing_icon")) {
                mSourceValues.put("homepage_landing_icon", "@mipmap/ic_launcher")
            }
        }
        if (mProject.name == "dev_zooper.xml") {
            if (!mSourceValues.containsKey("enable_zooper_page")) {
                val assetsFolder = File(MainBase.CURRENT_DIR, Main.ASSETS_FOLDER_PATH)
                val templatesFolder = File(assetsFolder, "templates")
                val list = templatesFolder.list()
                mSourceValues.put("enable_zooper_page", if (list != null && list.size > 0) "true" else "false")
            }
        }

        // Put original project configuration back where possible, leaving new configuration added
        val newFileContent: StringBuilder
        try {
            val fileRaw = Files.readAllBytes(Paths.get(mLatest.absolutePath))
            newFileContent = StringBuilder(String(fileRaw, Charset.forName("UTF-8")))
            val scanner = XmlScanner(newFileContent)

            while (!scanner.reachedEnd()) {
                val tag = scanner.nextTag() ?: continue
                val attributeName = AttributeExtractor.getAttributeValue("name", tag) ?: continue
                if (mSourceValues.containsKey(attributeName)) {
                    scanner.setElementValue(mSourceValues[attributeName]!!)
                } else if (attributeName == "about_buttons_names" || attributeName == "about_buttons_links") {
                    if (!Main.OLD_ABOUT_BUTTON1_TEXT.isEmpty() && !Main.OLD_ABOUT_BUTTON2_TEXT.isEmpty() ||
                            !Main.OLD_ABOUT_BUTTON1_LINK.isEmpty() && !Main.OLD_ABOUT_BUTTON2_LINK.isEmpty()) {
                        val value = StringBuilder(scanner.tagValue())
                        val start = value.indexOf("<item>") + "<item>".length
                        val end = value.indexOf("</item>", start)
                        if (attributeName == "about_buttons_names" && !Main.OLD_ABOUT_BUTTON1_TEXT.isEmpty()) {
                            value.replace(start, end, "%s|%s".format(Main.OLD_ABOUT_BUTTON1_TEXT, Main.OLD_ABOUT_BUTTON2_TEXT))
                        } else if (attributeName == "about_buttons_links" && !Main.OLD_ABOUT_BUTTON1_LINK.isEmpty()) {
                            value.replace(start, end, "%s|%s".format(Main.OLD_ABOUT_BUTTON1_LINK, Main.OLD_ABOUT_BUTTON2_LINK))
                        }
                        scanner.setElementValue(value.toString())
                    }
                }
            }
        } catch (e: Exception) {
            MainBase.LOG("[ERROR]: Failed to process $cleanedProjectPath for XML migration: ${e.message}")
            uiCallback?.onErrorOccurred("Failed to process $cleanedProjectPath for XML migration: ${e.message}")
            e.printStackTrace()
            return false
        }

        // Write the latest (remote) file's changed contents to the project (local) file
        try {
            mProject.delete()
            mProject.writeBytes(newFileContent.toString().toByteArray(Charset.forName("UTF-8")))
        } catch (e: Exception) {
            e.printStackTrace()
            MainBase.LOG("[ERROR]: Failed to write to $cleanedProjectPath: ${e.message}")
            uiCallback?.onErrorOccurred("Failed to write to $cleanedProjectPath: ${e.message}")
            e.printStackTrace()
            return false
        }

        MainBase.LOG("[MIGRATE]: $cleanedProjectPath")
        uiCallback?.onStatusUpdate("Migrated XML resource file: $cleanedProjectPath")
        return true
    }
}