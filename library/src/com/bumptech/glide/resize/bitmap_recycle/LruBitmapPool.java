package com.bumptech.glide.resize.bitmap_recycle;

import android.graphics.Bitmap;
import android.util.SparseArray;
import com.bumptech.glide.util.Log;

import java.util.ArrayList;
import java.util.List;

public class LruBitmapPool implements BitmapPool {
    private final GroupedBitmapLinkedMap pool = new GroupedBitmapLinkedMap();

    private final int maxSize;
    private int currentSize = 0;

    public LruBitmapPool(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public synchronized boolean put(Bitmap bitmap) {
        final int key = getKey(bitmap);
        final int size = getSize(bitmap);

        pool.put(key, bitmap);
        currentSize += size;
        evict();

        return true;
    }

    private void evict() {
        while (currentSize > maxSize) {
            currentSize -= getSize(pool.removeLast());
        }
    }

    @Override
    public synchronized Bitmap get(int width, int height) {
        final int sizeKey = getKey(width, height);
        final Bitmap result = pool.get(sizeKey);
        if (result == null) {
            Log.d("LBP: missing bitmap for width=" + width + " height=" + height);
        } else {
            currentSize -= getSize(result);
        }

        return result;
    }

    private static int getKey(Bitmap bitmap) {
        return getKey(bitmap.getWidth(), bitmap.getHeight());
    }

    private static int getKey(int width, int height) {
        return width >= height ? width * width + width + height : width + height * height;
   }

    private static int getSize(Bitmap bitmap) {
        return bitmap.getHeight() * bitmap.getWidth();
    }

    /**
     * Similar to {@link java.util.LinkedHashMap} when access ordered except that it is access ordered on groups
     * of bitmaps rather than individual objects. The idea is to be able to find the LRU bitmap size, rather than the
     * LRU bitmap object. We can then remove bitmaps from the least recently used size of bitmap when we need to
     * reduce our cache size.
     *
     * For the purposes of the LRU, we count gets for a particular size of bitmap as an access, even if no bitmaps
     * of that size are present. We do not count addition or removal of bitmaps as an access.
     */
    private static class GroupedBitmapLinkedMap {
        private final SparseArray<LinkedEntry> keyToEntry = new SparseArray<LinkedEntry>();
        private final LinkedEntry head = new LinkedEntry();

        public void put(int key, Bitmap bitmap) {
            LinkedEntry entry = keyToEntry.get(key);
            if (entry == null) {
                entry = new LinkedEntry(key);
                makeTail(entry);
                keyToEntry.put(key, entry);
            }
            entry.add(bitmap);
        }

        public Bitmap get(int key) {
            LinkedEntry entry = keyToEntry.get(key);
            if (entry == null) {
                entry = new LinkedEntry(key);
                keyToEntry.put(key, entry);
            }
            makeHead(entry);

            return entry.removeLast();
        }

        public Bitmap removeLast() {
            LinkedEntry last = head.prev;

            while (last != head) {
                Bitmap removed = last.removeLast();
                if (removed != null) {
                    return removed;
                } else {
                    //we will clean up empty lru entries since they are likely to have been one off or unusual sizes
                    //and are not likely to be requested again so the gc thrash should be minimal. Doing so will speed
                    //up our removeLast operation in the future and prevent our linked list from growing to arbitrarily
                    //large sizes
                    removeEntry(last);
                    keyToEntry.remove(last.key);
                }

                last = last.prev;
            }

            return null;
        }

        private void makeHead(LinkedEntry entry) {
            removeEntry(entry);
            entry.prev = head;
            entry.next = head.next;
            updateEntry(entry);
        }

        private void makeTail(LinkedEntry entry) {
            removeEntry(entry);
            entry.prev = head.prev;
            entry.next = head;
            updateEntry(entry);
        }

        //after updating entry's next and prev, set
        //those entry's prev and next (respectively) to rentry
        private static void updateEntry(LinkedEntry entry) {
            entry.next.prev = entry;
            entry.prev.next = entry;
        }

        private static void removeEntry(LinkedEntry entry) {
            entry.prev.next = entry.next;
            entry.next.prev = entry.prev;
        }

        private static class LinkedEntry {
            private List<Bitmap> value;
            private final int key;
            LinkedEntry next;
            LinkedEntry prev;

            //head only
            public LinkedEntry() {
                this(-1);
            }

            public LinkedEntry(int key) {
                next = prev = this;
                this.key = key;
            }

            public Bitmap removeLast() {
                final int valueSize = value != null ? value.size() : 0;
                return valueSize > 0 ? value.remove(valueSize-1) : null;
            }

            public void add(Bitmap bitmap) {
                if (value == null) {
                    value = new ArrayList<Bitmap>();
                }
                value.add(bitmap);
            }
        }
    }
}
