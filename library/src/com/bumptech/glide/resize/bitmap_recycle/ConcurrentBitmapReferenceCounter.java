package com.bumptech.glide.resize.bitmap_recycle;

import android.graphics.Bitmap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConcurrentBitmapReferenceCounter implements BitmapReferenceCounter {

    private static class InnerTrackerPool {
        private ConcurrentLinkedQueue<InnerTracker> pool = new ConcurrentLinkedQueue<InnerTracker>();

        public InnerTracker get() {
            InnerTracker result = pool.poll();
            if (result == null) {
                result = new InnerTracker();
            }

            return result;
        }

        public void release(InnerTracker innerTracker) {
            pool.offer(innerTracker);
        }
    }

    private static class InnerTracker {
        private volatile int refs = 0;
        private volatile boolean pending = false;

        public void acquire() {
            pending = false;
            synchronized (this) {
                refs++;
            }
        }

        public boolean release() {
            synchronized (this) {
                refs--;
            }

            return refs == 0 && !pending;
        }

        public boolean reject() {
            pending = false;
            return refs == 0;
        }

        public void markPending() {
            pending = true;
        }
    }

    private final ConcurrentHashMap<Integer, InnerTracker> counter;
    private final BitmapPool target;
    private final InnerTrackerPool pool = new InnerTrackerPool();

    public ConcurrentBitmapReferenceCounter(BitmapPool target) {
        this.target = target;
        counter = new ConcurrentHashMap<Integer, InnerTracker>();
    }

    @Override
    public void initBitmap(Bitmap toInit) {
        final InnerTracker ifAbsent = pool.get();
        final InnerTracker old = counter.putIfAbsent(toInit.hashCode(), ifAbsent);
        if (old != null) {
            pool.release(ifAbsent);
        }
    }

    @Override
    public void acquireBitmap(Bitmap bitmap) {
        get(bitmap).acquire();
    }

    @Override
    public void releaseBitmap(Bitmap bitmap) {
        final InnerTracker tracker = get(bitmap);
        if (tracker.release()) {
            recycle(tracker, bitmap);
        }
    }

    @Override
    public void rejectBitmap(Bitmap bitmap) {
        final InnerTracker tracker = get(bitmap);
        if (tracker.reject()) {
            recycle(tracker, bitmap);
        }
    }

    @Override
    public void markPending(Bitmap bitmap) {
        get(bitmap).markPending();
    }

    private InnerTracker get(Bitmap bitmap) {
        return counter.get(bitmap.hashCode());
    }

    private void recycle(InnerTracker tracker, Bitmap bitmap) {
        if (!target.put(bitmap)) {
            counter.remove(bitmap.hashCode());
            pool.release(tracker);
        }
    }
}
