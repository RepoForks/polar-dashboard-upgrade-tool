package com.afollestad.polarupgradetool;

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

    public static File skipPackage(File file) {
        // file = java folder
        File[] contents = file.listFiles();
        if (contents == null) return file;
        file = contents[0];

        // file = com
        contents = file.listFiles();
        if (contents == null) return file;
        file = contents[0];

        // file = company
        contents = file.listFiles();
        if (contents == null) return file;
        file = contents[0];

        // file = iconpack
        contents = file.listFiles();
        if (contents == null) return file;
        file = contents[0];

        return file;
    }

    private Util() {
    }
}