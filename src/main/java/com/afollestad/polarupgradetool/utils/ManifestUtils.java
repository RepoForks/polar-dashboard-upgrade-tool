package com.afollestad.polarupgradetool.utils;

import org.apache.maven.model.v3_0_0.Model;
import org.apache.maven.model.v3_0_0.io.xpp3.MavenXpp3Reader;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Project : polarupgradetool
 * Author : pddstudio
 * Year : 2016
 */
public class ManifestUtils {

    private static final String GITHUB_POM_URL = "https://raw.githubusercontent.com/afollestad/polar-dashboard-upgrade-tool/master/pom.xml";
    private static final String MANIFEST_PUT_VERSION = "PUT-Version";
    private static final String VERSION_UNKNOWN = "???";

    public static void getGithubApplicationVersion() {
        try {
            MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();
            URL mavenUrl = new URL(GITHUB_POM_URL);
            Model pom = mavenXpp3Reader.read(new InputStreamReader(mavenUrl.openStream()));
            System.out.println("Pom Name: " + pom.getName() + " Artifact ID: " + pom.getArtifactId());
            System.out.println("Current Version: " + pom.getCurrentVersion() + " Group ID: " + pom.getGroupId());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static String getApplicationVersion(Class<?> className) {
        File jarName;
        try {

            jarName = getClassSource(className);
            if(jarName == null) return VERSION_UNKNOWN;

            JarFile jarFile = new JarFile(jarName);
            Attributes attributes = jarFile.getManifest().getMainAttributes();
            if(attributes == null) return VERSION_UNKNOWN;

            for(Object key : attributes.keySet()) {
                if(key.toString().equals(MANIFEST_PUT_VERSION)) return attributes.get(key).toString();
            }

        } catch (IOException io) {
            io.printStackTrace();
        }

        return VERSION_UNKNOWN;
    }

    public static File getClassSource(Class<?> className) {
        String classResource = className.getName().replace(".", "/") + ".class";
        ClassLoader classLoader = className.getClassLoader();
        if(classLoader == null) classLoader = ManifestUtils.class.getClassLoader();
        URL url;
        if(classLoader == null) {
            url = ClassLoader.getSystemResource(classResource);
        } else {
            url = classLoader.getResource(classResource);
        }
        if(url != null) {
            String urlString = url.toString();
            int split;
            String jarName;
            if(urlString.startsWith("jar:file:")) {
                split = urlString.indexOf("!");
                jarName = urlString.substring(4, split);
                return new File(fromUri(jarName));
            } else if(urlString.startsWith("file:")) {
                split = urlString.indexOf(classResource);
                jarName = urlString.substring(0, split);
                return new File(fromUri(jarName));
            }
        }
        return null;
    }

    private static String fromUri(String uri) {
        URL url = null;
        try
        {
            url = new URL(uri);
        }
        catch(MalformedURLException emYouEarlEx)
        {
            // Ignore malformed exception
        }
        if(url == null || !("file".equals(url.getProtocol())))
        {
            throw new IllegalArgumentException("Can only handle valid file: URIs");
        }
        StringBuffer buf = new StringBuffer(url.getHost());
        if(buf.length() > 0)
        {
            buf.insert(0, File.separatorChar).insert(0, File.separatorChar);
        }
        String file = url.getFile();
        int queryPos = file.indexOf('?');
        buf.append((queryPos < 0) ? file : file.substring(0, queryPos));

        uri = buf.toString().replace('/', File.separatorChar);

        if(File.pathSeparatorChar == ';' && uri.startsWith("\\") && uri.length() > 2
                && Character.isLetter(uri.charAt(1)) && uri.lastIndexOf(':') > -1)
        {
            uri = uri.substring(1);
        }
        String path = decodeUri(uri);
        return path;
    }

    private static String decodeUri(String uri) {
        if(uri.indexOf('%') == -1)
        {
            return uri;
        }
        StringBuffer sb = new StringBuffer();
        CharacterIterator iter = new StringCharacterIterator(uri);
        for(char c = iter.first(); c != CharacterIterator.DONE; c = iter.next())
        {
            if(c == '%')
            {
                char c1 = iter.next();
                if(c1 != CharacterIterator.DONE)
                {
                    int i1 = Character.digit(c1, 16);
                    char c2 = iter.next();
                    if(c2 != CharacterIterator.DONE)
                    {
                        int i2 = Character.digit(c2, 16);
                        sb.append((char) ((i1 << 4) + i2));
                    }
                }
            }
            else
            {
                sb.append(c);
            }
        }
        String path = sb.toString();
        return path;
    }

    private static Object getValueFromAttr(Attributes attributes, String manifestValue) {
        for(Object key : attributes.keySet()) {
            if(key.toString().equals(manifestValue)) return attributes.get(key);
        }
        return null;
    }

}
