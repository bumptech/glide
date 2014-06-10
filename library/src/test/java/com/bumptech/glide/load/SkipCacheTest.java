package com.bumptech.glide.load;

import com.bumptech.glide.Resource;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.mockito.Mockito.mock;

public class SkipCacheTest {

    @Test
    public void testEncode() throws Exception {
        SkipCache skipCache = new SkipCache();
        Resource resource = mock(Resource.class);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        skipCache.encode(resource, os);

        assertEquals(0, os.toByteArray().length);
    }

    @Test
    public void testReturnsFalseFromEncode() {
        SkipCache skipCache = new SkipCache();

        assertFalse(skipCache.encode(mock(Resource.class), new ByteArrayOutputStream()));
    }
}
