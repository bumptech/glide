package com.bumptech.glide.load.engine.cache;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculatorTest.LowRamActivityManager;
import com.bumptech.glide.tests.Util;
import com.google.common.collect.Range;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivityManager;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 19, shadows = LowRamActivityManager.class)
public class MemorySizeCalculatorTest {
  private MemorySizeHarness harness;
  private int initialSdkVersion;

  @Before
  public void setUp() {
    initialSdkVersion = Build.VERSION.SDK_INT;
    Util.setSdkVersionInt(18);
    harness = new MemorySizeHarness();
  }

  @After
  public void tearDown() {
    Util.setSdkVersionInt(initialSdkVersion);
  }

  @Test
  public void testDefaultMemoryCacheSizeIsTwiceScreenSize() {
    Shadows.shadowOf(harness.activityManager).setMemoryClass(getLargeEnoughMemoryClass());

    float memoryCacheSize = harness.getCalculator().getMemoryCacheSize();

    assertThat(memoryCacheSize).isEqualTo(harness.getScreenSize() * harness.memoryCacheScreens);
  }

  @Test
  public void testCanSetCustomMemoryCacheSize() {
    harness.memoryCacheScreens = 9.5f;
    Shadows.shadowOf(harness.activityManager).setMemoryClass(getLargeEnoughMemoryClass());

    float memoryCacheSize = harness.getCalculator().getMemoryCacheSize();

    assertThat(memoryCacheSize).isEqualTo(harness.getScreenSize() * harness.memoryCacheScreens);
  }

  @Test
  public void testDefaultMemoryCacheSizeIsLimitedByMemoryClass() {
    final int memoryClassBytes =
        Math.round(harness.getScreenSize() * harness.memoryCacheScreens * harness.sizeMultiplier);

    Shadows.shadowOf(harness.activityManager).setMemoryClass(memoryClassBytes / (1024 * 1024));

    float memoryCacheSize = harness.getCalculator().getMemoryCacheSize();

    assertThat(memoryCacheSize)
        .isIn(Range.atMost(memoryClassBytes * harness.sizeMultiplier));
  }

  @Test
  public void testDefaultBitmapPoolSize() {
    Shadows.shadowOf(harness.activityManager).setMemoryClass(getLargeEnoughMemoryClass());

    float bitmapPoolSize = harness.getCalculator().getBitmapPoolSize();

    assertThat(bitmapPoolSize).isEqualTo(harness.getScreenSize() * harness.bitmapPoolScreens);
  }

  @Test
  public void testCanSetCustomBitmapPoolSize() {
    harness.bitmapPoolScreens = 2f;
    Shadows.shadowOf(harness.activityManager).setMemoryClass(getLargeEnoughMemoryClass());

    float bitmapPoolSize = harness.getCalculator().getBitmapPoolSize();

    assertThat(bitmapPoolSize).isEqualTo(harness.getScreenSize() * harness.bitmapPoolScreens);
  }

  @Test
  public void testDefaultBitmapPoolSizeIsLimitedByMemoryClass() {
    final int memoryClassBytes =
        Math.round(harness.getScreenSize() * harness.bitmapPoolScreens * harness.sizeMultiplier);

    Shadows.shadowOf(harness.activityManager).setMemoryClass(memoryClassBytes / (1024 * 1024));

    int bitmapPoolSize = harness.getCalculator().getBitmapPoolSize();

    assertThat((float) bitmapPoolSize)
        .isIn(Range.atMost(memoryClassBytes * harness.sizeMultiplier));
  }

  @Test
  public void testCumulativePoolAndMemoryCacheSizeAreLimitedByMemoryClass() {
    final int memoryClassBytes = Math.round(
        harness.getScreenSize() * (harness.bitmapPoolScreens + harness.memoryCacheScreens)
            * harness.sizeMultiplier);
    Shadows.shadowOf(harness.activityManager).setMemoryClass(memoryClassBytes / (1024 * 1024));

    int memoryCacheSize = harness.getCalculator().getMemoryCacheSize();
    int bitmapPoolSize = harness.getCalculator().getBitmapPoolSize();

    assertThat((float) memoryCacheSize + bitmapPoolSize)
        .isIn(Range.atMost(memoryClassBytes * harness.sizeMultiplier));
  }

