/*
 * Copyright (c) 2013. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.resize;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;

import java.util.concurrent.Executor;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/2/13
 * Time: 1:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class HandlerExecutor implements Executor {
    private final Handler worker;
    private final Object token;

    public HandlerExecutor() {
        HandlerThread workerThread = new HandlerThread("HandlerExecutor");
        workerThread.start();
        this.worker = new Handler(workerThread.getLooper());
        token = hashCode();
    }

    public HandlerExecutor(Handler bgHandler) {
        this.worker = bgHandler;
        token = hashCode();
    }

    @Override
    public void execute(Runnable runnable) {
        worker.postAtTime(runnable,  token, SystemClock.uptimeMillis());
    }
}
