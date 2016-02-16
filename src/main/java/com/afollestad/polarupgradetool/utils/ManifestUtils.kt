package com.afollestad.polarupgradetool.utils

import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL
import java.text.CharacterIterator
import java.text.StringCharacterIterator
import java.util.jar.JarFile

/**
 * Project : polarupgradetool
 * Author : pddstudio
 * Year : 2016
 */
object ManifestUtils {

    private val GITHUB_POM_URL = "https://raw.githubusercontent.com/afollestad/polar-dashboard-upgrade-tool/master/pom.xml"
    private val MANIFEST_PUT_VERSION = "PUT-Version"
    private val VERSION_UNKNOWN = "???"

    //e.printStackTrace();
    val remoteApplicationModel: Model?
        get() {
            try {
                val mavenXpp3Reader = MavenXpp3Reader()
                val mavenUrl = URL(GITHUB_POM_URL)
                val inputStreamReader = InputStreamReader(mavenUrl.openStream())
                val bufferedReader = BufferedReader(inputStreamReader)
                val pom = mavenXpp3Reader.read(bufferedReader)
                bufferedReader.close()
                return pom
            } catch (e: Exception) {
                return null
            }

        }

    fun getApplicationVersion(className: Class<*>): String {
        val jarName: File?
        try {

            jarName = getClassSource(className)
            if (jarName == null) return VERSION_UNKNOWN

            val jarFile = JarFile(jarName)
            val attributes = jarFile.manifest.mainAttributes ?: return VERSION_UNKNOWN

            for (key in attributes.keys) {
                if (key.toString() == MANIFEST_PUT_VERSION) return attributes[key].toString()
            }

        } catch (io: IOException) {
            io.printStackTrace()
        }

        return VERSION_UNKNOWN
    }

    fun getClassSource(className: Class<*>): File? {
        val classResource = className.name.replace(".", "/") + ".class"
        var classLoader: ClassLoader? = className.classLoader
        if (classLoader == null) classLoader = ManifestUtils::class.java.classLoader
        val url: URL?
        if (classLoader == null) {
            url = ClassLoader.getSystemResource(classResource)
        } else {
            url = classLoader.getResource(classResource)
        }
        if (url != null) {
            val urlString = url.toString()
            val split: Int
            val jarName: String
            if (urlString.startsWith("jar:file:")) {
                split = urlString.indexOf("!")
                jarName = urlString.substring(4, split)
                return File(fromUri(jarName))
            } else if (urlString.startsWith("file:")) {
                split = urlString.indexOf(classResource)
                jarName = urlString.substring(0, split)
                return File(fromUri(jarName))
            }
        }
        return null
    }

    private fun fromUri(uri: String): String {
        var newUri = uri
        var url: URL? = null
        try {
            url = URL(newUri)
        } catch (emYouEarlEx: MalformedURLException) {
            // Ignore malformed exception
        }

        if (url == null || "file" != url.protocol) {
            throw IllegalArgumentException("Can only handle valid file: URIs")
        }
        val buf = StringBuilder(url.host)
        if (buf.length > 0) {
            buf.insert(0, File.separatorChar).insert(0, File.separatorChar)
        }
        val file = url.file
        val queryPos = file.indexOf('?')
        buf.append(if (queryPos < 0) file else file.substring(0, queryPos))

        newUri = buf.toString().replace('/', File.separatorChar)

        if (File.pathSeparatorChar == ';' && newUri.startsWith("\\") && newUri.length > 2
                && Character.isLetter(newUri[1]) && newUri.lastIndexOf(':') > -1) {
            newUri = newUri.substring(1)
        }
        return decodeUri(newUri)
    }

    private fun decodeUri(uri: String): String {
        if (uri.indexOf('%') == -1) {
            return uri
        }
        val sb = StringBuilder()
        val iter = StringCharacterIterator(uri)
        var c = iter.first()
        while (c != CharacterIterator.DONE) {
            if (c == '%') {
                val c1 = iter.next()
                if (c1 != CharacterIterator.DONE) {
                    val i1 = Character.digit(c1, 16)
                    val c2 = iter.next()
                    if (c2 != CharacterIterator.DONE) {
                        val i2 = Character.digit(c2, 16)
                        sb.append(((i1 shl 4) + i2).toChar())
                    }
                }
            } else {
                sb.append(c)
            }
            c = iter.next()
        }
        return sb.toString()
    }
}