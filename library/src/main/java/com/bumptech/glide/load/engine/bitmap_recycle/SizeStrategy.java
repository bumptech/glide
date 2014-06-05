package com.bumptech.glide.load.engine.bitmap_recycle;

import android.annotation.TargetApi;
import android.graphics.Bitmap;

import java.util.TreeMap;

/**
 * A strategy for reusing bitmaps that relies on {@link Bitmap#reconfigure(int, int, Bitmap.Config)}. Requires KitKat
 * (API 19) or higher.
 */
@TargetApi(19)
class SizeStrategy implements LruPoolStrategy {
    private static final int MAX_SIZE_MULTIPLE = 4;
    private final KeyPool keyPool = new KeyPool();
    private final GroupedLinkedMap<Key, Bitmap> groupedMap = new GroupedLinkedMap<Key, Bitmap>();
    private final TreeMap<Integer, Integer> sortedSizes = new TreeMap<Integer, Integer>();

    @Override
    public void put(Bitmap bitmap) {
        final Key key = keyPool.get(bitmap.getAllocationByteCount());

        groupedMap.put(key, bitmap);

        Integer current = sortedSizes.get(key.size);
        sortedSizes.put(key.size, current == null ? 1 : current + 1);
    }

    @Override
    public Bitmap get(int width, int height, Bitmap.Config config) {
        final int size = getSize(width, height, config);
        Key key = keyPool.get(size);

        Integer possibleSize = sortedSizes.ceilingKey(size);
        if (possibleSize != null && possibleSize != size && possibleSize <= size * MAX_SIZE_MULTIPLE) {
            keyPool.offer(key);
            key = keyPool.get(possibleSize);
        }

        // Do a get even if we know we don't have a bitmap so that the key moves to the front in the lru pool
        final Bitmap result = groupedMap.get(key);
        if (result != null) {
            result.reconfigure(width, height, config);
            decrementBitmapOfSize(possibleSize);
        }

        return result;
    }

    @Override
    public Bitmap removeLast() {
        Bitmap removed = groupedMap.removeLast();
        if (removed != null) {
            final int removedSize = removed.getAllocationByteCount();
            decrementBitmapOfSize(removedSize);
        }
        return removed;
    }

    private void decrementBitmapOfSize(Integer size) {
        Integer current = sortedSizes.get(size);
        if (current == 1) {
            sortedSizes.remove(size);
        } else {
            sortedSizes.put(size, current - 1);
        }
    }

    @Override
    public String logBitmap(Bitmap bitmap) {
        return getBitmapString(bitmap);
    }

    @Override
    public String logBitmap(int width, int height, Bitmap.Config config) {
        return getBitmapString(getSize(width, height, config));
    }

    @Override
    public int getSize(Bitmap bitmap) {
        return bitmap.getAllocationByteCount();
    }

    @Override
    public String toString() {
        String result = "SizeStrategy:\n  " + groupedMap + "\n  SortedSizes( ";
        boolean hadAtLeastOneKey = false;
        for (Integer size : sortedSizes.keySet()) {
            hadAtLeastOneKey = true;
            result += "{" + getBitmapString(size) + ":" + sortedSizes.get(size) + "}, ";
        }
        if (hadAtLeastOneKey) {
            result = result.substring(0, result.length() - 2);
        }
        return result + " )";
    }

    private static String getBitmapString(Bitmap bitmap) {
        return getBitmapString(bitmap.getAllocationByteCount());
    }

    private static String getBitmapString(int size) {
        return "[" + size + "]";
    }

    private static int getSize(int width, int height, Bitmap.Config config) {
        return width * height * getBytesPerPixel(config);
    }

    private static int getBytesPerPixel(Bitmap.Config config) {
        switch (config) {
            case ARGB_8888:
                return 4;
            case RGB_565:
                return 2;
            case ARGB_4444:
                return 2;
            case ALPHA_8:
                return 1;
            default:
                // We only use this to calculate sizes to get, so choosing 4 bytes per pixel is conservative and
                // probably forces us to get a larger bitmap than we really need. Since we can't tell for sure, probably
                // better safe than sorry.
                return 4;
        }
    }

    private static class KeyPool extends BaseKeyPool<Key> {

        public Key get(int size) {
            Key result = get();
            result.init(size);
            return result;
        }

        @Override
        protected Key create() {
            return new Key(this);
        }
    }

    private static class Key implements Poolable {
        private final KeyPool pool;
        private int size;

        private Key(KeyPool pool) {
            this.pool = pool;
        }

        public void init(int size) {
            this.size = size;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            return size == key.size;
        }

        @Override
        public int hashCode() {
            return size;
        }

        @Override
        public String toString() {
            return getBitmapString(size);
        }

        @Override
        public void offer() {
            pool.offer(this);
        }
    }
}
