/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.cache;

import android.graphics.Bitmap;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/25/12
 * Time: 4:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class SizedBitmapCache {
    private static final int MAX_PER_SIZE = 10;
    private Map<String, Queue<Bitmap>> availableBitmaps = new HashMap<String, Queue<Bitmap>>();

    public void put(Bitmap bitmap) {
        final String sizeKey = getSizeKey(bitmap.getWidth(), bitmap.getHeight());
        Queue<Bitmap> available = availableBitmaps.get(sizeKey);
        if (available == null) {
            available = new LinkedList<Bitmap>();
            availableBitmaps.put(sizeKey, available);
        }

        if (available.size() < MAX_PER_SIZE) {
            available.add(bitmap);
        }
    }

    public Bitmap get(int width, int height) {
        final String sizeKey = getSizeKey(width, height);
        Queue<Bitmap> available = availableBitmaps.get(sizeKey);
        if (available == null || available.size() == 0) {
            return null;
        } else {
            return available.remove();
        }
    }

    private static final String getSizeKey(int width, int height) {
        return width + "_" + height;
    }
}
