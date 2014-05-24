package com.bumptech.glide.loader.bitmap.model.stream;

import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.InputStream;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class StreamByteArrayLoaderTest {

    @Test
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

    @Test
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
