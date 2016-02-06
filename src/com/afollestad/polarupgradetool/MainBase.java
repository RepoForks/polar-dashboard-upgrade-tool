package com.afollestad.polarupgradetool;

import print.color.Ansi;
import print.color.ColoredPrinter;
import print.color.ColoredPrinterI;
import print.color.ColoredPrinterWIN;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Aidan Follestad (afollestad)
 */
class MainBase {

    private final static String ARCHIVE_URL = "https://github.com/afollestad/polar-dashboard/archive/master.zip";
    public final static int BUFFER_SIZE = 2048;

    protected static File EXTRACTED_ZIP_ROOT;
    protected static File CURRENT_DIR;
    private final static String ARCHIVE_ROOT = File.separator + "polar-dashboard-master";

    public static String cleanupPath(String from) {
        if (from.startsWith(CURRENT_DIR.getAbsolutePath())) {
            from = from.substring(CURRENT_DIR.getAbsolutePath().length());
        } else if (from.startsWith(EXTRACTED_ZIP_ROOT.getAbsolutePath())) {
            from = from.substring(EXTRACTED_ZIP_ROOT.getAbsolutePath().length());
        }
        return from;
    }

    public static void LOG(String msg, Object... args) {
        if (args != null)
            msg = String.format(msg, args);
        Ansi.FColor color;
        if (msg.startsWith("[ERROR]")) {
            color = Ansi.FColor.RED;
        } else if (msg.startsWith("[INFO]") || msg.startsWith("[DETECTED]") || msg.startsWith("[RENAMING]")) {
            color = Ansi.FColor.CYAN;
        } else {
            color = Ansi.FColor.WHITE;
        }
        getPrinter(color, Ansi.BColor.BLACK).println(msg);
    }

    public static void PROGRESS(String label, long read, long total) {
        final int percent = (int) Math.ceil(((double) read / (double) total) * 100d);
        StringBuilder sb = new StringBuilder(13);
        sb.append('\r');
        if (label != null) {
            sb.append(label);
            sb.append("  ");
        }
        sb.append('[');
        final int numOfEqual = percent / 10;
        final int numOfSpace = 10 - numOfEqual;
        for (int i = 0; i < numOfEqual; i++) sb.append('=');
        for (int i = 0; i < numOfSpace; i++) sb.append(' ');
        sb.append("]");
        sb.append("   ");
        sb.append(Util.round(percent));
        sb.append("%  ");
        sb.append(Util.readableFileSizeMB(read));
        sb.append('/');
        sb.append(Util.readableFileSizeMB(total));
        System.out.print(sb.toString());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected static boolean downloadArchive() {
        InputStream is = null;
        FileOutputStream os = null;
        try {
            URL url = new URL(ARCHIVE_URL);
            URLConnection conn = url.openConnection();
            is = conn.getInputStream();

            long contentLength;
            try {
                final String contentLengthStr = conn.getHeaderField("Content-Length");
                if (contentLengthStr == null || contentLengthStr.trim().isEmpty()) {
                    LOG("[ERROR]: No Content-Length header was returned by GitHub. Try running this app again.");
                    return false;
                }
                contentLength = Long.parseLong(contentLengthStr);
            } catch (Throwable e) {
                e.printStackTrace();
                LOG("[ERROR]: Failed to get the size of Polar's latest code archive. Please try running this app again.", e.getMessage());
                return false;
            }

            final File destZip = new File(CURRENT_DIR, "PolarLatest.zip");
            if (destZip.exists()) destZip.delete();
            os = new FileOutputStream(destZip);

            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            int totalRead = 0;

            LOG("[INFO]: Downloading a ZIP of Polar's latest code (%s)...", FileUtil.readableFileSize(contentLength));
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                totalRead += read;
                PROGRESS(null, totalRead, contentLength);
            }

            PROGRESS(null, contentLength, contentLength);
            System.out.println();
            LOG("[INFO]: Download complete!");
            os.flush();

            Util.closeQuietely(is);
            Util.closeQuietely(os);

            EXTRACTED_ZIP_ROOT = new File(CURRENT_DIR, "PolarLatest");
            if (EXTRACTED_ZIP_ROOT.exists()) {
                LOG("[INFO]: Removed %d files/folders from %s.", FileUtil.wipe(EXTRACTED_ZIP_ROOT),
                        Main.cleanupPath(EXTRACTED_ZIP_ROOT.getAbsolutePath()));
            }

            LOG("[INFO]: Extracting %s to %s...", cleanupPath(destZip.getAbsolutePath()),
                    cleanupPath(EXTRACTED_ZIP_ROOT.getAbsolutePath()));
            UnzipUtil.unzip(destZip.getAbsolutePath(), EXTRACTED_ZIP_ROOT.getAbsolutePath());
            LOG("[INFO]: Extraction complete!\n");
            destZip.delete();
            EXTRACTED_ZIP_ROOT = new File(EXTRACTED_ZIP_ROOT, ARCHIVE_ROOT);
        } catch (Exception e) {
            LOG("[ERROR]: An error occurred during download or extraction: %s\n", e.getMessage());
            return false;
        } finally {
            Util.closeQuietely(is);
            Util.closeQuietely(os);
        }
        return true;
    }

    protected static ColoredPrinterI getPrinter(Ansi.FColor frontColor, Ansi.BColor backColor) {
        String os = System.getProperty("os.name");
        //System.out.println("DETECTED OS: " + os);
        if (os.toLowerCase().startsWith("win")) {
            return new ColoredPrinterWIN.Builder(1, false)
                    .foreground(frontColor).background(backColor).build();
        } else {
            return new ColoredPrinter.Builder(1, false)
                    .foreground(frontColor).background(backColor).build();
        }

    }
}
