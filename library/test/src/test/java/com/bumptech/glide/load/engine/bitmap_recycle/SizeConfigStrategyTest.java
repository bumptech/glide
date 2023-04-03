package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class SizeConfigStrategyTest {

  @Mock private SizeConfigStrategy.KeyPool pool;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testKeyEquals() {
    new EqualsTester()
        .addEqualityGroup(
            new SizeConfigStrategy.Key(pool, 100, Bitmap.Config.ARGB_8888),
            new SizeConfigStrategy.Key(pool, 100, Bitmap.Config.ARGB_8888))
        .addEqualityGroup(new SizeConfigStrategy.Key(pool, 101, Bitmap.Config.ARGB_8888))
        .addEqualityGroup(new SizeConfigStrategy.Key(pool, 100, Bitmap.Config.RGB_565))
        .addEqualityGroup(new SizeConfigStrategy.Key(pool, 100, null /*config*/))
        .testEquals();
  }
}
