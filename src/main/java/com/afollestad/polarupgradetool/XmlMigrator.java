package com.afollestad.polarupgradetool;

import com.afollestad.polarupgradetool.jfx.UICallback;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Aidan Follestad (afollestad)
 */
public class XmlMigrator {

    private static String getElementRegex(String... elementNames) {
        StringBuilder matchingGroup = new StringBuilder("(");
        for (int i = 0; i < elementNames.length; i++) {
            elementNames[i] = elementNames[i].replace("-", "\\-");
            if (i > 0) matchingGroup.append('|');
            matchingGroup.append(elementNames[i]);
        }
        matchingGroup.append(')');
        final String baseRegex = "<%s name=\"[\\S]*\">[\\s\\S\\n\\r]*<\\/%s>";
        final String elementNamesStr = matchingGroup.toString();
        return String.format(baseRegex, elementNamesStr, elementNamesStr);
    }

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
            final String fileContent = new String(fileRaw, "UTF-8");
            final Pattern elementRegex = Pattern.compile(getElementRegex("bool", "string", "drawable", "integer", "string-array", "color"));
            final Matcher matcher = elementRegex.matcher(fileContent);
            int start = 0;
            while (matcher.find(start)) {
                final String tag = fileContent.substring(matcher.start(), matcher.end());
                final String tagName = AttributeExtractor.getTagName(tag);
                if (tagName == null) continue;
                final String attributeName = AttributeExtractor.getAttributeValue("name", tag);
                final String elementValue = AttributeExtractor.getElementValue(tag);
                if (elementValue != null)
                    mSourceValues.put(attributeName, elementValue);
                start = matcher.end() + 1;
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

        // Default values
        mSourceValues.put("feedback_email", mSourceValues.get("icon_request_email"));
        mSourceValues.put("donate_license_key", "");
        mSourceValues.put("wallpapers_json_url", "");
        mSourceValues.put("licensing_public_key", "");

        // Put original project configuration back where possible, leaving new configuration added
        StringBuilder newFileContent;
        try {
            byte[] fileRaw = Files.readAllBytes(Paths.get(mLatest.getAbsolutePath()));
            newFileContent = new StringBuilder(new String(fileRaw, "UTF-8"));
            final Pattern elementRegex = Pattern.compile(getElementRegex("bool", "string", "drawable", "integer", "string-array", "color"));
            int start = 0;
            while (start != -1) {
                final Matcher matcher = elementRegex.matcher(newFileContent.toString());
                if (matcher.find(start)) {
                    final String tag = newFileContent.substring(matcher.start(), matcher.end());
                    final String tagName = AttributeExtractor.getTagName(tag);
                    if (tagName == null) continue;
                    final String attributeName = AttributeExtractor.getAttributeValue("name", tag);
                    if (mSourceValues.containsKey(attributeName)) {
                        final String tagReplacement = AttributeExtractor.setElementValue(tag, mSourceValues.get(attributeName));
                        newFileContent.replace(matcher.start(), matcher.end(), tagReplacement);
                    }
                    start = matcher.end() + 1;
                } else {
                    start = -1;
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
            Files.write(Paths.get(mProject.getAbsolutePath()),
                    newFileContent.toString().getBytes("UTF-8"), StandardOpenOption.WRITE);
        } catch (Exception e) {
            e.printStackTrace();
            Main.LOG("[ERROR]: Failed to write to %s: %s", Main.cleanupPath(mProject.getAbsolutePath()), e.getMessage());
            if (uiCallback != null)
                uiCallback.onErrorOccurred(String.format("Failed to write to %s: %s", Main.cleanupPath(mProject.getAbsolutePath()), e.getMessage()));
            e.printStackTrace();
            return false;
        }

        Main.LOG("[INFO]: Migrated %s", Main.cleanupPath(mProject.getAbsolutePath()));
        if (uiCallback != null)
            uiCallback.onStatusUpdate("Migrated XML resource file: " + Main.cleanupPath(mProject.getAbsolutePath()));
        return true;
    }
}