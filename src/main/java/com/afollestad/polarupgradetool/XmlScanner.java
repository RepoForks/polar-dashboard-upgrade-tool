package com.afollestad.polarupgradetool;

/**
 * @author Aidan Follestad (afollestad)
 */
public class XmlScanner {

    private int mIndex = 0;
    private StringBuilder mXml;
    private String mTagName;
    private String mTagValue;
    private int mTagStart;
    private int mTagEnd;
    private int mValueStart;
    private int mValueEnd;
    private boolean mReachedEnd = false;

    public XmlScanner(StringBuilder xml) {
        mXml = xml;
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

    public String currentTag() {
        return mXml.substring(mTagStart, mTagEnd);
    }

    public String nextTag() {
        if (mReachedEnd) return null;
        try {
            mTagStart = mXml.indexOf("<", mIndex);
            if (mTagStart < 0) {
                // No more tags in the file
                mReachedEnd = true;
                return null;
            } else if (mXml.charAt(mTagStart + 1) == '?') {
                // Skip header
                mIndex = mXml.indexOf("?>", mTagStart + 1) + 2;
                return nextTag();
            } else if (mXml.charAt(mTagStart + 1) == '!') {
                // Skip comments
                mIndex = mXml.indexOf("-->", mTagStart + 1) + 3;
                return nextTag();
            }
            final int next = mXml.indexOf(">", mTagStart);
            final int firstSpace = mXml.indexOf(" ", mTagStart);
            if (firstSpace > next) {
                // Skip elements with no attributes
                mIndex = firstSpace + 1;
                return nextTag();
            }
            if (!mXml.substring(mTagStart, next).contains("name=")) {
                // Skip elements with no name attribute
                mIndex = next + 1;
                return nextTag();
            }
            mTagName = mXml.substring(mTagStart + 1, firstSpace);
            final String endFindStr = "</" + mTagName + ">";
            mTagEnd = mXml.indexOf(endFindStr, next + 1);
            if (mTagEnd < 0) {
                // Didn't find an end to this tag, skip it
                mIndex = mTagEnd + 1;
                return nextTag();
            }
            mValueStart = next + 1;
            mValueEnd = mTagEnd;
            mTagValue = mXml.substring(mValueStart, mValueEnd);
//            mTagEnd += endFindStr.length();
            final String tag = mXml.substring(mTagStart, mTagEnd);
            mIndex = mTagEnd;
            return tag;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

//    public void reset() {
//        mIndex = 0;
//        mReachedEnd = false;
//    }

    public void setElementValue(String value) {
        mTagValue = value;
        mXml.replace(mValueStart, mValueEnd, value);
        final String endFindStr = "</" + mTagName + ">";
        mValueEnd = mXml.indexOf(endFindStr, mValueStart);
        mTagEnd = mValueEnd + endFindStr.length();
        System.out.print("\0");
    }

    @Override
    public String toString() {
        return mXml.substring(mIndex);
    }
}