package com.afollestad.polarupgradetool;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Aidan Follestad (afollestad)
 */
public class XmlMigrator {

    private final File mProject;
    private final File mLatest;

    public XmlMigrator(File project, File latest) {
        mProject = project;
        mLatest = latest;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public boolean process() {
        if (!mProject.exists()) {
            Main.LOG("[ERROR]: %s doesn't exist.", Main.cleanupPath(mProject.getAbsolutePath()));
            return false;
        } else if (!mLatest.exists()) {
            Main.LOG("[ERROR]: %s doesn't exist.", Main.cleanupPath(mLatest.getAbsolutePath()));
            return false;
        }

        InputStream is = null;
        BufferedReader reader = null;
        OutputStream os = null;
        BufferedWriter writer = null;

        // Read cache of values from the project (source) file, so they are maintained through migration
        final HashMap<String, String> mSourceValues = new HashMap<>();
        try {
            is = new FileInputStream(mProject);
            reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                final String elementName = AttributeExtractor.getTagName(line);
                if (elementName == null || elementName.equals("resources")) continue;
                final String attributeName = AttributeExtractor.getAttributeValue("name", line);
                if (attributeName != null) {
                    final String elementValue = AttributeExtractor.getElementValue(line);
                    if (elementValue != null)
                        mSourceValues.put(attributeName, elementValue);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Main.LOG("[ERROR]: Failed to perform XML file migration: %s", e.getMessage());
            return false;
        } finally {
            Util.closeQuietely(reader);
            Util.closeQuietely(is);
        }

        // Read the contents of the latest (destination) file to ArrayList
        final ArrayList<String> destLines = new ArrayList<>();
        try {
            is = new FileInputStream(mLatest);
            reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                final String elementName = AttributeExtractor.getTagName(line);
                if (elementName != null && !elementName.equals("resources")) {
                    final String attributeName = AttributeExtractor.getAttributeValue("name", line);
                    final String sourceValue = mSourceValues.get(attributeName);
                    if (sourceValue != null)
                        line = AttributeExtractor.setElementValue(line, sourceValue);
                }
                destLines.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Main.LOG("[ERROR]: Failed to perform XML file migration: %s", e.getMessage());
            return false;
        } finally {
            Util.closeQuietely(reader);
            Util.closeQuietely(is);
        }

        // Write processed lines back to the project (source) file
        try {
            mProject.delete();
            os = new FileOutputStream(mProject);
            writer = new BufferedWriter(new OutputStreamWriter(os));
            for (int i = 0; i < destLines.size(); i++) {
                if (i > 0) writer.newLine();
                writer.write(destLines.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Main.LOG("[ERROR]: Failed to perform XML file migration: %s", e.getMessage());
            return false;
        } finally {
            Util.closeQuietely(writer);
            Util.closeQuietely(os);
        }

        Main.LOG("[INFO]: Migrated %s", Main.cleanupPath(mProject.getAbsolutePath()));
        return true;
    }
}