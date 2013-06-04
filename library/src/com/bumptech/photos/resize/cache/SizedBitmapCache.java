/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.resize.cache;

import android.graphics.Bitmap;
import com.bumptech.photos.util.Log;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * A cache of Bitmaps made available by size used to manage recycled bitmaps
 */
public class SizedBitmapCache {
    private static final int DEFAULT_MAX_PER_SIZE = 20;
    private Map<String, Queue<Bitmap>> availableBitmaps = new HashMap<String, Queue<Bitmap>>();
    private final int maxPerSize;

    public SizedBitmapCache(int maxPerSize) {
        this.maxPerSize = maxPerSize == 0 ? DEFAULT_MAX_PER_SIZE : maxPerSize;
    }

    public synchronized void put(Bitmap bitmap) {
        final String sizeKey = getSizeKey(bitmap.getWidth(), bitmap.getHeight());
        Queue<Bitmap> available = availableBitmaps.get(sizeKey);
        if (available == null) {
            available = new ArrayDeque<Bitmap>();
            availableBitmaps.put(sizeKey, available);
        }

        if (available.size() < maxPerSize) {
            available.offer(bitmap);
        }
    }

    public synchronized Bitmap get(int width, int height) {
        final String sizeKey = getSizeKey(width, height);
        final Queue<Bitmap> available = availableBitmaps.get(sizeKey);

        if (available == null) {
            Log.d("SBC: missing bitmap for key= " + sizeKey);
            return null;
        } else {
            //Log.d("SBC:  get key=" + sizeKey + " available=" + (available.size() - 1));
            return available.poll();
        }
    }

    private static final String getSizeKey(int width, int height) {
        return width + "_" + height;
    }
}
