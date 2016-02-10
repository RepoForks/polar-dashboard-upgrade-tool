package com.afollestad.polarupgradetool;

import com.afollestad.polarupgradetool.jfx.UICallback;

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

        // Download latest code
        if (!downloadArchive(uiCallback)) return;

        // Copy manifest
        File source = new File(EXTRACTED_ZIP_ROOT, MANIFEST_FILE_PATH);
        File dest = new File(CURRENT_DIR, MANIFEST_FILE_PATH);
        LOG("[MIGRATE]: AndroidManifest.xml...");
        uiCallback.onStatusUpdate("Migrating AndroidManifest.xml...");

        FileUtil.copyFolder(source, dest, new PackageCopyInterceptor() {
            @Override
            public boolean loggingEnabled() {
                return false;
            }
        });

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

        LOG("[MIGRATE]: app module...");
        uiCallback.onStatusUpdate("Migrating the app module...");
        System.out.println();

        // Check for Java files that no longer exist in the latest code
        source = new File(EXTRACTED_ZIP_ROOT, JAVA_FOLDER_PATH);
        source = Util.skipPackage(source);
        dest = new File(CURRENT_DIR, JAVA_FOLDER_PATH);
        dest = Util.skipPackage(dest);
        FileUtil.checkDiff(dest, source, Main::isBlacklisted, false);
        // Also check for Java files that don't exist in the project code but exist in the latest code
        FileUtil.checkDiff(dest, source, Main::isBlacklisted, true);
        // Copy Java files
        FileUtil.copyFolder(source, dest, new PackageCopyInterceptor());

        // Check for resource files that were deleted from the latest code
        source = new File(EXTRACTED_ZIP_ROOT, RES_FOLDER_PATH);
        dest = new File(CURRENT_DIR, RES_FOLDER_PATH);
        FileUtil.checkDiff(dest, source, Main::isBlacklisted, false);
        // Also check for resource files that don't exist in the project code but exist in the latest code
        FileUtil.checkDiff(dest, source, Main::isBlacklisted, true);
        // Copy resource files, minus blacklisted files
        FileUtil.copyFolder(source, dest, new PackageCopyInterceptor());

        // If changelog.xml is still used, rename it to dev_changelog.xml before migrating.
        source = new File(CURRENT_DIR, VALUES_FOLDER_PATH);
        source = new File(source, "changelog.xml");
        if (source.exists()) {
            dest = new File(CURRENT_DIR, VALUES_FOLDER_PATH);
            dest = new File(dest, "dev_changelog.xml");
            if (!dest.exists()) {
                LOG("[RENAME]: %s -> %s", cleanupPath(source.getAbsolutePath()), cleanupPath(dest.getAbsolutePath()));
                uiCallback.onStatusUpdate(String.format("Renaming %s -> %s", cleanupPath(source.getAbsolutePath()), cleanupPath(dest.getAbsolutePath())));

                if (!source.renameTo(dest)) {
                    LOG("[ERROR]: Unable to rename %s", cleanupPath(source.getAbsolutePath()));
                    uiCallback.onErrorOccurred("Unable to rename: " + cleanupPath(source.getAbsolutePath()));
                }
            } else {
                source.delete();
            }
        } else {
            LOG("[INFO] changelog.xml file wasn't found (in %s), assuming dev_changelog.xml is used already.",
                    cleanupPath(source.getParent()));
            uiCallback.onStatusUpdate(String.format("changelog.xml file wasn't found (in %s), assuming dev_changelog.xml is used already.",
                    cleanupPath(source.getParent())));
        }

        // If dev_options is still used, rename it to dev_customization before migrating.
        source = new File(CURRENT_DIR, VALUES_FOLDER_PATH);
        source = new File(source, "dev_options.xml");
        if (source.exists()) {
            dest = new File(CURRENT_DIR, VALUES_FOLDER_PATH);
            dest = new File(dest, "dev_customization.xml");
            if (!dest.exists()) {
                LOG("[RENAME]: %s -> %s", cleanupPath(source.getAbsolutePath()), cleanupPath(dest.getAbsolutePath()));
                uiCallback.onStatusUpdate("Renaming " + cleanupPath(source.getAbsolutePath()) + " -> " + cleanupPath(dest.getAbsolutePath()));
                if (!source.renameTo(dest)) {
                    LOG("[ERROR]: Unable to rename %s", cleanupPath(source.getAbsolutePath()));
                    uiCallback.onErrorOccurred("Unable to rename " + cleanupPath(source.getAbsolutePath()));
                }
            } else {
                source.delete();
            }
        } else {
            LOG("[INFO] dev_options.xml file wasn't found (in %s), assuming dev_customization.xml is used already.",
                    cleanupPath(source.getParent()));
            uiCallback.onStatusUpdate("dev_options.xml file wasn't found (in" + cleanupPath(source.getParent()) + "), assuming dev_customization.xml is used already.");
        }

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
            return line.replace("com.afollestad.polar", USER_PACKAGE);
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