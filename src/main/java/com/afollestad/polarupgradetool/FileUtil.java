package com.afollestad.polarupgradetool;

import java.io.*;

/**
 * @author Aidan Follestad (afollestad)
 */
public class FileUtil {

    public static String readableFileSize(long size) {
        double value;
        String unit;
        if (size < 1000) {
            value = (double) size;
            unit = "B";
        } else if (size >= 1000 && size < 1000000) {
            value = (double) size / (double) 1000;
            unit = "KB";
        } else if (size >= 1000000 && size < 1000000000) {
            value = (double) size / (double) 1000000;
            unit = "MB";
        } else {
            value = (double) size / (double) 1000000000;
            unit = "GB";
        }
        return String.format("%s%s", Util.round(value), unit);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static int wipe(File dir) {
        int count = 1;
        if (dir.isDirectory()) {
            File[] contents = dir.listFiles();
            if (contents != null && contents.length > 0) {
                for (File fi : contents)
                    count += wipe(fi);
            }
        }
        dir.delete();
        return count;
    }

    public static abstract class CopyInterceptor {
        public abstract String onCopyLine(File file, String line);

        public abstract boolean skip(File file);

        public boolean loggingEnabled() {
            return true;
        }
    }

    private static File mLastFolder;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean copyFolder(File source, File destination, CopyInterceptor interceptor) {
        if (interceptor != null && interceptor.skip(source)) {
            if (interceptor.loggingEnabled())
                Main.LOG("[INFO]: Ignored %s", Main.cleanupPath(source.getAbsolutePath()));
            return true;
        }

        if (interceptor == null || interceptor.loggingEnabled()) {
            if (mLastFolder == null || !mLastFolder.getAbsolutePath().equals(source.getAbsolutePath()))
                Main.LOG("%s -> %s", Main.cleanupPath(source.getAbsolutePath()), Main.cleanupPath(destination.getAbsolutePath()));
            mLastFolder = source;
        }
        if (source.isDirectory()) {
            if (!destination.exists())
                destination.mkdirs();
            String files[] = source.list();
            for (String file : files) {
                File srcFile = new File(source, file);
                File destFile = new File(destination, file);
                if (!copyFolder(srcFile, destFile, interceptor))
                    return false;
            }
            return true;
        } else {
            InputStream in = null;
            OutputStream out = null;
            BufferedReader reader = null;
            BufferedWriter writer = null;
            try {
                in = new FileInputStream(source);
                reader = new BufferedReader(new InputStreamReader(in));
                out = new FileOutputStream(destination);
                writer = new BufferedWriter(new OutputStreamWriter(out));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (interceptor != null)
                        line = interceptor.onCopyLine(source, line);
                    writer.write(line);
                    writer.newLine();
                }
            } catch (Exception e) {
                Main.LOG("[ERROR]: An error occurred while copying files: %s", e.getMessage());
                return false;
            } finally {
                Util.closeQuietely(reader);
                Util.closeQuietely(in);
                Util.closeQuietely(writer);
                Util.closeQuietely(out);
            }
            return true;
        }
    }

    private FileUtil() {
    }
}