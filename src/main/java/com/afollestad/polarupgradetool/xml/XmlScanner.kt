package com.afollestad.polarupgradetool.xml

/**
 * @author Aidan Follestad (afollestad)
 */
class XmlScanner(private val mXml: StringBuilder) {

    private var mIndex = 0
    private var mTagName: String = ""
    private var mTagValue: String = ""
    private var mTagStart: Int = 0
    private var mTagEnd: Int = 0
    private var mValueStart: Int = 0
    private var mValueEnd: Int = 0
    private var mReachedEnd = false

    fun tagName(): String {
        return mTagName
    }

    fun tagValue(): String {
        return mTagValue
    }

    fun reachedEnd(): Boolean {
        return mReachedEnd
    }

    fun currentTag(): String {
        return mXml.substring(mTagStart, mTagEnd)
    }

    fun nextTag(): String? {
        if (mReachedEnd) return null
        val next: Int
        val firstSpace: Int
        try {
            mTagStart = mXml.indexOf("<", mIndex)
            if (mTagStart < 0) {
                // No more tags in the file
                mReachedEnd = true
                return null
            } else if (mXml[mTagStart + 1] == '?') {
                // Skip header
                mIndex = mXml.indexOf("?>", mTagStart + 1) + 2
                return nextTag()
            } else if (mXml[mTagStart + 1] == '!') {
                // Skip comments
                mIndex = mXml.indexOf("-->", mTagStart + 1) + 3
                return nextTag()
            }

            next = mXml.indexOf(">", mTagStart)
            firstSpace = mXml.indexOf(" ", mTagStart)
            if (firstSpace == -1) {
                mReachedEnd = true
                return null
            } else if (firstSpace > next) {
                // Skip elements with no attributes
                mIndex = firstSpace + 1
                return nextTag()
            } else if (!mXml.substring(mTagStart, next).contains(" name=")) {
                // Skip elements with no name attribute
                mIndex = next + 1
                return nextTag()
            }

            mTagName = mXml.substring(mTagStart + 1, firstSpace)
            val endFindStr = "</$mTagName>"
            mTagEnd = mXml.indexOf(endFindStr, next + 1)
            if (mTagEnd < 0) {
                // Didn't find an end to this tag, skip it
                mIndex = mTagEnd + 1
                return nextTag()
            }
            mValueStart = next + 1
            mValueEnd = mTagEnd
            mTagValue = mXml.substring(mValueStart, mValueEnd)
            mTagEnd += endFindStr.length

            val tag = mXml.substring(mTagStart, mTagEnd)
            mIndex = mTagEnd
            return tag
        } catch (t: Throwable) {
            throw RuntimeException(t)
        }

    }

    //    public void reset() {
    //        mIndex = 0;
    //        mReachedEnd = false;
    //    }

    fun setElementValue(value: String) {
        val endFindStr = "</$mTagName>"
        mTagValue = value
        mXml.replace(mValueStart, mValueEnd, value)
        mValueEnd = mValueStart + mTagValue.length
        mTagEnd = mValueEnd + endFindStr.length
        mIndex = mTagEnd
    }

    override fun toString(): String {
        return mXml.substring(mIndex)
    }
}