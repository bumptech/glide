package com.bumptech.glide.load.data.resource;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.ByteArrayFetcher;
import org.junit.Test;

import java.io.InputStream;

import static junit.framework.Assert.assertEquals;

public class ByteArrayFetcherTest {

    @Test
    public void testReturnsStreamWithBytes() throws Exception {
        byte[] bytes = new byte[10];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }

        ByteArrayFetcher fetcher = new ByteArrayFetcher(bytes);
        InputStream is = fetcher.loadData(Priority.NORMAL);

        int read = 0;
        byte current;
        while ((current = (byte) is.read()) != -1) {
            assertEquals(bytes[read], current);
            read++;
        }
        assertEquals(bytes.length, read);
    }
}
