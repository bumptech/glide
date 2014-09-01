package com.bumptech.glide.tests;

import static junit.framework.Assert.assertEquals;

public class Util {

    public static String getExpectedClassId(Class clazz) {
        return clazz.getSimpleName() + "." + clazz.getPackage().getName();
    }

    public static void assertClassHasValidId(Class clazz, String id) {
        assertEquals(getExpectedClassId(clazz), id);
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

}
