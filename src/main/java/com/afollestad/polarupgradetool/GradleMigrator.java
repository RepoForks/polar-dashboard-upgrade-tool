package com.afollestad.polarupgradetool;

import com.afollestad.polarupgradetool.jfx.UICallback;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Aidan Follestad (afollestad)
 */
public class GradleMigrator {

    private final File mProject;
    private final File mLatest;
    private final ArrayList<String> mPropertyNames;
    private final ArrayList<String> mPropertyValues;
    private final UICallback uiCallback;

    public GradleMigrator(File project, File latest, String[] propertyNames, String[] propertyValues, UICallback uiCallback) {
        mProject = project;
        mLatest = latest;
        mPropertyNames = new ArrayList<>(propertyNames.length);
        Collections.addAll(mPropertyNames, propertyNames);
        mPropertyValues = new ArrayList<>(propertyValues.length);
        Collections.addAll(mPropertyValues, propertyValues);
        this.uiCallback = uiCallback;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public boolean process() {
        final ArrayList<String> lines = new ArrayList<>();
        InputStream is = null;
        BufferedReader reader = null;

        try {
            is = new FileInputStream(mLatest);
            reader = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = reader.readLine()) != null) {
                for (int i = 0; i < mPropertyNames.size(); i++) {
                    final String propertyName = mPropertyNames.get(i);
                    int start = line.indexOf(propertyName + " ");
                    if (start == -1) continue;
                    line = String.format("        %s %s", propertyName, mPropertyValues.get(i));
                }
                lines.add(line);
            }
        } catch (Exception e) {
            Main.LOG("[ERROR]: Failed to migrate a Gradle file: %s", e.getMessage());
            if (uiCallback != null) uiCallback.onErrorOccurred("Failed to migrate Gradle file:\n" + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            Util.closeQuietely(reader);
            Util.closeQuietely(is);
        }

        mProject.delete();
        OutputStream os = null;
        BufferedWriter writer = null;

        try {
            os = new FileOutputStream(mProject);
            writer = new BufferedWriter(new OutputStreamWriter(os));

            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) writer.newLine();
                writer.write(lines.get(i));
            }
        } catch (Exception e) {
            Main.LOG("[ERROR]: Failed to migrate a Gradle file: %s", e.getMessage());
            if (uiCallback != null) uiCallback.onErrorOccurred("Failed to migrate Gradle file:\n" + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            Util.closeQuietely(writer);
            Util.closeQuietely(os);
        }

        Main.LOG("[INFO]: Migrated Gradle file %s", Main.cleanupPath(mProject.getAbsolutePath()));
        if (uiCallback != null)
            uiCallback.onStatusUpdate("Migrated Gradle file: " + Main.cleanupPath(mProject.getAbsolutePath()));
        return true;
    }
}