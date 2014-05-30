package com.bumptech.glide.load.engine.cache;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

public class MemorySizeCalculator {
    private static final String TAG = "MemorySizeCalculator";

    static final int BYTES_PER_ARGB_8888_PIXEL = 4;
    static final int MEMORY_CACHE_TARGET_SCREENS = 2;
    static final int BITMAP_POOL_TARGET_SCREENS = 3;

    static final float MAX_SIZE_MULTIPLIER = 0.4f;
    static final float LOW_MEMORY_MAX_SIZE_MULTIPLIER = 0.33f;
    private final int bitmapPoolSize;
    private final int memoryCacheSize;

    interface ScreenDimensions {
        public int getWidthPixels();
        public int getHeightPixels();
    }

    public MemorySizeCalculator(Context context) {
        this((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE),
                new DisplayMetricsScreenDimensions(context.getResources().getDisplayMetrics()));
    }

    MemorySizeCalculator(ActivityManager activityManager, ScreenDimensions screenDimensions) {
        final int maxSize = getMaxSize(activityManager);

        final int screenSize = screenDimensions.getWidthPixels() * screenDimensions.getHeightPixels()
                * BYTES_PER_ARGB_8888_PIXEL;

        int targetPoolSize = screenSize * BITMAP_POOL_TARGET_SCREENS;
        int targetMemoryCacheSize = screenSize * MEMORY_CACHE_TARGET_SCREENS;

        if (targetMemoryCacheSize + targetPoolSize <= maxSize) {
            memoryCacheSize = targetMemoryCacheSize;
            bitmapPoolSize = targetPoolSize;
        } else {
            int part = Math.round((float) maxSize / (BITMAP_POOL_TARGET_SCREENS + MEMORY_CACHE_TARGET_SCREENS));
            memoryCacheSize = part * MEMORY_CACHE_TARGET_SCREENS;
            bitmapPoolSize = part * BITMAP_POOL_TARGET_SCREENS;
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Calculated memory cache size: " + toMb(memoryCacheSize) + " pool size: " + toMb(bitmapPoolSize)
                    + " memory class limited? " + (targetMemoryCacheSize + targetPoolSize > maxSize) + " max size: "
                    + toMb(maxSize) + " memoryClass: " + activityManager.getMemoryClass() + " isLowMemoryDevice: "
                    + isLowMemoryDevice(activityManager));
        }
    }

    public int getMemoryCacheSize() {
        return memoryCacheSize;
    }

    public int getBitmapPoolSize() {
        return bitmapPoolSize;
    }

    private static int getMaxSize(ActivityManager activityManager) {
        final int memoryClassBytes = activityManager.getMemoryClass() * 1024 * 1024;
        final boolean isLowMemoryDevice = isLowMemoryDevice(activityManager);
        return Math.round(memoryClassBytes *
                (isLowMemoryDevice ? LOW_MEMORY_MAX_SIZE_MULTIPLIER : MAX_SIZE_MULTIPLIER));
    }

    private static int toMb(int bytes) {
        return bytes / (1024 * 1024);
    }

    @TargetApi(19)
    private static boolean isLowMemoryDevice(ActivityManager activityManager) {
        final int sdkInt = Build.VERSION.SDK_INT;
        return sdkInt < 11 || (sdkInt >= 19 && activityManager.isLowRamDevice());
    }

    private static class DisplayMetricsScreenDimensions implements ScreenDimensions {
        private DisplayMetrics displayMetrics;

        public DisplayMetricsScreenDimensions(DisplayMetrics displayMetrics) {
            this.displayMetrics = displayMetrics;
        }

        @Override
        public int getWidthPixels() {
            return displayMetrics.widthPixels;
        }

        @Override
        public int getHeightPixels() {
            return displayMetrics.heightPixels;
        }
    }
}
