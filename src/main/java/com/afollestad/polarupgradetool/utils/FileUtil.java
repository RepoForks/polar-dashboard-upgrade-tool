package com.afollestad.polarupgradetool.utils;

import com.afollestad.polarupgradetool.Main;
import com.afollestad.polarupgradetool.jfx.UICallback;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

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
    public static void checkResRename(String oldName, String expectedName, UICallback uiCallback) {
        final File valuesFolder = new File(Main.CURRENT_DIR, Main.getResourcesDir());
        File source = new File(valuesFolder, oldName);
        if (source.exists()) {
            File dest = new File(valuesFolder, expectedName);
            if (!dest.exists()) {
                Main.LOG("[RENAME]: %s -> %s", Main.cleanupPath(source.getAbsolutePath()), Main.cleanupPath(dest.getAbsolutePath()));
                uiCallback.onStatusUpdate(String.format("Renaming %s -> %s",
                        Main.cleanupPath(source.getAbsolutePath()), Main.cleanupPath(dest.getAbsolutePath())));
                if (!source.renameTo(dest)) {
                    Main.LOG("[ERROR]: Unable to rename %s", Main.cleanupPath(source.getAbsolutePath()));
                    uiCallback.onErrorOccurred("Unable to rename: " + Main.cleanupPath(source.getAbsolutePath()));
                }
            } else {
                source.delete();
            }
        } else {
            String msg = String.format("%s file wasn't found (in %s), assuming %s is used already.",
                    oldName, Main.cleanupPath(source.getParent()), expectedName);
            Main.LOG("[INFO] " + msg);
            uiCallback.onStatusUpdate(msg);
        }
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

    public interface SkipInterceptor {
        boolean skip(File file);
    }

    public interface CopyInterceptor extends SkipInterceptor {
        String onCopyLine(File file, String line);

        boolean loggingEnabled();
    }

    private static void copyFileText(File src, File dst, CopyInterceptor interceptor) throws Exception {
        InputStream in = null;
        OutputStream out = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            in = new FileInputStream(src);
            reader = new BufferedReader(new InputStreamReader(in));
            out = new FileOutputStream(dst);
            writer = new BufferedWriter(new OutputStreamWriter(out));

            String line;
            while ((line = reader.readLine()) != null) {
                if (interceptor != null)
                    line = interceptor.onCopyLine(src, line);
                writer.write(line);
                writer.newLine();
            }
        } finally {
            Util.closeQuietely(reader);
            Util.closeQuietely(in);
            Util.closeQuietely(writer);
            Util.closeQuietely(out);
        }
    }

    private static void copyFileBinary(File src, File dst) throws Exception {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buffer = new byte[Main.BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1)
                out.write(buffer, 0, read);
        } finally {
            Util.closeQuietely(in);
            Util.closeQuietely(out);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean replaceInFile(File file, String find, String replace, UICallback uiCallback) {
        try {
            Path path = Paths.get(file.getAbsolutePath());
            byte[] content = Files.readAllBytes(path);
            String contentStr = new String(content, "UTF-8");
            contentStr = contentStr.replace(find, replace);
            content = contentStr.getBytes("UTF-8");
            file.delete();
            Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (Throwable t) {
            t.printStackTrace();
            Main.LOG("[ERROR]: Failed to perform a find and replace in %s: %s",
                    Main.cleanupPath(file.getAbsolutePath()), t.getMessage());
            uiCallback.onErrorOccurred(String.format("Failed to perform a find and replace in %s: %s",
                    Main.cleanupPath(file.getAbsolutePath()), t.getMessage()));
            return false;
        }
        return true;
    }

    // Checks for files in the project folder that no longer exist in the latest code
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void checkDiff(File project, File latest, SkipInterceptor interceptor, boolean importMode) {
        if (importMode) {
            if (project.isDirectory() && interceptor.skip(project))
                return;
            if (!project.exists() && latest.exists()) {
                Main.LOG("[ADD]: %s -> %s...",
                        Main.cleanupPath(latest.getAbsolutePath()), Main.cleanupPath(project.getAbsolutePath()));
                boolean result = copyFolder(latest, project, new CopyInterceptor() {
                    @Override
                    public String onCopyLine(File file, String line) {
                        return line.replace("com.afollestad.polar", Main.USER_CODE_PACKAGE);
                    }

                    @Override
                    public boolean loggingEnabled() {
                        return true;
                    }

                    @Override
                    public boolean skip(File file) {
                        return false;
                    }
                });
                if (!result) return;
            }
            if (latest.isDirectory()) {
                String files[] = latest.list();
                for (String file : files) {
                    File srcFile = new File(project, file);
                    File destFile = new File(latest, file);
                    checkDiff(srcFile, destFile, interceptor, true);
                }
            }
        } else {
            if (interceptor.skip(project))
                return;
            if (project.exists() && !latest.exists()) {
                Main.LOG("[DELETE]: %s", Main.cleanupPath(project.getAbsolutePath()));
                if (project.isDirectory()) {
                    wipe(project);
                } else {
                    project.delete();
                }
            } else if (project.isDirectory()) {
                String files[] = project.list();
                for (String file : files) {
                    File srcFile = new File(project, file);
                    File destFile = new File(latest, file);
                    checkDiff(srcFile, destFile, interceptor, false);
                }
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean copyFolder(File source, File destination, CopyInterceptor interceptor) {
        if (interceptor != null && interceptor.skip(source)) {
            if (interceptor.loggingEnabled())
                Main.LOG("[SKIP]: %s", Main.cleanupPath(source.getAbsolutePath()));
            return true;
        }

        if (interceptor == null || interceptor.loggingEnabled())
            Main.LOG("[COPY]: %s -> %s", Main.cleanupPath(source.getAbsolutePath()), Main.cleanupPath(destination.getAbsolutePath()));
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
            try {
                final String name = source.getName().toLowerCase(Locale.getDefault());
                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) {
                    copyFileBinary(source, destination);
                } else {
                    copyFileText(source, destination, interceptor);
                }
            } catch (Exception e) {
                Main.LOG("[ERROR]: An error occurred while copying %s: %s",
                        Main.cleanupPath(source.getAbsolutePath()), e.getMessage());
                return false;
            }
            return true;
        }
    }

    private FileUtil() {
    }
}