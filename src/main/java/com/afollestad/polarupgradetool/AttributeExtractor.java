package com.afollestad.polarupgradetool;

import com.afollestad.polarupgradetool.jfx.UICallback;

import java.io.*;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Aidan Follestad (afollestad)
 */
public class AttributeExtractor {

    private static final String XML_REGEX = "%s=[\"']?((?:.(?![\"']?\\s+(?:\\S+)=|[>\"']))+.)[\"']?";

    public static final int MODE_XML = 1;
    public static final int MODE_GRADLE = 2;

    private final File mFile;
    private final String[] mAttributeNames;
    private final int mMode;

    private final UICallback uiCallback;

    public AttributeExtractor(File file, String[] attrNames, int mode, UICallback uiCallback) {
        mFile = file;
        mAttributeNames = attrNames;
        mMode = mode;
        this.uiCallback = uiCallback;
    }

    public static String getTagName(String tag) {
        int start = tag.indexOf('<');
        if (start < 0) return null;
        start += 1;
        int end = tag.indexOf(' ', start);
        if (end < 0) return null;
        return tag.substring(start, end);
    }

    public static String getAttributeValue(String name, String tag) {
        Pattern pattern = Pattern.compile(String.format(XML_REGEX, name));
        Matcher matcher = pattern.matcher(tag);
        if (matcher.find()) {
            String result = tag.substring(matcher.start(), matcher.end());
            result = result.substring(result.indexOf('=') + 1, result.length());
            if (result.startsWith("\"") && result.endsWith("\""))
                result = result.substring(1, result.length() - 1);
            return result;
        }
        return null;
    }

    public static String getElementValue(String tag) {
        try {
            int start = tag.indexOf('>');
            if (start < 0) return null;
            int end = tag.lastIndexOf("</");
            if (end < 0) return null;
            return tag.substring(start + 1, end);
        } catch (Throwable t) {
            return null;
        }
    }

    public static String setElementValue(String tag, String value) {
        int start = tag.indexOf('>') + 1;
        if (start < 0) return tag;
        int end = tag.lastIndexOf("</");
        if (end < 0) return tag;
        else if (start > end) return tag;
        StringBuilder sb = new StringBuilder(tag);
        sb.replace(start, end, value);
        return sb.toString();
    }

    public HashMap<String, String> find() {
        if (!mFile.exists()) {
            Main.LOG("[ERROR]: File %s does not exist.", mFile.getAbsolutePath());
            if (uiCallback != null) uiCallback.onErrorOccurred("File does not exist:\n" + mFile.getAbsolutePath());
            return null;
        }

        InputStream is = null;
        BufferedReader reader = null;
        Pattern[] patterns = null;
        if (mMode == MODE_XML) {
            patterns = new Pattern[mAttributeNames.length];
            for (int i = 0; i < patterns.length; i++)
                patterns[i] = Pattern.compile(String.format(XML_REGEX, mAttributeNames[i]));
        }

        final HashMap<String, String> results = new HashMap<>(mAttributeNames.length);

        try {
            is = new FileInputStream(mFile);
            reader = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = reader.readLine()) != null) {
                if (patterns != null) {
                    // XML
                    for (Pattern pattern : patterns) {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            String result = line.substring(matcher.start(), matcher.end());
                            String name = result.substring(0, result.indexOf('='));
                            result = result.substring(result.indexOf('=') + 1, result.length());
                            if ((result.startsWith("\"") && result.endsWith("\"")) ||
                                    result.startsWith("'") && result.endsWith("'")) {
                                result = result.substring(1, result.length() - 1);
                            }
                            results.put(name, result);
                            break;
                        }
                    }
                } else {
                    // Gradle
                    for (String attr : mAttributeNames) {
                        int start = line.indexOf(attr + " ");
                        if (start == -1) continue;
                        String result = line.substring(start, line.length()).trim();
                        String name = result.substring(0, result.indexOf(' '));
                        result = result.substring(result.indexOf(' ') + 1);
                        if ((result.startsWith("\"") && result.endsWith("\"")) ||
                                result.startsWith("'") && result.endsWith("'")) {
                            result = result.substring(1, result.length() - 1);
                        }
                        results.put(name, result);
                    }
                }
            }
        } catch (Exception e) {
            Main.LOG("[ERROR] Failed to read %s: %s", mFile.getAbsolutePath(), e.getMessage());
            if (uiCallback != null)
                uiCallback.onErrorOccurred("Failed to read " + mFile.getAbsolutePath() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            Util.closeQuietely(reader);
            Util.closeQuietely(is);
        }
        return results;
    }
}