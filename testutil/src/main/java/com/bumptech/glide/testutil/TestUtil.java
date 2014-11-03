package com.bumptech.glide.testutil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Shared utility classes for tests.
 */
public final class TestUtil {
    private TestUtil() {
        // Utility class.
    }

    public static byte[] resourceToBytes(Class testClass, String resourceName) throws IOException {
        return isToBytes(TestResourceUtil.openResource(testClass, resourceName));
    }

    public static byte[] isToBytes(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        try {
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
        } finally {
            is.close();
        }
        return os.toByteArray();
    }

    public static String isToString(InputStream is) throws IOException {
        return new String(isToBytes(is));
    }

}
