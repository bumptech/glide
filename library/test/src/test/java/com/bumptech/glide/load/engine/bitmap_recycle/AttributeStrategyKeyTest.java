package com.bumptech.glide.load.engine.bitmap_recycle;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.graphics.Bitmap;
import com.bumptech.glide.load.engine.bitmap_recycle.AttributeStrategy.Key;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class AttributeStrategyKeyTest {

  private AttributeStrategy.KeyPool keyPool;

  @Before
  public void setUp() {
    keyPool = mock(AttributeStrategy.KeyPool.class);
  }

  @Test
  public void testEquality() {
    Key first = new Key(keyPool);
    first.init(100, 100, Bitmap.Config.ARGB_4444);
    Key second = new Key(keyPool);
    second.init(100, 100, Bitmap.Config.ARGB_4444);

    Key third = new Key(keyPool);
    third.init(200, 100, Bitmap.Config.ARGB_4444);

    Key fourth = new Key(keyPool);
    fourth.init(100, 200, Bitmap.Config.ARGB_4444);

    Key fifth = new Key(keyPool);
    fifth.init(100, 100, Bitmap.Config.RGB_565);

    new EqualsTester()
        .addEqualityGroup(first, second)
        .addEqualityGroup(third)
        .addEqualityGroup(fourth)
        .addEqualityGroup(fifth)
        .testEquals();
  }

  @Test
  public void testReturnsSelfToPoolOnOffer() {
    Key key = new Key(keyPool);
    key.offer();

    verify(keyPool).offer(eq(key));
  }

  @Test
  public void testInitSetsAttributes() {
    Key key = new Key(keyPool);
    key.init(100, 100, Bitmap.Config.ARGB_4444);

    Key other = new Key(keyPool);
    other.init(200, 200, Bitmap.Config.RGB_565);

    assertNotEquals(key, other);

    key.init(200, 200, Bitmap.Config.RGB_565);

    assertEquals(key, other);
  }
}
