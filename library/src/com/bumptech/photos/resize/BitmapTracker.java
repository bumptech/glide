package com.bumptech.photos.resize;

import android.graphics.Bitmap;
import com.bumptech.photos.resize.cache.SizedBitmapCache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 6/4/13
 * Time: 9:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class BitmapTracker {
    private final SizedBitmapCache target;
    private final BitmapReferenceCounter counter = new BitmapReferenceCounter();
    private final Set<Integer> pending = new HashSet<Integer>();

    public BitmapTracker(SizedBitmapCache target) {
        this.target = target;
    }

    public synchronized void acquireBitmap(Bitmap bitmap) {
        final int hashCode = bitmap.hashCode();
        pending.remove(hashCode);
        counter.inc(hashCode);
    }

    public synchronized void releaseBitmap(Bitmap bitmap) {
        final int hashCode = bitmap.hashCode();
        if (counter.dec(hashCode) == 0 && !pending.contains(hashCode)) {
            counter.rem(hashCode);
            target.put(bitmap);
        }
    }

    public synchronized void rejectBitmap(Bitmap bitmap) {
        final int hashCode = bitmap.hashCode();
        pending.remove(hashCode);
        if (counter.get(hashCode) == 0) {
            counter.rem(hashCode);
            target.put(bitmap);
        }
    }

    public synchronized void markPending(Bitmap bitmap) {
        final int hashCode = bitmap.hashCode();
        pending.add(hashCode);
    }

    private class BitmapReferenceCounter {
        private final Map<Integer, Integer> counter = new HashMap<Integer, Integer>();

        public void inc(int hashCode) {
            Integer currentCount = counter.get(hashCode);
            if (currentCount == null) {
                currentCount = 0;
            }
            counter.put(hashCode, currentCount + 1);
        }

        public int dec(int hashCode) {
            Integer currentCount = counter.get(hashCode);
            if (currentCount == null) {
                throw new IllegalArgumentException("Can't decrement null count bitmap=" + hashCode);
            }

            currentCount--;

            counter.put(hashCode, currentCount);

            return currentCount;
        }

        public int get(int hashCode) {
            Integer currentCount = counter.get(hashCode);

            if (currentCount == null) {
                currentCount = 0;
            }

            return currentCount;
        }

        public void rem(int hashCode) {
            counter.remove(hashCode);
        }

    }
}
