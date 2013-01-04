/*
 * Copyright (c) 2013. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.resize;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;

import java.util.concurrent.Executor;

/**
 * A simple Executor implemented using an Android {@link android.os.Handler}
 */
public class HandlerExecutor implements Executor {
    private final Handler worker;
    private final Object token;

    /**
     * Create a new executor with a new HandlerThread called "HandlerExecutor"
     */
    public HandlerExecutor() {
        HandlerThread workerThread = new HandlerThread("HandlerExecutor");
        workerThread.start();
        this.worker = new Handler(workerThread.getLooper());
        token = hashCode();
    }

    /**
     * Create a new executor that uses the given Handler's thread
     *
     * @param bgHandler The handler to post Runnables to
     */
    public HandlerExecutor(Handler bgHandler) {
        this.worker = bgHandler;
        token = hashCode();
    }

    @Override
    public void execute(Runnable runnable) {
        worker.postAtTime(runnable,  token, SystemClock.uptimeMillis());
    }
}
