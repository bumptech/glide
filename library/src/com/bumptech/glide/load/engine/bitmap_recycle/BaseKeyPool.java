package com.bumptech.glide.load.engine.bitmap_recycle;

import android.os.Build;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;

abstract class BaseKeyPool<T extends Poolable> {
    private static final int MAX_SIZE = 20;
    private final Queue<T> keyPool;

    public BaseKeyPool() {
        if (Build.VERSION.SDK_INT >= 9) {
            keyPool = new ArrayDeque<T>(MAX_SIZE);
        } else {
            keyPool = new LinkedList<T>();
        }
    }

    protected T get() {
        T result = keyPool.poll();
        if (result == null) {
            result = create();
        }
        return result;
    }

    public void offer(T key) {
        if (keyPool.size() < MAX_SIZE) {
            keyPool.offer(key);
        }
    }

    protected abstract T create();
}
