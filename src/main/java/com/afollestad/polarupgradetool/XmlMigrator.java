package com.afollestad.polarupgradetool;

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
            final XmlScanner scanner = new XmlScanner(fileContent);
            String tag;

            while ((tag = scanner.nextTag()) != null) {
                final String tagName = AttributeExtractor.getTagName(tag);
                if (tagName == null) continue;
                final String attributeName = AttributeExtractor.getAttributeValue("name", tag);
                final String elementValue = AttributeExtractor.getElementValue(tag);
                if (elementValue != null)
                    mSourceValues.put(attributeName, elementValue);
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

        // TODO if project defaults are used in latest, set them to empty/null

        // Put original project configuration back where possible, leaving new configuration added
        StringBuilder newFileContent;
        try {
            byte[] fileRaw = Files.readAllBytes(Paths.get(mLatest.getAbsolutePath()));
            newFileContent = new StringBuilder(new String(fileRaw, "UTF-8"));
            XmlScanner scanner = new XmlScanner(newFileContent);

            String tag;
            while ((tag = scanner.nextTag()) != null) {
                final String attributeName = AttributeExtractor.getAttributeValue("name", tag);
                if (mSourceValues.containsKey(attributeName))
                    scanner.setElementValue(mSourceValues.get(attributeName));
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