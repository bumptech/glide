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
public class BitmapPool {
    private static final int DEFAULT_MAX_PER_SIZE = 20;
    private Map<Integer, Queue<Bitmap>> availableBitmaps = new HashMap<Integer, Queue<Bitmap>>();
    private final int maxPerSize;

    public BitmapPool(int maxPerSize) {
        this.maxPerSize = maxPerSize == 0 ? DEFAULT_MAX_PER_SIZE : maxPerSize;
    }

    public synchronized void put(Bitmap bitmap) {
        final int sizeKey = getSizeKey(bitmap.getWidth(), bitmap.getHeight());
        Queue<Bitmap> available = availableBitmaps.get(sizeKey);
        if (available == null) {
            available = new ArrayDeque<Bitmap>();
            availableBitmaps.put(sizeKey, available);
            available.offer(bitmap);
        } else if (available.size() < maxPerSize) {
            available.offer(bitmap);
        }
    }

    public synchronized Bitmap get(int width, int height) {
        final int sizeKey = getSizeKey(width, height);
        final Queue<Bitmap> available = availableBitmaps.get(sizeKey);

        if (available == null) {
            Log.d("SBC: missing bitmap for key= " + sizeKey);
            return null;
        } else {
            //Log.d("SBC:  get key=" + sizeKey + " available=" + (available.size() - 1));
            return available.poll();
        }
    }

    //see http://szudzik.com/ElegantPairing.pdf
    //assumes width <= Short.MAX_VALUE && height <= SHORT.MAX_VALUE && width >= 0 && height >= 0
    private static int getSizeKey(int width, int height) {
        return width >= height ? width * width + width + height : width + height * height;
    }
}
