package com.bumptech.glide.load.engine.cache;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Range;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import com.bumptech.glide.tests.Util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class MemorySizeCalculatorTest {
    private MemorySizeHarness harness;
    private int initialSdkVersion;

    @Before
    public void setUp() {
        initialSdkVersion = Build.VERSION.SDK_INT;
        harness = new MemorySizeHarness();
    }

    @After
    public void tearDown() {
        Util.setSdkVersionInt(initialSdkVersion);
    }

    @Test
    public void testDefaultMemoryCacheSizeIsTwiceScreenSize() {
        Robolectric.shadowOf(harness.activityManager).setMemoryClass(getLargeEnoughMemoryClass());

        int memoryCacheSize = harness.getCalculator().getMemoryCacheSize();

        assertEquals(harness.getScreenSize() * harness.memoryCacheScreens , memoryCacheSize);
    }

    @Test
    public void testDefaultMemoryCacheSizeIsLimitedByMemoryClass() {
        final int memoryClassBytes = Math.round(harness.getScreenSize() * harness.memoryCacheScreens
                * harness.sizeMultiplier);

        Robolectric.shadowOf(harness.activityManager).setMemoryClass(memoryClassBytes / (1024 * 1024));

        int memoryCacheSize = harness.getCalculator().getMemoryCacheSize();

        assertThat((float) memoryCacheSize).isIn(Range.atMost(memoryClassBytes * harness.sizeMultiplier));
    }

    @Test
    public void testDefaultBitmapPoolSize() {
        Robolectric.shadowOf(harness.activityManager).setMemoryClass(getLargeEnoughMemoryClass());

        int bitmapPoolSize = harness.getCalculator().getBitmapPoolSize();

//        assertThat(bitmapPoolSize).isIn(Range.open());
        assertEquals(harness.getScreenSize() * harness.bitmapPoolScreens, bitmapPoolSize);
    }

    @Test
    public void testDefaultBitmapPoolSizeIsLimitedByMemoryClass() {
        final int memoryClassBytes = Math.round(harness.getScreenSize() * harness.bitmapPoolScreens
                * harness.sizeMultiplier);

        Robolectric.shadowOf(harness.activityManager).setMemoryClass(memoryClassBytes / (1024 * 1024));

        int bitmapPoolSize = harness.getCalculator().getBitmapPoolSize();

        assertThat((float) bitmapPoolSize).isIn(Range.atMost(memoryClassBytes * harness.sizeMultiplier));
    }

    @Test
    public void testCumulativePoolAndMemoryCacheSizeAreLimitedByMemoryClass() {
        final int memoryClassBytes = Math.round(harness.getScreenSize()
                * (harness.bitmapPoolScreens + harness.memoryCacheScreens) * harness.sizeMultiplier);
        Robolectric.shadowOf(harness.activityManager).setMemoryClass(memoryClassBytes / (1024 * 1024));

        int memoryCacheSize = harness.getCalculator().getMemoryCacheSize();
        int bitmapPoolSize = harness.getCalculator().getBitmapPoolSize();

        assertThat((float) memoryCacheSize + bitmapPoolSize).isIn(
                Range.atMost(memoryClassBytes * harness.sizeMultiplier));
    }

    @Test
    public void testCumulativePoolAndMemoryCacheSizesAreSmallerOnLowMemoryDevices() {
        Robolectric.shadowOf(harness.activityManager).setMemoryClass(getLargeEnoughMemoryClass() / 2);
        final int normalMemoryCacheSize = harness.getCalculator().getMemoryCacheSize();
        final int normalBitmapPoolSize = harness.getCalculator().getBitmapPoolSize();

        Util.setSdkVersionInt(10);

        final int smallMemoryCacheSize = harness.getCalculator().getMemoryCacheSize();
        final int smallBitmapPoolSize = harness.getCalculator().getBitmapPoolSize();

        assertThat(smallMemoryCacheSize).isLessThan(normalMemoryCacheSize);
        assertThat(smallBitmapPoolSize).isLessThan(normalBitmapPoolSize);
    }

    private int getLargeEnoughMemoryClass() {
        float totalScreenBytes = harness.getScreenSize() * (harness.bitmapPoolScreens + harness.memoryCacheScreens);
        // Memory class is in mb, not bytes!
        float totalScreenMb = totalScreenBytes / (1024 * 1024);
        float memoryClassMb = totalScreenMb / harness.sizeMultiplier;
        return (int) Math.ceil(memoryClassMb);
    }

    private static class MemorySizeHarness {
        int pixelSize = 500;
        int bytesPerPixel = MemorySizeCalculator.BYTES_PER_ARGB_8888_PIXEL;
        int memoryCacheScreens = MemorySizeCalculator.MEMORY_CACHE_TARGET_SCREENS;
        int bitmapPoolScreens = MemorySizeCalculator.BITMAP_POOL_TARGET_SCREENS;
        float sizeMultiplier = MemorySizeCalculator.MAX_SIZE_MULTIPLIER;
        ActivityManager activityManager =
                (ActivityManager) Robolectric.application.getSystemService(Context.ACTIVITY_SERVICE);
        MemorySizeCalculator.ScreenDimensions screenDimensions = mock(MemorySizeCalculator.ScreenDimensions.class);

        public MemorySizeCalculator getCalculator() {
            when(screenDimensions.getWidthPixels()).thenReturn(pixelSize);
            when(screenDimensions.getHeightPixels()).thenReturn(pixelSize);
            return new MemorySizeCalculator(Robolectric.application, activityManager, screenDimensions);
        }

        public int getScreenSize() {
            return pixelSize * pixelSize * bytesPerPixel;
        }
    }
}
