package com.bumptech.glide.tests;

import static org.junit.Assert.assertEquals;

import android.os.Build;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

    public static void writeFile(File file, byte[] data) throws IOException {
        OutputStream out = new FileOutputStream(file);
        try {
            out.write(data);
            out.flush();
            out.close();
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
                // Do nothing.
            }
        }
    }

    public static byte[] readFile(File file, int expectedLength) throws IOException {
        InputStream is = new FileInputStream(file);
        byte[] result = new byte[expectedLength];
        try {
            assertEquals(expectedLength, is.read(result));
            assertEquals(-1, is.read());
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // Do nothing.
            }
        }
        return result;
    }

    public static <T> Answer<T> arg(final int argumentIndex) {
        return new Answer<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T answer(InvocationOnMock invocation) {
                if (argumentIndex >= invocation.getArguments().length) {
                    throw new IllegalArgumentException("Cannot invoke argument " + argumentIndex
                            + " greater than arguments length " + invocation.getArguments().length);
                }
                return (T) invocation.getArguments()[argumentIndex];
            }
        };
    }

    public static void setSdkVersionInt(int version) {
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK_INT", version);
    }
}
