package com.bumptech.glide.loader.bitmap.resource;

import android.test.AndroidTestCase;

import java.io.InputStream;

public class ByteArrayFetcherTest extends AndroidTestCase {

    public void testReturnsStreamWithBytes() throws Exception {
        byte[] bytes = new byte[10];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }

        ByteArrayFetcher fetcher = new ByteArrayFetcher(bytes, "");
        InputStream is = fetcher.loadResource();

        int read = 0;
        byte current;
        while ((current = (byte) is.read()) != -1) {
            assertEquals(bytes[read], current);
            read++;
        }
        assertEquals(bytes.length, read);
    }

    public void testReturnsGivenId() {
        String id = "testId";
        ByteArrayFetcher fetcher = new ByteArrayFetcher(new byte[10], id);
        assertEquals(id, fetcher.getId());
    }
}
