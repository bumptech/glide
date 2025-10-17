package com.bumptech.glide.request.transition;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.graphics.drawable.Drawable;
import com.bumptech.glide.load.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class DrawableCrossFadeFactoryTest {

  private DrawableCrossFadeFactory factory;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    factory = new DrawableCrossFadeFactory(100 /*duration*/, false /*isCrossFadeEnabled*/);
  }

  @Test
  public void testReturnsNoAnimationIfFromMemoryCache() {
    assertEquals(
        NoTransition.<Drawable>get(),
        factory.build(DataSource.MEMORY_CACHE, true /*isFirstResource*/));
  }

  @Test
  public void testReturnsReturnsAnimationIfNotFromMemoryCacheAndIsFirstResource() {
    assertNotEquals(
        NoTransition.<Drawable>get(),
        factory.build(DataSource.DATA_DISK_CACHE, true /*isFirstResource*/));
  }

  @Test
  public void testReturnsAnimationIfNotFromMemoryCacheAndNotIsFirstResource() {
    assertNotEquals(
        NoTransition.<Drawable>get(),
        factory.build(DataSource.DATA_DISK_CACHE, false /*isFirstResource*/));
  }
}
