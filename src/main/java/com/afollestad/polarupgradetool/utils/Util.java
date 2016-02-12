package com.afollestad.polarupgradetool.utils;

import java.io.Closeable;
import java.io.File;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Util {

    public static void closeQuietely(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable ignored) {
        }
    }

    public static String round(double value) {
        final DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        return df.format(value);
    }

    public static String round(float value) {
        final DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        return df.format(value);
    }

    public static String readableFileSizeMB(long size) {
        final double value = (double) size / 1000000;
        return String.format("%sMB", round(value));
    }

    public static String detectCodePackage(final File javaFolder) {
        String pkg = "";

        File[] contents = javaFolder.listFiles();
        if (contents == null) return pkg;
        // com
        pkg += contents[0].getName();

        contents = javaFolder.listFiles();
        if (contents == null) return pkg;
        // afollestad
        pkg += "." + contents[0].getName();

        contents = javaFolder.listFiles();
        if (contents == null) return pkg;
        // polar
        pkg += "." + contents[0].getName();

        return pkg;
    }

    public static File skipPackage(File javaFolder) {
        File[] contents = javaFolder.listFiles();
        if (contents == null) return javaFolder;
        // com
        javaFolder = contents[0];

        contents = javaFolder.listFiles();
        if (contents == null) return javaFolder;
        // afollestad
        javaFolder = contents[0];

        contents = javaFolder.listFiles();
        if (contents == null) return javaFolder;
        // polar
        javaFolder = contents[0];

        return javaFolder;
    }

    private Util() {
    }
}