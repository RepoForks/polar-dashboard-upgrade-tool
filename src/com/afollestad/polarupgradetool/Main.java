package com.afollestad.polarupgradetool;

import print.color.Ansi;

import java.io.File;
import java.util.HashMap;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Main extends MainBase {

    public static String USER_PACKAGE;
    public static String USER_VERSION_NAME;
    public static String USER_VERSION_CODE;
    public static String USER_APPNAME;

    private final static String LICENSING_MODULE_ROOT = File.separator + "licensing";
    private final static String GRADLE_FILE_PATH = File.separator + "app" + File.separator + "build.gradle";
    private final static String MAIN_FOLDER = File.separator + "app" + File.separator + "src" + File.separator + "main";
    private final static String JAVA_FOLDER_PATH = MAIN_FOLDER + File.separator + "java";
    private final static String RES_FOLDER_PATH = MAIN_FOLDER + File.separator + "res";
    private final static String VALUES_FOLDER_PATH = MAIN_FOLDER + File.separator + "res" + File.separator + "values";
    private final static String MANIFEST_FILE_PATH = MAIN_FOLDER + File.separator + "AndroidManifest.xml";

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(String[] args) {
        CURRENT_DIR = new File(System.getProperty("user.dir"));

        getPrinter(Ansi.FColor.YELLOW, Ansi.BColor.BLACK).println(
                "\n--------------------------------------\n| Welcome to the Polar upgrade tool! |\n--------------------------------------");

        // Use app/build.gradle and /res/values/strings.xml to load info about icon pack
        File gradleFile = new File(CURRENT_DIR, GRADLE_FILE_PATH);
        AttributeExtractor gradleExtractor = new AttributeExtractor(gradleFile,
                new String[]{"applicationId", "versionName", "versionCode"}, AttributeExtractor.MODE_GRADLE);
        XmlElementExtractor stringsExtractor = new XmlElementExtractor(new File(CURRENT_DIR,
                String.format("%s%s%s%s%s", RES_FOLDER_PATH, File.separator, "values", File.separator, "strings.xml")),
                new String[]{"string"}, new String[]{"app_name"});
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

        // Download latest code
        if (!downloadArchive()) {
            LOG(""); // resets foreground color
            return;
        }

        // Copy manifest
        File source = new File(EXTRACTED_ZIP_ROOT, MANIFEST_FILE_PATH);
        File dest = new File(CURRENT_DIR, MANIFEST_FILE_PATH);
        LOG("[INFO]: Migrating AndroidManifest.xml");
        FileUtil.copyFolder(source, dest, new FileUtil.CopyInterceptor() {
            @Override
            public String onCopyLine(File file, String line) {
                return line.replace("com.afollestad.polar", USER_PACKAGE);
            }

            @Override
            public boolean skip(File file) {
                return isBlacklisted(file);
            }

            @Override
            public boolean loggingEnabled() {
                return false;
            }
        });

        // Copy build.gradle
        source = new File(CURRENT_DIR, GRADLE_FILE_PATH);
        GradleMigrator gradleMigrator = new GradleMigrator(source,
                new String[]{"applicationId", "versionCode", "versionName"},
                new String[]{String.format("\"%s\"", USER_PACKAGE), USER_VERSION_CODE, String.format("\"%s\"", USER_VERSION_NAME)});
        if (!gradleMigrator.process()) return;

        // TODO migrate app/build.gradle

        // Copy licensing module
        LOG("[INFO]: Migrating the licensing module...");
        source = new File(EXTRACTED_ZIP_ROOT, LICENSING_MODULE_ROOT);
        dest = new File(CURRENT_DIR, LICENSING_MODULE_ROOT);
        FileUtil.copyFolder(source, dest, new FileUtil.CopyInterceptor() {
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
        });

        System.out.println();

        // Copy Java files
        source = new File(EXTRACTED_ZIP_ROOT, JAVA_FOLDER_PATH);
        source = Util.skipPackage(source);
        dest = new File(CURRENT_DIR, JAVA_FOLDER_PATH);
        dest = Util.skipPackage(dest);
        FileUtil.copyFolder(source, dest, new FileUtil.CopyInterceptor() {
            @Override
            public String onCopyLine(File file, String line) {
                return line.replace("com.afollestad.polar", USER_PACKAGE);
            }

            @Override
            public boolean skip(File file) {
                return isBlacklisted(file);
            }
        });

        // If changelog.xml is still used, rename it to dev_changelog.xml before migrating.
        source = new File(CURRENT_DIR, VALUES_FOLDER_PATH);
        source = new File(source, "changelog.xml");
        if (source.exists()) {
            dest = new File(CURRENT_DIR, VALUES_FOLDER_PATH);
            dest = new File(dest, "dev_changelog.xml");
            if (!dest.exists()) {
                LOG("[RENAMING]: %s -> %s", cleanupPath(source.getAbsolutePath()), cleanupPath(dest.getAbsolutePath()));
                if (!source.renameTo(dest))
                    LOG("[ERROR]: Unable to rename %s", cleanupPath(source.getAbsolutePath()));
            } else {
                source.delete();
            }
        } else {
            LOG("[INFO] changelog.xml file wasn't found (in %s), assuming dev_changelog.xml is used already.",
                    cleanupPath(source.getParent()));
        }

        // If dev_options is still used, rename it to dev_customization before migrating.
        source = new File(CURRENT_DIR, VALUES_FOLDER_PATH);
        source = new File(source, "dev_options.xml");
        if (source.exists()) {
            dest = new File(CURRENT_DIR, VALUES_FOLDER_PATH);
            dest = new File(dest, "dev_customization.xml");
            if (!dest.exists()) {
                LOG("[RENAMING]: %s -> %s", cleanupPath(source.getAbsolutePath()), cleanupPath(dest.getAbsolutePath()));
                if (!source.renameTo(dest))
                    LOG("[ERROR]: Unable to rename %s", cleanupPath(source.getAbsolutePath()));
            } else {
                source.delete();
            }
        } else {
            LOG("[INFO] dev_options.xml file wasn't found (in %s), assuming dev_customization.xml is used already.",
                    cleanupPath(source.getParent()));
        }

        // Copy resource files, minus blacklisted files
        source = new File(EXTRACTED_ZIP_ROOT, RES_FOLDER_PATH);
        dest = new File(CURRENT_DIR, RES_FOLDER_PATH);
        FileUtil.copyFolder(source, dest, new FileUtil.CopyInterceptor() {
            @Override
            public String onCopyLine(File file, String line) {
                return line.replace("com.afollestad.polar", USER_PACKAGE);
            }

            @Override
            public boolean skip(File file) {
                return isBlacklisted(file);
            }
        });

        // Migrate the black listed files
        File projectValues = new File(new File(CURRENT_DIR, RES_FOLDER_PATH), "values");
        File latestValues = new File(new File(EXTRACTED_ZIP_ROOT, RES_FOLDER_PATH), "values");
        XmlMigrator migrator = new XmlMigrator(
                new File(projectValues, "strings.xml"), new File(latestValues, "strings.xml"));
        if (!migrator.process()) return;
        migrator = new XmlMigrator(
                new File(projectValues, "dev_about.xml"), new File(latestValues, "dev_about.xml"));
        if (!migrator.process()) return;
        migrator = new XmlMigrator(
                new File(projectValues, "dev_changelog.xml"), new File(latestValues, "dev_changelog.xml"));
        if (!migrator.process()) return;
        migrator = new XmlMigrator(
                new File(projectValues, "dev_customization.xml"), new File(latestValues, "dev_customization.xml"));
        if (!migrator.process()) return;
        migrator = new XmlMigrator(
                new File(projectValues, "dev_theming.xml"), new File(latestValues, "dev_theming.xml"));
        if (!migrator.process()) return;

        System.out.println();
        getPrinter(Ansi.FColor.BLUE, Ansi.BColor.BLACK).print(
                String.format("Upgrade is complete for %s!", USER_APPNAME));
        getPrinter(Ansi.FColor.WHITE, Ansi.BColor.BLACK).println(" ");
        EXTRACTED_ZIP_ROOT.delete();
    }

    private static boolean isBlacklisted(File file) {
        if (file.isDirectory()) {
            return file.getName().startsWith("mipmap") ||
                    file.getName().equals("drawable-nodpi") ||
                    file.getName().equals("xml");
        } else {
            return file.getName().equals("list_item_about_dev.xml") ||
                    (file.getName().startsWith("dev_") && file.getName().endsWith(".xml")) ||
                    file.getName().equals("theme_config.xml") ||
                    file.getName().equals("strings.xml");
        }
    }
}