package com.bumptech.glide.util;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.SystemClock;

public class LogTime {
    private static final double MILLIS_MULTIPLIER = Build.VERSION.SDK_INT >= 17 ? (1d / Math.pow(10, 6)) : 1d;

    @TargetApi(17)
    public static long getLogTime() {
        if (Build.VERSION.SDK_INT >= 17) {
            return SystemClock.elapsedRealtimeNanos();
        } else {
            return SystemClock.currentThreadTimeMillis();
        }
    }

    public static double getElapsedMillis(long logTime) {
        return (getLogTime() - logTime) * MILLIS_MULTIPLIER;
    }
}
