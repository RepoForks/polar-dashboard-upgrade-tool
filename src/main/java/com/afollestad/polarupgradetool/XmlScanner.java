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
    private boolean mReachedEnd = false;

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

    public boolean reachedEnd() {
        return mReachedEnd;
    }

    public String nextTag() {
        try {
            final int start = mXml.indexOf("<", mIndex);
            if (start < 0) {
                mReachedEnd = true;
                return null;
            } else if (mXml.charAt(start + 1) == '!') {
                // Skip comments
                mIndex = start + 1;
                return nextTag();
            }
            final int next = mXml.indexOf(">", start);
            if (!mXml.substring(start, next).contains("name=")) {
                mIndex = next + 1;
                return nextTag();
            }
            final int firstSpace = mXml.indexOf(" ", start);
            if (firstSpace > next) {
                mIndex = firstSpace + 1;
                return nextTag();
            }
            mTagName = mXml.substring(start + 1, firstSpace);
            final String endFindStr = "</" + mTagName + ">";
            int end = mXml.indexOf(endFindStr, next + 1);
            if (end < 0) {
                mIndex = end + 1;
                return nextTag();
            }
            mValueStart = next + 1;
            mValueEnd = end;
            mTagValue = mXml.substring(mValueStart, mValueEnd);
            end += endFindStr.length();
            final String tag = mXml.substring(start, end);
            mIndex = end;
            return tag;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public void setElementValue(String value) {
        mXml.replace(mValueStart, mValueEnd, value);
    }

    @Override
    public String toString() {
        return mXml.substring(mIndex);
    }
}