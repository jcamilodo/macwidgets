package com.explodingpixels.util;

public class PlatformUtils {

    private PlatformUtils() {
        // utility class - no constructor needed.
    }

    /**
     * Get's the version of Java currently running.
     *
     * @return the version of Java that is running.
     */
    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }

    /**
     * True if this JVM is running on a Mac.
     *
     * @return true if this JVM is running on a Mac.
     */
    public static boolean isMac() {
        return System.getProperty("os.name").startsWith("Mac OS");
    }

    /**
     * True if this JVM is running Java 6 on a Mac.
     *
     * @return true if this JVM is running Java 6 on a Mac.
     */
    public static boolean isJava6OnMac() {
        return isMac() && getJavaVersion().startsWith("1.6");
    }

    /**
     * True if this JVM is running 64 bit Java on a Mac.
     *
     * @return true if this JVM is running 64 bit Java on a Mac.
     */
    public static boolean is64BitJavaOnMac() {
        return isMac() && System.getProperty("os.arch").equals("x86_64");
    }

}
