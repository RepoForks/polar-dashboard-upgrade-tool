package com.afollestad.polarupgradetool.xml;

import com.afollestad.polarupgradetool.AttributeExtractor;
import com.afollestad.polarupgradetool.Main;
import com.afollestad.polarupgradetool.jfx.UICallback;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

/**
 * @author Aidan Follestad (afollestad)
 */
public class XmlMigrator {

//    private static String getElementRegex(String... elementNames) {
//        StringBuilder matchingGroup = new StringBuilder("(");
//        for (int i = 0; i < elementNames.length; i++) {
//            elementNames[i] = elementNames[i].replace("-", "\\-");
//            if (i > 0) matchingGroup.append('|');
//            matchingGroup.append(elementNames[i]);
//        }
//        matchingGroup.append(')');
//        final String baseRegex = "<%s name=\"[\\S]*\">[\\s\\S\\n\\r]*<\\/%s>";
//        final String elementNamesStr = matchingGroup.toString();
//        return String.format(baseRegex, elementNamesStr, elementNamesStr);
//    }

    private final File mProject;
    private final File mLatest;

    private UICallback uiCallback;

    public XmlMigrator(File project, File latest, UICallback uiCallback) {
        mProject = project;
        mLatest = latest;
        this.uiCallback = uiCallback;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public boolean process() {
        if (!mProject.exists()) {
            Main.LOG("[ERROR]: %s doesn't exist.", Main.cleanupPath(mProject.getAbsolutePath()));
            if (uiCallback != null)
                uiCallback.onErrorOccurred(Main.cleanupPath(mProject.getAbsolutePath()) + " doesn't exist.");
            return false;
        } else if (!mLatest.exists()) {
            Main.LOG("[ERROR]: %s doesn't exist.", Main.cleanupPath(mLatest.getAbsolutePath()));
            if (uiCallback != null)
                uiCallback.onErrorOccurred(Main.cleanupPath(mLatest.getAbsolutePath()) + " doesn't exist.");
            return false;
        }

        final HashMap<String, String> mSourceValues = new HashMap<>();

        // Read the project (local) file to pull out the user's current configuration
        try {
            byte[] fileRaw = Files.readAllBytes(Paths.get(mProject.getAbsolutePath()));
            final StringBuilder fileContent = new StringBuilder(new String(fileRaw, "UTF-8"));
            final XmlScanner scanner = new XmlScanner(fileContent, mProject);

            while (!scanner.reachedEnd()) {
                final String tag = scanner.nextTag();
                if (tag == null) continue;
                final String attributeName = AttributeExtractor.getAttributeValue("name", tag);
                final String tagValue = scanner.tagValue();
                if (tagValue != null)
                    mSourceValues.put(attributeName, tagValue);
            }
        } catch (Exception e) {
            Main.LOG("[ERROR]: Failed to process %s for XML migration: %s",
                    Main.cleanupPath(mProject.getAbsolutePath()), e.getMessage());
            if (uiCallback != null) {
                uiCallback.onErrorOccurred(String.format("Failed to process %s for XML migration: %s",
                        Main.cleanupPath(mProject.getAbsolutePath()), e.getMessage()));
            }
            e.printStackTrace();
            return false;
        }

        if (mProject.getName().equals("dev_customization.xml")) {
            // If project defaults are used in project file, set them to empty/null
            if (!mSourceValues.containsKey("wallpapers_json_url")) {
                mSourceValues.put("wallpapers_json_url", "");
            }
            if (!mSourceValues.containsKey("icon_request_email") ||
                    mSourceValues.get("icon_request_email").equals("fake-email@fake-website.com")) {
                mSourceValues.put("icon_request_email", "");
            }
            if (!mSourceValues.containsKey("donate_license_key")) {
                mSourceValues.put("donate_license_key", "");
            }
            if (!mSourceValues.containsKey("feedback_email") ||
                    mSourceValues.get("feedback_email").equals("fake-email@fake-website.com")) {
                mSourceValues.put("feedback_email", mSourceValues.get("icon_request_email"));
            }
            if (!mSourceValues.containsKey("homepage_landing_icon")) {
                mSourceValues.put("homepage_landing_icon", "@mipmap/ic_launcher");
            }
        }

        // Put original project configuration back where possible, leaving new configuration added
        StringBuilder newFileContent;
        try {
            byte[] fileRaw = Files.readAllBytes(Paths.get(mLatest.getAbsolutePath()));
            newFileContent = new StringBuilder(new String(fileRaw, "UTF-8"));
            XmlScanner scanner = new XmlScanner(newFileContent, mLatest);

            while (!scanner.reachedEnd()) {
                final String tag = scanner.nextTag();
                if (tag == null) continue;
                final String attributeName = AttributeExtractor.getAttributeValue("name", tag);
                if (attributeName == null) continue;
                if (mSourceValues.containsKey(attributeName)) {
                    scanner.setElementValue(mSourceValues.get(attributeName));
                } else if (attributeName.equals("about_buttons_names") ||
                        attributeName.equals("about_buttons_links")) {
                    if ((Main.OLD_ABOUT_BUTTON1_TEXT != null && Main.OLD_ABOUT_BUTTON2_TEXT != null) ||
                            (Main.OLD_ABOUT_BUTTON1_LINK != null && Main.OLD_ABOUT_BUTTON2_LINK != null)) {
                        final StringBuilder value = new StringBuilder(scanner.tagValue());
                        int start = value.indexOf("<item>") + "<item>".length();
                        int end = value.indexOf("</item>", start);
                        if (attributeName.equals("about_buttons_names") &&
                                Main.OLD_ABOUT_BUTTON1_TEXT != null) {
                            value.replace(start, end, String.format("%s|%s",
                                    Main.OLD_ABOUT_BUTTON1_TEXT, Main.OLD_ABOUT_BUTTON2_TEXT));
                        } else if (attributeName.equals("about_buttons_links") &&
                                Main.OLD_ABOUT_BUTTON1_LINK != null) {
                            value.replace(start, end, String.format("%s|%s",
                                    Main.OLD_ABOUT_BUTTON1_LINK, Main.OLD_ABOUT_BUTTON2_LINK));
                        }
                        scanner.setElementValue(value.toString());
                    }
                }
            }
        } catch (Exception e) {
            Main.LOG("[ERROR]: Failed to process %s for XML migration: %s",
                    Main.cleanupPath(mProject.getAbsolutePath()), e.getMessage());
            if (uiCallback != null) {
                uiCallback.onErrorOccurred(String.format("Failed to process %s for XML migration: %s",
                        Main.cleanupPath(mProject.getAbsolutePath()), e.getMessage()));
            }
            e.printStackTrace();
            return false;
        }

        // Write the latest (remote) file's changed contents to the project (local) file
        try {
            mProject.delete();
            Files.write(Paths.get(mProject.getAbsolutePath()),
                    newFileContent.toString().getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (Exception e) {
            e.printStackTrace();
            Main.LOG("[ERROR]: Failed to write to %s: %s", Main.cleanupPath(mProject.getAbsolutePath()), e.getMessage());
            if (uiCallback != null)
                uiCallback.onErrorOccurred(String.format("Failed to write to %s: %s", Main.cleanupPath(mProject.getAbsolutePath()), e.getMessage()));
            e.printStackTrace();
            return false;
        }

        Main.LOG("[MIGRATE]: %s", Main.cleanupPath(mProject.getAbsolutePath()));
        if (uiCallback != null)
            uiCallback.onStatusUpdate("Migrated XML resource file: " + Main.cleanupPath(mProject.getAbsolutePath()));
        return true;
    }
}