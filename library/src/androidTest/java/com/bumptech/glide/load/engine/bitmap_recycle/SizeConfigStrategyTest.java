package com.bumptech.glide.load.engine.bitmap_recycle;

import com.google.common.testing.EqualsTester;

import android.graphics.Bitmap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SizeConfigStrategyTest {

    @Mock SizeConfigStrategy.KeyPool pool;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testKeyEquals() {
        new EqualsTester()
                .addEqualityGroup(
                        new SizeConfigStrategy.Key(pool, 100, Bitmap.Config.ARGB_8888),
                        new SizeConfigStrategy.Key(pool, 100, Bitmap.Config.ARGB_8888)
                )
                .addEqualityGroup(
                        new SizeConfigStrategy.Key(pool, 101, Bitmap.Config.ARGB_8888)
                )
                .addEqualityGroup(
                        new SizeConfigStrategy.Key(pool, 100, Bitmap.Config.RGB_565)
                )
                .addEqualityGroup(
                        new SizeConfigStrategy.Key(pool, 100, null /*config*/)
                )
                .testEquals();

    }
}