package com.afollestad.polarupgradetool;

/**
 * @author Aidan Follestad (afollestad)
 */
public class XmlScanner {

    private final int mInitialIndex;
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
        // Skip over the root tag
        mInitialIndex = mIndex = mXml.indexOf(">") + 1;
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
        try {
            mTagStart = mXml.indexOf("<", mIndex);
            if (mTagStart < 0) {
                // No more tags in the file
                mReachedEnd = true;
                return null;
            } else if (mXml.charAt(mTagStart + 1) == '!') {
                // Skip comments
                mIndex = mTagStart + 1;
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
            mTagEnd += endFindStr.length();
            final String tag = mXml.substring(mTagStart, mTagEnd);
            mIndex = mTagEnd;
            return tag;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public void reset() {
        mIndex = mInitialIndex;
        mReachedEnd = false;
    }

    public void setElementValue(String value) {
        final String formerValue = mTagValue;
        mTagValue = value;
        final int difference = mTagValue.length() - formerValue.length();
        mXml.replace(mValueStart, mValueEnd, value);
        mValueEnd += difference;
        mTagEnd += difference;
    }

    @Override
    public String toString() {
        return mXml.substring(mIndex);
    }
}