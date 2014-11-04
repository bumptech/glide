package com.bumptech.glide.load.data.resource;

import static org.junit.Assert.assertEquals;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.ByteArrayFetcher;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.InputStream;

@RunWith(JUnit4.class)
public class ByteArrayFetcherTest {

    @Test
    public void testReturnsStreamWithBytes() throws Exception {
        byte[] bytes = new byte[10];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }

        ByteArrayFetcher fetcher = new ByteArrayFetcher(bytes, "testId");
        InputStream is = fetcher.loadData(Priority.NORMAL);

        int read = 0;
        byte current;
        while ((current = (byte) is.read()) != -1) {
            assertEquals(bytes[read], current);
            read++;
        }
        assertEquals(bytes.length, read);
    }

    @Test
    public void testReturnsGivenId() {
        String expected = "fakeId";
        ByteArrayFetcher fetcher = new ByteArrayFetcher(new byte[0], expected);
        assertEquals(expected, fetcher.getId());
    }
}
