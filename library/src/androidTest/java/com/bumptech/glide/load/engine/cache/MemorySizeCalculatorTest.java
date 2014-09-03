package com.bumptech.glide.load.engine.cache;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.matchers.LessOrEqual;
import org.mockito.internal.matchers.LessThan;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
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
        setSdkVersionInt(initialSdkVersion);
    }

    private void setSdkVersionInt(int version) {
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK_INT", version);
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

        assertThat(memoryCacheSize, new LessOrEqual<Integer>(Math.round(memoryClassBytes * harness.sizeMultiplier)));
    }

    @Test
    public void testDefaultBitmapPoolSizeIsThreeTimesScreenSize() {
        Robolectric.shadowOf(harness.activityManager).setMemoryClass(getLargeEnoughMemoryClass());

        int bitmapPoolSize = harness.getCalculator().getBitmapPoolSize();

        assertEquals(harness.getScreenSize() * harness.bitmapPoolScreens, bitmapPoolSize);
    }

    @Test
    public void testDefaultBitmapPoolSizeIsLimitedByMemoryClass() {
        final int memoryClassBytes = Math.round(harness.getScreenSize() * harness.bitmapPoolScreens
                * harness.sizeMultiplier);

        Robolectric.shadowOf(harness.activityManager).setMemoryClass(memoryClassBytes / (1024 * 1024));

        int bitmapPoolSize = harness.getCalculator().getBitmapPoolSize();

        assertThat(bitmapPoolSize, new LessOrEqual<Integer>(Math.round(memoryClassBytes * harness.sizeMultiplier)));
    }

    @Test
    public void testCumulativePoolAndMemoryCacheSizeAreLimitedByMemoryClass() {
        final int memoryClassBytes = Math.round(harness.getScreenSize()
                * (harness.bitmapPoolScreens + harness.memoryCacheScreens) * harness.sizeMultiplier);
        Robolectric.shadowOf(harness.activityManager).setMemoryClass(memoryClassBytes / (1024 * 1024));

        int memoryCacheSize = harness.getCalculator().getMemoryCacheSize();
        int bitmapPoolSize = harness.getCalculator().getBitmapPoolSize();

        String failHelpMessage =
                  "memoryCacheSize: " + memoryCacheSize
                + " bitmapPoolSize: " + bitmapPoolSize
                + " memoryClass: "    + memoryCacheSize
                + " sizeMultiplier: " + harness.sizeMultiplier;
        assertThat(failHelpMessage, memoryCacheSize + bitmapPoolSize,
                new LessOrEqual<Integer>(Math.round(memoryClassBytes * harness.sizeMultiplier)));
    }

    @Test
    public void testCumulativePoolAndMemoryCacheSizesAreSmallerOnLowMemoryDevices() {
        Robolectric.shadowOf(harness.activityManager).setMemoryClass(getLargeEnoughMemoryClass() / 2);
        final int normalMemoryCacheSize = harness.getCalculator().getMemoryCacheSize();
        final int normalBitmapPoolSize = harness.getCalculator().getBitmapPoolSize();

        setSdkVersionInt(10);

        final int smallMemoryCacheSize = harness.getCalculator().getMemoryCacheSize();
        final int smallBitmapPoolSize = harness.getCalculator().getBitmapPoolSize();

        assertThat(smallMemoryCacheSize, new LessThan<Integer>(normalMemoryCacheSize));
        assertThat(smallBitmapPoolSize, new LessThan<Integer>(normalBitmapPoolSize));
    }

    private int getLargeEnoughMemoryClass() {
        // Memory class is in mb, not bytes!
        return Math.round(harness.getScreenSize() * (harness.bitmapPoolScreens + harness.memoryCacheScreens)
                * (1f / harness.sizeMultiplier) / (1024 * 1024));
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
            return new MemorySizeCalculator(activityManager, screenDimensions);
        }

        public int getScreenSize() {
            return pixelSize * pixelSize * bytesPerPixel;
        }
    }
}
