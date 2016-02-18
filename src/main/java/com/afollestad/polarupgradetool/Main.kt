package com.afollestad.polarupgradetool;

import com.afollestad.polarupgradetool.jfx.UICallback
import com.afollestad.polarupgradetool.utils.CopyInterceptor
import com.afollestad.polarupgradetool.utils.FileUtil
import com.afollestad.polarupgradetool.utils.Util
import com.afollestad.polarupgradetool.utils.ZipUtil
import com.afollestad.polarupgradetool.xml.XmlElementExtractor
import com.afollestad.polarupgradetool.xml.XmlMigrator
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * @author Aidan Follestad (afollestad)
 */
class Main : MainBase() {

    open class PackageCopyInterceptor : CopyInterceptor {
        override fun onCopyLine(file: File, line: String): String {
            try {
                return line.replace("com.afollestad.polar", USER_CODE_PACKAGE)
            } catch (t: Throwable) {
                t.printStackTrace()
                return line
            }

        }

        override fun skip(file: File): Boolean {
            return isBlacklisted(file)
        }

        override fun loggingEnabled(): Boolean {
            return true
        }
    }

    companion object {

        fun isBlacklisted(file: File): Boolean {
            if (file.isDirectory) {
                return file.name.startsWith("mipmap") ||
                        file.name == "drawable-nodpi" ||
                        file.name == "xml" ||
                        file.name == ".gradle" ||
                        file.name == ".idea" ||
                        file.name == "build" ||
                        file.name == ".DS_Store"
            } else {
                return file.name == "list_item_about_dev.xml" ||
                        file.name.startsWith("dev_") && file.name.endsWith(".xml") ||
                        file.name == "theme_config.xml" ||
                        file.name == "strings.xml" ||
                        file.name == "colors.xml" ||
                        file.name == "appfilter.xml" ||
                        file.name == "icon_pack.xml" ||
                        file.parent != null && file.parent == "drawable-nodpi"
            }
        }

        var USER_PACKAGE: String = ""
        var USER_VERSION_NAME: String = ""
        var USER_VERSION_CODE: String = ""
        var USER_APPNAME: String = ""
        var USER_CODE_PACKAGE: String = ""

        var OLD_ABOUT_BUTTON1_TEXT: String = ""
        var OLD_ABOUT_BUTTON1_LINK: String = ""
        var OLD_ABOUT_BUTTON2_TEXT: String = ""
        var OLD_ABOUT_BUTTON2_LINK: String = ""

        private val LIBS_FOLDER = File.separator + "app" + File.separator + "libs"
        private val LICENSING_MODULE_ROOT = File.separator + "licensing"
        private val GRADLE_FILE_PATH = File.separator + "app" + File.separator + "build.gradle"
        private val MAIN_FOLDER = File.separator + "app" + File.separator + "src" + File.separator + "main"
        private val JAVA_FOLDER_PATH = MAIN_FOLDER + File.separator + "java"
        val ASSETS_FOLDER_PATH = MAIN_FOLDER + File.separator + "assets"
        private val RES_FOLDER_PATH = MAIN_FOLDER + File.separator + "res"
        val VALUES_FOLDER_PATH = MAIN_FOLDER + File.separator + "res" + File.separator + "values"
        private val MANIFEST_FILE_PATH = MAIN_FOLDER + File.separator + "AndroidManifest.xml"

        @SuppressWarnings("ResultOfMethodCallIgnored")
        fun upgrade(projectPath: String, uiCallback: UICallback) {
            //CURRENT_DIR = new File(System.getProperty("user.dir"));
            MainBase.CURRENT_DIR = File(projectPath)
            println("\n--------------------------------------\n" +
                    "| Welcome to the Polar upgrade tool! |\n" +
                    "--------------------------------------")

            // Use app/build.gradle and /res/values/strings.xml to load info about icon pack
            val gradleFile = File(MainBase.CURRENT_DIR, GRADLE_FILE_PATH)
            val gradleExtractor = AttributeExtractor(gradleFile,
                    arrayOf("applicationId", "versionName", "versionCode"), AttributeExtractor.MODE_GRADLE, uiCallback)
            val stringsExtractor = XmlElementExtractor(File(MainBase.CURRENT_DIR,
                    "%s%s%s%s%s".format(RES_FOLDER_PATH, File.separator, "values", File.separator, "strings.xml")),
                    arrayOf("string"), arrayOf("app_name"), uiCallback)
            val gradleAttrs = gradleExtractor.find() ?: return
            val stringsAttrs = stringsExtractor.find() ?: return

            USER_APPNAME = stringsAttrs["app_name"] ?: "Unknown"
            USER_PACKAGE = gradleAttrs["applicationId"] ?: "Unknown"
            USER_VERSION_NAME = gradleAttrs["versionName"] ?: "Unknown"
            USER_VERSION_CODE = gradleAttrs["versionCode"] ?: "Unknown"
            MainBase.LOG("[DETECTED]: app_name = $USER_APPNAME, applicationId = $USER_PACKAGE, versionName = $USER_VERSION_NAME, versionCode = $USER_VERSION_CODE")
            uiCallback.onProjectDetected(USER_APPNAME, USER_PACKAGE, USER_VERSION_NAME, USER_VERSION_CODE)

            // Pull out package name used for files
            var source = File(MainBase.CURRENT_DIR, JAVA_FOLDER_PATH)
            USER_CODE_PACKAGE = Util.detectCodePackage(source)
            LOG("[INFO]: Code package = $USER_CODE_PACKAGE")

            var projectBackup = File(MainBase.CURRENT_DIR, "PolarLatest")
            if (projectBackup.exists()) FileUtil.wipe(projectBackup)
            projectBackup = File(MainBase.CURRENT_DIR, "${USER_APPNAME.replace(" ", "_")}-BACKUP.zip")
            if (projectBackup.exists()) projectBackup.delete()

            val cleanBackupPath = cleanupPath(projectBackup.absolutePath)
            LOG("[INFO]: Backing up your existing project to $cleanBackupPath...")
            uiCallback.onStatusUpdate("Backing up your existing project to $cleanBackupPath...")
            try {
                ZipUtil.writeZipFile(MainBase.CURRENT_DIR as File, projectBackup)
            } catch (e: Exception) {
                e.printStackTrace()
                MainBase.Companion.LOG("[ERROR]: Failed to make a backup of your project: ${e.message}")
                uiCallback.onErrorOccurred("Failed to make a backup of your project! ${e.message}")
                return
            }

            uiCallback.onStatusUpdate("Project backed up successfully!")

            // Pull out information about the designer for later use, if possible
            val localResFolder = File(MainBase.CURRENT_DIR, RES_FOLDER_PATH)
            val listItemDevAbout = File(File(localResFolder, "layout"), "list_item_about_dev.xml")
            if (listItemDevAbout.exists()) {
                MainBase.LOG("[INFO]: Extracting information from ${cleanupPath(listItemDevAbout.absolutePath)}")
                uiCallback.onStatusUpdate("Extracting information from ${cleanupPath(listItemDevAbout.absolutePath)}")
                try {
                    val contents = Files.readAllBytes(Paths.get(listItemDevAbout.absolutePath))
                    val contentsStr = String(contents, Charset.forName("UTF-8"))

                    val findStrOne = "android:tag=\""
                    var start = contentsStr.indexOf(findStrOne) + findStrOne.length
                    var end = contentsStr.indexOf("\"", start + 1)
                    OLD_ABOUT_BUTTON1_LINK = contentsStr.substring(start, end)
                    start = contentsStr.indexOf(findStrOne, end + 1) + findStrOne.length
                    end = contentsStr.indexOf("\"", start + 1)
                    OLD_ABOUT_BUTTON2_LINK = contentsStr.substring(start, end)

                    val findStrTwo = "android:text=\""
                    start = contentsStr.indexOf("<Button")
                    start = contentsStr.indexOf(findStrTwo, start) + findStrTwo.length
                    end = contentsStr.indexOf("\"", start + 1)
                    OLD_ABOUT_BUTTON1_TEXT = contentsStr.substring(start, end)
                    start = contentsStr.indexOf(findStrTwo, end + 1) + findStrTwo.length
                    end = contentsStr.indexOf("\"", start + 1)
                    OLD_ABOUT_BUTTON2_TEXT = contentsStr.substring(start, end)

                    val lookupNames = ArrayList<String>()
                    if (OLD_ABOUT_BUTTON1_TEXT.startsWith("@string/")) {
                        OLD_ABOUT_BUTTON1_TEXT = OLD_ABOUT_BUTTON1_TEXT.substring(
                                OLD_ABOUT_BUTTON1_TEXT.indexOf('/') + 1)
                        lookupNames.add(OLD_ABOUT_BUTTON1_TEXT)
                    }
                    if (OLD_ABOUT_BUTTON2_TEXT.startsWith("@string/")) {
                        OLD_ABOUT_BUTTON2_TEXT = OLD_ABOUT_BUTTON2_TEXT.substring(
                                OLD_ABOUT_BUTTON2_TEXT.indexOf('/') + 1)
                        lookupNames.add(OLD_ABOUT_BUTTON2_TEXT)
                    }

                    if (lookupNames.size > 0) {
                        val valuesFolder = File(localResFolder, "values")
                        val tagNames = Array(lookupNames.size, {
                            it ->
                            "string"
                        })
                        val lookupNamesAry = lookupNames.toArray<String>(arrayOfNulls<String>(lookupNames.size))
                        var extractor = XmlElementExtractor(
                                File(valuesFolder, "dev_about.xml"), tagNames, lookupNamesAry, uiCallback)
                        var result: HashMap<String, String>? = extractor.find()
                        if (result != null && result.size > 0) {
                            for (key in result.keys) {
                                if (key == OLD_ABOUT_BUTTON1_TEXT)
                                    OLD_ABOUT_BUTTON1_TEXT = result[key] ?: "Website"
                                else
                                    OLD_ABOUT_BUTTON2_TEXT = result[key] ?: "Google+"
                            }
                        } else {
                            // Try again with the other possible file
                            extractor = XmlElementExtractor(
                                    File(valuesFolder, "strings.xml"), tagNames, lookupNamesAry, uiCallback)
                            result = extractor.find()
                            if (result != null && result.size > 0) {
                                for (key in result.keys) {
                                    if (key == OLD_ABOUT_BUTTON1_TEXT)
                                        OLD_ABOUT_BUTTON1_TEXT = result[key] ?: "Website"
                                    else
                                        OLD_ABOUT_BUTTON2_TEXT = result[key] ?: "Google+"
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    val devAboutPath = listItemDevAbout.absolutePath
                    MainBase.Companion.LOG("[ERROR]: Failed to read ${cleanupPath(devAboutPath)}: ${e.message}")
                    uiCallback.onErrorOccurred("Failed to read ${cleanupPath(devAboutPath)}: ${e.message}")
                    return
                }

            }

            // Download latest code
            if (!downloadArchive(uiCallback)) return

            // Copy manifest
            source = File(MainBase.EXTRACTED_ZIP_ROOT, MANIFEST_FILE_PATH)
            var dest = File(MainBase.CURRENT_DIR, MANIFEST_FILE_PATH)
            MainBase.Companion.LOG("[MIGRATE]: AndroidManifest.xml...")
            uiCallback.onStatusUpdate("Migrating AndroidManifest.xml...")

            val templatesFolder = File(File(MainBase.CURRENT_DIR, ASSETS_FOLDER_PATH), "templates")
            if (!FileUtil.copyFolder(source, dest, object : PackageCopyInterceptor() {

                private var mZooper: Boolean = false

                override fun onCopyLine(file: File, line: String): String {
                    var newLine = super.onCopyLine(file, line)
                    if (newLine.contains("<!-- Uncomment for Zooper")) {
                        mZooper = true
                    } else if (mZooper) {
                        if (!newLine.contains("<!--")) {
                            mZooper = false
                        } else if (templatesFolder.exists()) {
                            newLine = newLine.replace("<!--", "").replace("-->", "")
                        }
                    }
                    return newLine
                }

                override fun loggingEnabled(): Boolean {
                    return false
                }
            }, uiCallback)) {
                return
            }

            // Copy build.gradle
            source = File(MainBase.CURRENT_DIR, GRADLE_FILE_PATH)
            dest = File(MainBase.EXTRACTED_ZIP_ROOT, GRADLE_FILE_PATH)
            val gradleMigrator = GradleMigrator(source, dest, uiCallback)
            if (!gradleMigrator.process()) return

            // Copy licensing module
            LOG("[MIGRATE]: licensing module...")
            uiCallback.onStatusUpdate("Migrating the licensing module...")
            source = File(MainBase.EXTRACTED_ZIP_ROOT, LICENSING_MODULE_ROOT)
            dest = File(MainBase.CURRENT_DIR, LICENSING_MODULE_ROOT)
            if (!FileUtil.copyFolder(source, dest, object : CopyInterceptor {
                override fun onCopyLine(file: File, line: String): String {
                    return line
                }

                override fun skip(file: File): Boolean {
                    return false
                }

                override fun loggingEnabled(): Boolean {
                    return false
                }
            }, uiCallback)) {
                return
            }

            MainBase.LOG("[MIGRATE]: app module...")
            uiCallback.onStatusUpdate("Migrating the app module...")
            println()

            // Perform renames to files in /res/values
            FileUtil.checkResRename("changelog.xml", "dev_changelog.xml", uiCallback)
            FileUtil.checkResRename("dev_options.xml", "dev_customization.xml", uiCallback)

            // Check for Java files that no longer exist in the latest code
            source = File(MainBase.EXTRACTED_ZIP_ROOT, JAVA_FOLDER_PATH)
            source = Util.skipPackage(source)
            dest = File(MainBase.CURRENT_DIR, JAVA_FOLDER_PATH)
            dest = Util.skipPackage(dest)
            if (!FileUtil.checkDiff(dest, source, false, uiCallback, { isBlacklisted(dest) })) return
            // Also check for Java files that don't exist in the project code but exist in the latest code
            if (!FileUtil.checkDiff(dest, source, true, uiCallback, { isBlacklisted(dest) })) return
            // Copy Java files
            if (!FileUtil.copyFolder(source, dest, PackageCopyInterceptor(), uiCallback)) return

            // Check for resource files that were deleted from the latest code
            source = File(MainBase.EXTRACTED_ZIP_ROOT, RES_FOLDER_PATH)
            dest = File(MainBase.CURRENT_DIR, RES_FOLDER_PATH)
            if (!FileUtil.checkDiff(dest, source, false, uiCallback, { isBlacklisted(dest) })) return
            // Also check for resource files that don't exist in the project code but exist in the latest code
            if (!FileUtil.checkDiff(dest, source, true, uiCallback, { isBlacklisted(dest) })) return
            // Copy resource files, minus blacklisted files
            if (!FileUtil.copyFolder(source, dest, PackageCopyInterceptor(), uiCallback)) return

            // Migrate the files ignored during direct copy
            val projectValues = File(File(MainBase.CURRENT_DIR, RES_FOLDER_PATH), "values")
            val latestValues = File(File(MainBase.EXTRACTED_ZIP_ROOT, RES_FOLDER_PATH), "values")

            // Migrate strings.xml
            var migrator = XmlMigrator(
                    File(projectValues, "strings.xml"), File(latestValues, "strings.xml"),
                    uiCallback)
            if (!migrator.process()) return

            // Migrate dev_ prefixed XML files
            val files = projectValues.list()
            var shouldReturn = false
            for (file in files) {
                if (file.startsWith("dev_") && file.endsWith(".xml") || file == "colors.xml") {
                    migrator = XmlMigrator(
                            File(projectValues, file), File(latestValues, file), uiCallback)
                    if (!migrator.process()) {
                        shouldReturn = true
                        break
                    }
                }
            }
            if (shouldReturn) return

            val layoutDir = File(File(MainBase.CURRENT_DIR, RES_FOLDER_PATH), "layout")
            var file = File(layoutDir, "list_item_about_dev.xml")
            if (file.exists()) {
                LOG("[DELETE]: ${cleanupPath(file.absolutePath)}")
                file.delete()
            }
            file = File(layoutDir, "list_item_about_aidan.xml")
            if (file.exists()) {
                LOG("[DELETE]: ${cleanupPath(file.absolutePath)}")
                file.delete()
            }
            file = File(layoutDir, "list_item_about_tom.xml")
            if (file.exists()) {
                LOG("[DELETE]: ${cleanupPath(file.absolutePath)}")
                file.delete()
            }
            file = File(layoutDir, "list_item_about_daniel.xml")
            if (file.exists()) {
                LOG("[DELETE]: ${cleanupPath(file.absolutePath)}")
                file.delete()
            }
            file = File(MainBase.CURRENT_DIR, LIBS_FOLDER)
            if (file.exists()) {
                LOG("[DELETE]: ${cleanupPath(file.absolutePath)}")
                FileUtil.wipe(file)
            }

            val assetsDir = File(File(MainBase.CURRENT_DIR, MAIN_FOLDER), "assets")
            if (!FileUtil.replaceInFile(File(assetsDir, "themecfg.xml"), "Unnamed", USER_APPNAME, uiCallback))
                return
            if (!FileUtil.replaceInFile(File(assetsDir, "themeinfo.xml"), "Unnamed", USER_APPNAME, uiCallback))
                return
            if (!FileUtil.replaceInFile(File(assetsDir, "themeinfo.xml"), "com.afollestad.polar", USER_PACKAGE, uiCallback))
                return

            val xmlDir = File(File(MainBase.CURRENT_DIR, RES_FOLDER_PATH), "xml")
            if (!FileUtil.replaceInFile(File(xmlDir, "themecfg.xml"), "Unnamed", USER_APPNAME, uiCallback))
                return
            if (!FileUtil.replaceInFile(File(xmlDir, "themeinfo.xml"), "Unnamed", USER_APPNAME, uiCallback))
                return
            if (!FileUtil.replaceInFile(File(xmlDir, "themeinfo.xml"), "com.afollestad.polar", USER_PACKAGE, uiCallback))
                return

            println("\nUpgrade is complete for %s!".format(USER_APPNAME))
            uiCallback.onStatusUpdate("Upgrade is complete for %s!".format(USER_APPNAME))
            if (EXTRACTED_ZIP_ROOT != null)
                EXTRACTED_ZIP_ROOT!!.delete()
            uiCallback.onUpdateSuccessful()
        }
    }
}