package com.bumptech.glide.loader.bitmap.model;

import android.test.ActivityTestCase;
import com.bumptech.glide.loader.bitmap.model.stream.StreamByteArrayLoader;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;

import java.io.InputStream;

public class StreamByteArrayLoaderTest extends ActivityTestCase {

    public void testCanHandleByteArray() {
        StreamByteArrayLoader loader = new StreamByteArrayLoader() {
            @Override
            public String getId(byte[] model) {
                return "id";
            }
        };

        byte[] data = new byte[10];
        ResourceFetcher<InputStream> fetcher = loader.getResourceFetcher(data, -1, -1);
        assertNotNull(fetcher);
    }

    public void testReturnsGivenId() {
        final String id = "testId";
        StreamByteArrayLoader loader = new StreamByteArrayLoader() {
            @Override
            public String getId(byte[] model) {
                return id;
            }
        };
        assertEquals(id, loader.getId(new byte[10]));
    }
}
