package com.bumptech.glide.resize.bitmap_recycle;

import static android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class LruBitmapPool implements BitmapPool {
    private static final String TAG = "LruBitmapPool";
    private final GroupedBitmapLinkedMap pool = new GroupedBitmapLinkedMap();

    private final int maxSize;
    private int currentSize = 0;

    public LruBitmapPool(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public synchronized boolean put(Bitmap bitmap) {
        // BitmapFactory.decodeStream can sometimes return bitmaps with null configs, which can't generally be reused
        if (bitmap.getConfig() == null)
            return false;
        
        final int size = getSize(bitmap);

        pool.put(bitmap);

        currentSize += size;
        evict();

        return true;
    }

    private void evict() {
        trimToSize(maxSize);
    }

    @Override
    public synchronized Bitmap get(int width, int height, Bitmap.Config config) {
        final Bitmap result = pool.get(width, height, config);
        if (result == null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Missing bitmap with dimens=[" + width + "x" + height + "] and config config=" + config);
            }
        } else {
            currentSize -= getSize(result);
        }

        return result;
    }

    @Override
    public void clearMemory() {
        trimToSize(0);
    }

    @Override
    public void trimMemory(int level) {
        if (level >= TRIM_MEMORY_MODERATE) {
            clearMemory();
        } else if (level >= TRIM_MEMORY_BACKGROUND) {
            trimToSize(currentSize / 2);
        }
    }

    private void trimToSize(int size) {
        while (currentSize > size) {
            final Bitmap removed = pool.removeLast();
            currentSize -= getSize(removed);
            removed.recycle();
        }
    }

    private static int getSize(Bitmap bitmap) {
        return bitmap.getHeight() * bitmap.getRowBytes();
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
        private final Map<Key, LinkedEntry> keyToEntry = new HashMap<Key, LinkedEntry>();
        private final LinkedEntry head = new LinkedEntry();
        private final KeyPool keyPool = new KeyPool();

        private static class KeyPool {
            private static final int MAX_SIZE = 20;

            private final Queue<Key> keyPool = new LinkedList<Key>();

            public Key get(int width, int height, Bitmap.Config config) {
                Key result = keyPool.poll();
                if (result == null) {
                    result = new Key();
                }
                result.init(width, height, config);
                return result;
            }

            public void offer(Key key) {
                if (keyPool.size() <= MAX_SIZE) {
                    keyPool.offer(key);
                }
            }
        }

        private static class Key {
            private int width;
            private int height;
            private Bitmap.Config config; //this can be null :(

            public void init(int width, int height, Bitmap.Config config) {
                this.width = width;
                this.height = height;
                this.config = config;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Key key = (Key) o;

                if (height != key.height) return false;
                if (width != key.width) return false;
                if (config != key.config) return false;

                return true;
            }

            @Override
            public int hashCode() {
                int result = width;
                result = 31 * result + height;
                result = 31 * result + (config != null ? config.hashCode() : 0);
                return result;
            }
        }

        public void put(Bitmap bitmap) {
            // BitmapFactory.decodeStream can sometimes return bitmaps with null configs, which can't generally be reused
            if (bitmap.getConfig() == null)
                return;
            
            final Key key = keyPool.get(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());

            LinkedEntry entry = keyToEntry.get(key);
            if (entry == null) {
                entry = new LinkedEntry(key);
                makeTail(entry);
                keyToEntry.put(key, entry);
            } else {
                keyPool.offer(key);
            }

            entry.add(bitmap);
        }

        public Bitmap get(int width, int height, Bitmap.Config config) {
            final Key key = keyPool.get(width, height, config);

            LinkedEntry entry = keyToEntry.get(key);
            if (entry == null) {
                entry = new LinkedEntry(key);
                keyToEntry.put(key, entry);
            } else {
                keyPool.offer(key);
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
                    keyPool.offer(last.key);
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
            private final Key key;
            LinkedEntry next;
            LinkedEntry prev;

            //head only
            public LinkedEntry() {
                this(null);
            }

            public LinkedEntry(Key key) {
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
