package com.bumptech.glide;

import android.annotation.SuppressLint;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

protected class Log {
    protected static boolean DEBUG = false;

    private static final String TAG = "GLIDE";

    @SuppressLint("SimpleDateFormat")
    private static final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    protected static void e(String message, Object... args) {
        String formatted = args.length > 0 ? String.format(message, args) : message;
        android.util.Log.e(TAG, formatted);
    }

    protected static void e(String message, Throwable t, Object... args){
        String formatted = args.length > 0 ? String.format(message, args) : message;
        android.util.Log.e(TAG, formatted, t);
    }

    protected static void w(String message, Object... args) {
        String formatted = args.length > 0 ? String.format(message, args) : message;
        android.util.Log.w(TAG, formatted);
    }

    protected static void i(String message, Object... args) {
        if (!DEBUG) return;

        String formatted = args.length > 0 ? String.format(message, args) : message;
        android.util.Log.i(TAG, formatted);
    }

    protected static void d(String message, Object... args) {
        if (!DEBUG) return;

        String formatted = args.length > 0 ? String.format(message, args) : message;
        formatted = "[" + dateFormat.format(new Date()) + "] " + formatted;
        android.util.Log.d(TAG, formatted);
    }

    protected static void v(String message, Object... args) {
        if (!DEBUG) return;

        String formatted = args.length > 0 ? String.format(message, args) : message;
        android.util.Log.v(TAG, formatted);
    }
}
