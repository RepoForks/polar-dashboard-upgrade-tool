package com.afollestad.polarupgradetool;

/**
 * @author Aidan Follestad (afollestad)
 */
public class XmlScanner {

    private int mIndex = 0;
    private StringBuilder mXml;
    private String mTagName;
    private String mTagValue;
    private int mValueStart;
    private int mValueEnd;

    public XmlScanner(StringBuilder xml) {
        mXml = xml;
        // Skip over the root tag
        mIndex = mXml.indexOf(">") + 1;
    }

//    public void updateXml(StringBuilder xml) {
//        mXml = xml;
//    }

    public String tagName() {
        return mTagName;
    }

    public String tagValue() {
        return mTagValue;
    }

    public String nextTag() {
        final int start = mXml.indexOf("<", mIndex);
        if (start < 0)
            return null;
        else if (mXml.charAt(start + 1) == '!') {
            // Skip comments
            mIndex = start + 1;
            return nextTag();
        }
        final int next = mXml.indexOf(">", start);
        if (!mXml.substring(start, next).contains("name="))
            return null;
        final int firstSpace = mXml.indexOf(" ", start);
        if (firstSpace > next)
            return null;
        mTagName = mXml.substring(start + 1, firstSpace);
        final String endFindStr = "</" + mTagName + ">";
        int end = mXml.indexOf(endFindStr, next + 1);
        if (end < 0)
            return null;
        mValueStart = next + 1;
        mValueEnd = end;
        mTagValue = mXml.substring(mValueStart, mValueEnd);
        end += endFindStr.length();
        final String tag = mXml.substring(start, end);
        mIndex = end + 1;
        return tag;
    }

    public void setElementValue(String value) {
        mXml.replace(mValueStart, mValueEnd, value);
    }
}