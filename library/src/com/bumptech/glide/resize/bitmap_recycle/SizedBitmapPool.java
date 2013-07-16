package com.bumptech.glide.resize.bitmap_recycle;

import android.graphics.Bitmap;
import com.bumptech.glide.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 7/10/13
 * Time: 1:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class SizedBitmapPool implements BitmapPool {
    private final Map<Integer, ArrayList<Bitmap>> pool = new HashMap<Integer, ArrayList<Bitmap>>();
    //Typically there should only be a handful of keys (often 1 or 2) so
    //iterating over this list should be fast
    private final LinkedList<Integer> keys = new LinkedList<Integer>();

    private final int maxSize;
    private int currentSize = 0;

    public SizedBitmapPool(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public synchronized boolean put(Bitmap bitmap) {
        final int key = getKey(bitmap);
        final int size = getSize(bitmap);

        ArrayList<Bitmap> bitmaps = pool.get(key);
        if (bitmaps == null) {
            bitmaps = new ArrayList<Bitmap>();
            pool.put(key, bitmaps);
        } else {
            removeKey(key);
        }
        keys.addFirst(key);
        currentSize += size;

        bitmaps.add(bitmap);

        maybeEvict();

        return true;
    }

    private void maybeEvict() {
        while (currentSize > maxSize) {
            final Integer key = keys.getLast();
            List<Bitmap> toRemove = pool.get(key);
            while (toRemove.size() > 0 && currentSize > maxSize) {
                Bitmap removed = toRemove.remove(toRemove.size()-1);
                currentSize -= getSize(removed);
            }
            if (toRemove.size() == 0) {
                removeKey(key);
            }
        }
    }

    @Override
    public synchronized Bitmap get(int width, int height) {
        final int key = getKey(width, height);
        final ArrayList<Bitmap> list = pool.get(key);
        final Bitmap result;
        if (list != null && list.size() > 0) {
            result = list.remove(list.size()-1); //most efficient to remove from the end of an ArrayList
            currentSize -= getSize(result);
            removeKey(key);
            if (list.size() > 0) {
                keys.addFirst(key);
            }
        } else {
            Log.d("SBP: missing bitmap for width=" + width + " height=" + height);
            result = null;
        }
        return result;
    }

    //keys.remove(int) -> remove index int, not object int :(
    private void removeKey(int key) {
        keys.remove(new Integer(key));
    }

    private int getKey(Bitmap bitmap) {
        return getKey(bitmap.getWidth(), bitmap.getHeight());
    }

    private int getKey(int width, int height) {
        return width >= height ? width * width + width + height : width + height * height;
   }

    private int getSize(Bitmap bitmap) {
        return bitmap.getHeight() * bitmap.getRowBytes();
    }
}
