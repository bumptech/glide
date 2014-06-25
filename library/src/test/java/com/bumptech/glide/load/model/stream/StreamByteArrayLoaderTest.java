package com.bumptech.glide.load.model.stream;

import com.bumptech.glide.load.data.DataFetcher;
import org.junit.Test;

import java.io.InputStream;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;

public class StreamByteArrayLoaderTest {

    @Test
    public void testCanHandleByteArray() {
        StreamByteArrayLoader loader = new StreamByteArrayLoader();

        byte[] data = new byte[10];
        DataFetcher<InputStream> fetcher = loader.getResourceFetcher(data, -1, -1);
        assertNotNull(fetcher);
    }

    @Test
    public void testFetcherReturnsGivenId() {
        String id = "testId";
        StreamByteArrayLoader loader = new StreamByteArrayLoader(id);

        assertEquals(id, loader.getResourceFetcher(new byte[0], 1, 1).getId());
    }
}