  @Test
  public void testCumulativePoolAndMemoryCacheSizesAreSmallerOnLowMemoryDevices() {
    Shadows.shadowOf(harness.activityManager).setMemoryClass(getLargeEnoughMemoryClass() / 2);
    final int normalMemoryCacheSize = harness.getCalculator().getMemoryCacheSize();
    final int normalBitmapPoolSize = harness.getCalculator().getBitmapPoolSize();

    Util.setSdkVersionInt(10);

    // Keep the bitmap pool size constant, even though normally it would change.
    harness.byteArrayPoolSizeBytes *= 2;
    final int smallMemoryCacheSize = harness.getCalculator().getMemoryCacheSize();
    final int smallBitmapPoolSize = harness.getCalculator().getBitmapPoolSize();

    assertThat(smallMemoryCacheSize).isLessThan(normalMemoryCacheSize);
    assertThat(smallBitmapPoolSize).isLessThan(normalBitmapPoolSize);
  }

  @Test
  public void testByteArrayPoolSize_withLowRamDevice_isHalfTheSpecifiedBytes() {
    LowRamActivityManager activityManager =
        (LowRamActivityManager) Shadow.extract(harness.activityManager);
    Util.setSdkVersionInt(19);
    activityManager.setMemoryClass(getLargeEnoughMemoryClass());
    activityManager.setIsLowRam(true);

    int byteArrayPoolSize = harness.getCalculator().getArrayPoolSizeInBytes();
    assertThat(byteArrayPoolSize).isEqualTo(harness.byteArrayPoolSizeBytes / 2);
  }

  private int getLargeEnoughMemoryClass() {
    float totalScreenBytes =
        harness.getScreenSize() * (harness.bitmapPoolScreens + harness.memoryCacheScreens);
    float totalBytes = totalScreenBytes + harness.byteArrayPoolSizeBytes;
    // Memory class is in mb, not bytes!
    float totalMb = totalBytes / (1024 * 1024);
    float memoryClassMb = totalMb / harness.sizeMultiplier;
    return (int) Math.ceil(memoryClassMb);
  }

  private static class MemorySizeHarness {
    int pixelSize = 500;
    int bytesPerPixel = MemorySizeCalculator.BYTES_PER_ARGB_8888_PIXEL;
    float memoryCacheScreens = MemorySizeCalculator.Builder.MEMORY_CACHE_TARGET_SCREENS;
    float bitmapPoolScreens = MemorySizeCalculator.Builder.BITMAP_POOL_TARGET_SCREENS;
    float sizeMultiplier = MemorySizeCalculator.Builder.MAX_SIZE_MULTIPLIER;
    int byteArrayPoolSizeBytes = MemorySizeCalculator.Builder.ARRAY_POOL_SIZE_BYTES;
    ActivityManager activityManager =
        (ActivityManager) RuntimeEnvironment.application.getSystemService(Context.ACTIVITY_SERVICE);
    MemorySizeCalculator.ScreenDimensions screenDimensions =
        mock(MemorySizeCalculator.ScreenDimensions.class);

    public MemorySizeCalculator getCalculator() {
      when(screenDimensions.getWidthPixels()).thenReturn(pixelSize);
      when(screenDimensions.getHeightPixels()).thenReturn(pixelSize);
      return new MemorySizeCalculator.Builder(RuntimeEnvironment.application)
          .setMemoryCacheScreens(memoryCacheScreens)
          .setBitmapPoolScreens(bitmapPoolScreens)
          .setMaxSizeMultiplier(sizeMultiplier)
          .setActivityManager(activityManager)
          .setScreenDimensions(screenDimensions)
          .setArrayPoolSize(byteArrayPoolSizeBytes)
          .build();
    }

    public int getScreenSize() {
      return pixelSize * pixelSize * bytesPerPixel;
    }
  }

  @Implements(ActivityManager.class)
  public static final class LowRamActivityManager extends ShadowActivityManager {

    private boolean isLowRam;

    void setIsLowRam(boolean isLowRam) {
      this.isLowRam = isLowRam;
    }

    @Implementation
    public boolean isLowRamDevice() {
      return isLowRam;
    }
  }
}
