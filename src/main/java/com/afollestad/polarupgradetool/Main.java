package com.afollestad.polarupgradetool;

import com.afollestad.polarupgradetool.jfx.UICallback;
import com.afollestad.polarupgradetool.utils.FileUtil;
import com.afollestad.polarupgradetool.utils.Util;
import com.afollestad.polarupgradetool.utils.ZipUtil;
import com.afollestad.polarupgradetool.xml.XmlElementExtractor;
import com.afollestad.polarupgradetool.xml.XmlMigrator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Main extends MainBase {

    public static String USER_PACKAGE;
    public static String USER_VERSION_NAME;
    public static String USER_VERSION_CODE;
    public static String USER_APPNAME;
    public static String USER_CODE_PACKAGE;

    public static String OLD_ABOUT_BUTTON1_TEXT;
    public static String OLD_ABOUT_BUTTON1_LINK;
    public static String OLD_ABOUT_BUTTON2_TEXT;
    public static String OLD_ABOUT_BUTTON2_LINK;

    private final static String LIBS_FOLDER = File.separator + "app" + File.separator + "libs";
    private final static String LICENSING_MODULE_ROOT = File.separator + "licensing";
    private final static String GRADLE_FILE_PATH = File.separator + "app" + File.separator + "build.gradle";
    private final static String MAIN_FOLDER = File.separator + "app" + File.separator + "src" + File.separator + "main";
    private final static String JAVA_FOLDER_PATH = MAIN_FOLDER + File.separator + "java";
    private final static String RES_FOLDER_PATH = MAIN_FOLDER + File.separator + "res";
    private final static String VALUES_FOLDER_PATH = MAIN_FOLDER + File.separator + "res" + File.separator + "values";
    private final static String MANIFEST_FILE_PATH = MAIN_FOLDER + File.separator + "AndroidManifest.xml";

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void upgrade(String projectPath, UICallback uiCallback) {
        //CURRENT_DIR = new File(System.getProperty("user.dir"));
        CURRENT_DIR = new File(projectPath);
        System.out.println("\n--------------------------------------\n" +
                "| Welcome to the Polar upgrade tool! |\n" +
                "--------------------------------------");

        // Use app/build.gradle and /res/values/strings.xml to load info about icon pack
        File gradleFile = new File(CURRENT_DIR, GRADLE_FILE_PATH);
        AttributeExtractor gradleExtractor = new AttributeExtractor(gradleFile,
                new String[]{"applicationId", "versionName", "versionCode"}, AttributeExtractor.MODE_GRADLE, uiCallback);
        XmlElementExtractor stringsExtractor = new XmlElementExtractor(new File(CURRENT_DIR,
                String.format("%s%s%s%s%s", RES_FOLDER_PATH, File.separator, "values", File.separator, "strings.xml")),
                new String[]{"string"}, new String[]{"app_name"}, uiCallback);
        HashMap<String, String> gradleAttrs = gradleExtractor.find();
        if (gradleAttrs == null) return;
        HashMap<String, String> stringsAttrs = stringsExtractor.find();
        if (stringsAttrs == null) return;

        USER_APPNAME = stringsAttrs.get("app_name");
        USER_PACKAGE = gradleAttrs.get("applicationId");
        USER_VERSION_NAME = gradleAttrs.get("versionName");
        USER_VERSION_CODE = gradleAttrs.get("versionCode");
        LOG("[DETECTED]: app_name = %s, applicationId = %s, versionName = %s, versionCode = %s",
                USER_APPNAME, USER_PACKAGE, USER_VERSION_NAME, USER_VERSION_CODE);
        uiCallback.onProjectDetected(USER_APPNAME, USER_PACKAGE, USER_VERSION_NAME, USER_VERSION_CODE);

        // Pull out package name used for files
        File source = new File(CURRENT_DIR, JAVA_FOLDER_PATH);
        USER_CODE_PACKAGE = Util.detectCodePackage(source);
        Main.LOG("[INFO]: Code package = %s", USER_CODE_PACKAGE);

        File projectBackup = new File(CURRENT_DIR, "PolarLatest");
        if (projectBackup.exists())
            FileUtil.wipe(projectBackup);
        projectBackup = new File(CURRENT_DIR,
                String.format("%s-BACKUP.zip", USER_APPNAME.replace(" ", "_")));
        if (projectBackup.exists())
            projectBackup.delete();

        LOG("[INFO]: Backing up your existing project to %s...", Main.cleanupPath(projectBackup.getAbsolutePath()));
        uiCallback.onStatusUpdate(String.format("Backing up your existing project to %s...", Main.cleanupPath(projectBackup.getAbsolutePath())));
        try {
            ZipUtil.writeZipFile(CURRENT_DIR, projectBackup);
        } catch (Exception e) {
            e.printStackTrace();
            LOG("[ERROR]: Failed to make a backup of your project: %s", e.getMessage());
            uiCallback.onErrorOccurred("Failed to make a backup of your project! " + e.getMessage());
            return;
        }
        uiCallback.onStatusUpdate("Project backed up successfully!");

        // Pull out information about the designer for later use, if possible
        final File localResFolder = new File(CURRENT_DIR, RES_FOLDER_PATH);
        final File listItemDevAbout = new File(new File(localResFolder, "layout"), "list_item_about_dev.xml");
        if (listItemDevAbout.exists()) {
            LOG("[INFO]: Extracting information from %s", cleanupPath(listItemDevAbout.getAbsolutePath()));
            uiCallback.onStatusUpdate("Extracting information from " + cleanupPath(listItemDevAbout.getAbsolutePath()));
            try {
                final byte[] contents = Files.readAllBytes(Paths.get(listItemDevAbout.getAbsolutePath()));
                final String contentsStr = new String(contents, "UTF-8");

                final String findStrOne = "android:tag=\"";
                int start = contentsStr.indexOf(findStrOne) + findStrOne.length();
                int end = contentsStr.indexOf("\"", start + 1);
                OLD_ABOUT_BUTTON1_LINK = contentsStr.substring(start, end);
                start = contentsStr.indexOf(findStrOne, end + 1) + findStrOne.length();
                end = contentsStr.indexOf("\"", start + 1);
                OLD_ABOUT_BUTTON2_LINK = contentsStr.substring(start, end);

                final String findStrTwo = "android:text=\"";
                start = contentsStr.indexOf("<Button");
                start = contentsStr.indexOf(findStrTwo, start) + findStrTwo.length();
                end = contentsStr.indexOf("\"", start + 1);
                OLD_ABOUT_BUTTON1_TEXT = contentsStr.substring(start, end);
                start = contentsStr.indexOf(findStrTwo, end + 1) + findStrTwo.length();
                end = contentsStr.indexOf("\"", start + 1);
                OLD_ABOUT_BUTTON2_TEXT = contentsStr.substring(start, end);

                final ArrayList<String> lookupNames = new ArrayList<>();
                if (OLD_ABOUT_BUTTON1_TEXT.startsWith("@string/")) {
                    OLD_ABOUT_BUTTON1_TEXT = OLD_ABOUT_BUTTON1_TEXT.substring(
                            OLD_ABOUT_BUTTON1_TEXT.indexOf('/') + 1);
                    lookupNames.add(OLD_ABOUT_BUTTON1_TEXT);
                }
                if (OLD_ABOUT_BUTTON2_TEXT.startsWith("@string/")) {
                    OLD_ABOUT_BUTTON2_TEXT = OLD_ABOUT_BUTTON2_TEXT.substring(
                            OLD_ABOUT_BUTTON2_TEXT.indexOf('/') + 1);
                    lookupNames.add(OLD_ABOUT_BUTTON2_TEXT);
                }
                if (lookupNames.size() > 0) {
                    final File valuesFolder = new File(localResFolder, "values");
                    final String[] tagNames = new String[lookupNames.size()];
                    final String[] lookupNamesAry = lookupNames.toArray(new String[lookupNames.size()]);
                    for (int i = 0; i < tagNames.length; i++) tagNames[i] = "string";
                    XmlElementExtractor extractor = new XmlElementExtractor(
                            new File(valuesFolder, "dev_about.xml"), tagNames, lookupNamesAry, uiCallback);
                    HashMap<String, String> result = extractor.find();
                    if (result != null && result.size() > 0) {
                        for (String key : result.keySet()) {
                            if (key.equals(OLD_ABOUT_BUTTON1_TEXT))
                                OLD_ABOUT_BUTTON1_TEXT = result.get(key);
                            else OLD_ABOUT_BUTTON2_TEXT = result.get(key);
                        }
                    } else {
                        // Try again with the other possible file
                        extractor = new XmlElementExtractor(
                                new File(valuesFolder, "strings.xml"), tagNames, lookupNamesAry, uiCallback);
                        result = extractor.find();
                        if (result != null && result.size() > 0) {
                            for (String key : result.keySet()) {
                                if (key.equals(OLD_ABOUT_BUTTON1_TEXT))
                                    OLD_ABOUT_BUTTON1_TEXT = result.get(key);
                                else OLD_ABOUT_BUTTON2_TEXT = result.get(key);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOG("[ERROR]: Failed to read %s: %s", cleanupPath(listItemDevAbout.getAbsolutePath()), e.getMessage());
                uiCallback.onErrorOccurred("Failed to read " + cleanupPath(listItemDevAbout.getAbsolutePath()));
                return;
            }
        }

        // Download latest code
        if (!downloadArchive(uiCallback)) return;

        // Copy manifest
        source = new File(EXTRACTED_ZIP_ROOT, MANIFEST_FILE_PATH);
        File dest = new File(CURRENT_DIR, MANIFEST_FILE_PATH);
        LOG("[MIGRATE]: AndroidManifest.xml...");
        uiCallback.onStatusUpdate("Migrating AndroidManifest.xml...");

        if (!FileUtil.copyFolder(source, dest, new PackageCopyInterceptor() {
            @Override
            public boolean loggingEnabled() {
                return false;
            }
        }, uiCallback)) {
            return;
        }

        // Copy build.gradle
        source = new File(CURRENT_DIR, GRADLE_FILE_PATH);
        dest = new File(EXTRACTED_ZIP_ROOT, GRADLE_FILE_PATH);
        GradleMigrator gradleMigrator = new GradleMigrator(source, dest, uiCallback);
        if (!gradleMigrator.process()) return;

        // Copy licensing module
        LOG("[MIGRATE]: licensing module...");
        uiCallback.onStatusUpdate("Migrating the licensing module...");
        source = new File(EXTRACTED_ZIP_ROOT, LICENSING_MODULE_ROOT);
        dest = new File(CURRENT_DIR, LICENSING_MODULE_ROOT);
        if (!FileUtil.copyFolder(source, dest, new FileUtil.CopyInterceptor() {
            @Override
            public String onCopyLine(File file, String line) {
                return line;
            }

            @Override
            public boolean skip(File file) {
                return false;
            }

            @Override
            public boolean loggingEnabled() {
                return false;
            }
        }, uiCallback)) {
            return;
        }

        LOG("[MIGRATE]: app module...");
        uiCallback.onStatusUpdate("Migrating the app module...");
        System.out.println();

        // Perform renames to files in /res/values
        FileUtil.checkResRename("changelog.xml", "dev_changelog.xml", uiCallback);
        FileUtil.checkResRename("dev_options.xml", "dev_customization.xml", uiCallback);

        // Check for Java files that no longer exist in the latest code
        source = new File(EXTRACTED_ZIP_ROOT, JAVA_FOLDER_PATH);
        source = Util.skipPackage(source);
        dest = new File(CURRENT_DIR, JAVA_FOLDER_PATH);
        dest = Util.skipPackage(dest);
        if (!FileUtil.checkDiff(dest, source, Main::isBlacklisted, false, uiCallback)) return;
        // Also check for Java files that don't exist in the project code but exist in the latest code
        if (!FileUtil.checkDiff(dest, source, Main::isBlacklisted, true, uiCallback)) return;
        // Copy Java files
        if (!FileUtil.copyFolder(source, dest, new PackageCopyInterceptor(), uiCallback)) return;

        // Check for resource files that were deleted from the latest code
        source = new File(EXTRACTED_ZIP_ROOT, RES_FOLDER_PATH);
        dest = new File(CURRENT_DIR, RES_FOLDER_PATH);
        if (!FileUtil.checkDiff(dest, source, Main::isBlacklisted, false, uiCallback)) return;
        // Also check for resource files that don't exist in the project code but exist in the latest code
        if (!FileUtil.checkDiff(dest, source, Main::isBlacklisted, true, uiCallback)) return;
        // Copy resource files, minus blacklisted files
        if (!FileUtil.copyFolder(source, dest, new PackageCopyInterceptor(), uiCallback)) return;

        // Migrate the files ignored during direct copy
        final File projectValues = new File(new File(CURRENT_DIR, RES_FOLDER_PATH), "values");
        final File latestValues = new File(new File(EXTRACTED_ZIP_ROOT, RES_FOLDER_PATH), "values");

        // Migrate strings.xml
        XmlMigrator migrator = new XmlMigrator(
                new File(projectValues, "strings.xml"), new File(latestValues, "strings.xml"),
                uiCallback);
        if (!migrator.process()) return;

        // Migrate dev_ prefixed XML files
        String[] files = projectValues.list();
        boolean shouldReturn = false;
        for (String file : files) {
            if (file.startsWith("dev_") && file.endsWith(".xml")) {
                migrator = new XmlMigrator(
                        new File(projectValues, file), new File(latestValues, file), uiCallback);
                if (!migrator.process()) {
                    shouldReturn = true;
                    break;
                }
            }
        }
        if (shouldReturn) return;

        final File layoutDir = new File(new File(CURRENT_DIR, RES_FOLDER_PATH), "layout");
        File file = new File(layoutDir, "list_item_about_dev.xml");
        if (file.exists()) {
            Main.LOG("[DELETE]: %s", cleanupPath(file.getAbsolutePath()));
            file.delete();
        }
        file = new File(layoutDir, "list_item_about_aidan.xml");
        if (file.exists()) {
            Main.LOG("[DELETE]: %s", cleanupPath(file.getAbsolutePath()));
            file.delete();
        }
        file = new File(layoutDir, "list_item_about_tom.xml");
        if (file.exists()) {
            Main.LOG("[DELETE]: %s", cleanupPath(file.getAbsolutePath()));
            file.delete();
        }
        file = new File(layoutDir, "list_item_about_daniel.xml");
        if (file.exists()) {
            Main.LOG("[DELETE]: %s", cleanupPath(file.getAbsolutePath()));
            file.delete();
        }
        file = new File(CURRENT_DIR, LIBS_FOLDER);
        if (file.exists()) {
            Main.LOG("[DELETE]: %s", cleanupPath(file.getAbsolutePath()));
            FileUtil.wipe(file);
        }

        final File assetsDir = new File(new File(CURRENT_DIR, MAIN_FOLDER), "assets");
        if (!FileUtil.replaceInFile(new File(assetsDir, "themecfg.xml"), "Unnamed", USER_APPNAME, uiCallback))
            return;
        if (!FileUtil.replaceInFile(new File(assetsDir, "themeinfo.xml"), "Unnamed", USER_APPNAME, uiCallback))
            return;
        if (!FileUtil.replaceInFile(new File(assetsDir, "themeinfo.xml"), "com.afollestad.polar", USER_PACKAGE, uiCallback))
            return;

        final File xmlDir = new File(new File(CURRENT_DIR, RES_FOLDER_PATH), "xml");
        if (!FileUtil.replaceInFile(new File(xmlDir, "themecfg.xml"), "Unnamed", USER_APPNAME, uiCallback))
            return;
        if (!FileUtil.replaceInFile(new File(xmlDir, "themeinfo.xml"), "Unnamed", USER_APPNAME, uiCallback))
            return;
        if (!FileUtil.replaceInFile(new File(xmlDir, "themeinfo.xml"), "com.afollestad.polar", USER_PACKAGE, uiCallback))
            return;

        System.out.println(String.format("\nUpgrade is complete for %s!", USER_APPNAME));
        uiCallback.onStatusUpdate(String.format("Upgrade is complete for %s!", USER_APPNAME));
        EXTRACTED_ZIP_ROOT.delete();
        uiCallback.onUpdateSuccessful();
    }

    private static boolean isBlacklisted(File file) {
        if (file.isDirectory()) {
            return file.getName().startsWith("mipmap") ||
                    file.getName().equals("drawable-nodpi") ||
                    file.getName().equals("xml") ||
                    file.getName().equals(".gradle") ||
                    file.getName().equals(".idea") ||
                    file.getName().equals("build");
        } else {
            return file.getName().equals("list_item_about_dev.xml") ||
                    (file.getName().startsWith("dev_") && file.getName().endsWith(".xml")) ||
                    file.getName().equals("theme_config.xml") ||
                    file.getName().equals("strings.xml") ||
                    file.getName().equals("fragment_homepage.xml");
        }
    }

    public static String getResourcesDir() {
        return VALUES_FOLDER_PATH;
    }

    public static class PackageCopyInterceptor implements FileUtil.CopyInterceptor {
        @Override
        public String onCopyLine(File file, String line) {
            try {
                return line.replace("com.afollestad.polar", USER_CODE_PACKAGE);
            } catch (Throwable t) {
                t.printStackTrace();
                return line;
            }
        }

        @Override
        public boolean skip(File file) {
            return isBlacklisted(file);
        }

        @Override
        public boolean loggingEnabled() {
            return true;
        }
    }
}