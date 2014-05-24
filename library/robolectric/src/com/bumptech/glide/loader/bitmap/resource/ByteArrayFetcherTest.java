package com.bumptech.glide.loader.bitmap.resource;

import com.bumptech.glide.resize.Metadata;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.InputStream;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class ByteArrayFetcherTest {

    @Test
    public void testReturnsStreamWithBytes() throws Exception {
        byte[] bytes = new byte[10];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }

        ByteArrayFetcher fetcher = new ByteArrayFetcher(bytes);
        InputStream is = fetcher.loadResource(mock(Metadata.class));

        int read = 0;
        byte current;
        while ((current = (byte) is.read()) != -1) {
            assertEquals(bytes[read], current);
            read++;
        }
        assertEquals(bytes.length, read);
    }
}
