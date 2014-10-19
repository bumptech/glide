package com.bumptech.glide.gifdecoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class TestUtil {

    private TestUtil() {
        // Utility class.
    }

    private static InputStream openResource(String imageName) throws IOException {
        return TestUtil.class.getResourceAsStream("/" + imageName);
    }

    public static byte[] readResourceData(String imageName) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        InputStream is = null;
        try {
            is = openResource(imageName);
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore.
                }
            }
        }
        return os.toByteArray();
    }
}
