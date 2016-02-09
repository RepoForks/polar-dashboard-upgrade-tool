package com.afollestad.polarupgradetool;

import com.afollestad.polarupgradetool.jfx.UICallback;

import java.io.*;
import java.util.ArrayList;

/**
 * @author Aidan Follestad (afollestad)
 */
public class GradleMigrator {

    private final File mProject;
    private final File mLatest;
    private final UICallback uiCallback;

    public GradleMigrator(File project, File latest, UICallback uiCallback) {
        mProject = project;
        mLatest = latest;
        this.uiCallback = uiCallback;
    }

    private String processLineProperty(String propertyName, String line, String propertyValue) {
        int start = line.indexOf(propertyName + " ");
        if (start == -1) return line;
        return String.format("        %s %s", propertyName, propertyValue);
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
                line = line.replace("output.outputFile.parent, \"MyPolarPack-${variant.versionName}.apk\")",
                        "output.outputFile.parent, \"" + Main.USER_APPNAME + "-${variant.versionName}.apk\")");
                line = processLineProperty("applicationId", line, "\"" + Main.USER_PACKAGE + "\"");
                line = processLineProperty("versionName", line, "\"" + Main.USER_VERSION_NAME + "\"");
                line = processLineProperty("versionCode", line, Main.USER_VERSION_CODE);
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