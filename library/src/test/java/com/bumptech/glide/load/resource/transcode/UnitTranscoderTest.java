package com.bumptech.glide.load.resource.transcode;

import com.bumptech.glide.Resource;
import com.bumptech.glide.tests.Util;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class UnitTranscoderTest {

    @Test
    public void testReturnsTheGivenResource() {
        Resource resource = mock(Resource.class);
        ResourceTranscoder<Object, Object> unitTranscoder = UnitTranscoder.get();

        assertEquals(resource, unitTranscoder.transcode(resource));
    }

    @Test
    public void testHasValidId() {
        Util.assertClassHasValidId(UnitTranscoder.class, UnitTranscoder.get().getId());
    }

}
