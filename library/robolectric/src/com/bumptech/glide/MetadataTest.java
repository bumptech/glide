package com.bumptech.glide;

import com.bumptech.glide.Metadata;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DecodeFormat;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MetadataTest {

    @Test(expected = NullPointerException.class)
    public void testNullPriorityThrows() {
        new Metadata(null, DecodeFormat.ALWAYS_ARGB_8888);
    }

    @Test(expected = NullPointerException.class)
    public void testNullDecodeFormatThrows() {
        new Metadata(Priority.IMMEDIATE, null);
    }

    @Test
    public void testCanGetPriority() {
        assertEquals(Priority.IMMEDIATE, new Metadata(Priority.IMMEDIATE, DecodeFormat.ALWAYS_ARGB_8888).getPriority());
    }

    @Test
    public void testCanGetDecodeFormat() {
        assertEquals(DecodeFormat.ALWAYS_ARGB_8888,
                new Metadata(Priority.LOW, DecodeFormat.ALWAYS_ARGB_8888).getDecodeFormat());
    }

    @Test
    public void testMetadataWithSameArgsAreEqual() {
        assertEquals(new Metadata(Priority.LOW, DecodeFormat.PREFER_RGB_565),
                new Metadata(Priority.LOW, DecodeFormat.PREFER_RGB_565));
    }

}
