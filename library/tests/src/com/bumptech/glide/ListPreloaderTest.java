package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.test.AndroidTestCase;
import com.bumptech.glide.loader.bitmap.model.stream.StreamModelLoader;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ListPreloaderTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testGetItemsIsCalledIncreasing() {
        final AtomicBoolean called = new AtomicBoolean(false);
        ListPreloaderAdapter preloader = new ListPreloaderAdapter(getContext(), 10) {
            @Override
            protected List<Object> getItems(int start, int end) {
                called.set(true);
                assertEquals(11, start);
                assertEquals(21, end);
                return super.getItems(start, end);
            }
        };
        preloader.onScroll(null, 1, 10, 30);
        assertTrue(called.get());
    }

    public void testGetItemsIsCalledDecreasing() {
        final AtomicBoolean called = new AtomicBoolean(false);
        ListPreloaderAdapter preloader = new ListPreloaderAdapter(getContext(), 10) {
            @Override
            protected List<Object> getItems(int start, int end) {
                // Ignore the preload caused from us starting at the end
                if (start == 30) {
                    return Collections.EMPTY_LIST;
                }
                called.set(true);
                assertEquals(19, start);
                assertEquals(29, end);
                return super.getItems(start, end);
            }
        };
        preloader.onScroll(null, 30, 10, 30);
        preloader.onScroll(null, 29, 10, 30);
        assertTrue(called.get());
    }

    public void testGetItemsIsNeverCalledWithEndGreaterThanTotalItems() {
        final AtomicBoolean called = new AtomicBoolean(false);
        ListPreloaderAdapter preloader = new ListPreloaderAdapter(getContext(), 10) {
            @Override
            protected List<Object> getItems(int start, int end) {
                called.set(true);
                assertEquals(26, start);
                assertEquals(30, end);
                return super.getItems(start, end);
            }
        };
        preloader.onScroll(null, 16, 10, 30);
        assertTrue(called.get());
    }

    public void testGetItemsIsNeverCalledWithStartLessThanZero() {
        final AtomicBoolean called = new AtomicBoolean(false);
        ListPreloaderAdapter preloader = new ListPreloaderAdapter(getContext(), 10) {
            @Override
            protected List<Object> getItems(int start, int end) {
                if (start == 17) {
                    return Collections.EMPTY_LIST;
                }
                called.set(true);
                assertEquals(0, start);
                assertEquals(6, end);
                return super.getItems(start, end);
            }
        };
        preloader.onScroll(null, 7, 10, 30);
        preloader.onScroll(null, 6, 10, 30);
        assertTrue(called.get());
    }

    public void testDontPreloadItemsRepeatedlyWhileIncreasing() {
        final AtomicInteger called = new AtomicInteger();
        ListPreloaderAdapter preloader = new ListPreloaderAdapter(getContext(), 10) {
            @Override
            protected List<Object> getItems(int start, int end) {
                final int current = called.getAndIncrement();
                if (current == 0) {
                    assertEquals(11, start);
                    assertEquals(21, end);
                } else if (current == 1) {
                    assertEquals(21, start);
                    assertEquals(24, end);
                }
                return super.getItems(start, end);
            }
        };

        preloader.onScroll(null, 1, 10, 30);
        preloader.onScroll(null, 4, 10, 30);

        assertEquals(2, called.get());
    }

    public void testDontPreloadItemsRepeatedlyWhileDecreasing() {
        final AtomicInteger called = new AtomicInteger();
        ListPreloaderAdapter preloader = new ListPreloaderAdapter(getContext(), 10) {
            @Override
            protected List<Object> getItems(int start, int end) {
                if (start == 30) {
                    return Collections.EMPTY_LIST;
                }
                final int current = called.getAndIncrement();
                if (current == 0) {
                    assertEquals(10, start);
                    assertEquals(20, end);
                } else if (current == 1) {
                    assertEquals(7, start);
                    assertEquals(10, end);
                }
                return super.getItems(start, end);
            }
        };

        preloader.onScroll(null, 21, 10, 30);
        preloader.onScroll(null, 20, 10, 30);
        preloader.onScroll(null, 17, 10, 30);
        assertEquals(2, called.get());
    }

    public void testItemsArePreloadedWithGlide() {
        final List<Object> objects = new ArrayList<Object>();
        objects.add(new Object());
        objects.add(new Object());
        final HashSet<Object> loadedObjects = new HashSet<Object>();
        ListPreloaderAdapter preloader = new ListPreloaderAdapter(getContext(), 10) {
            @Override
            protected List<Object> getItems(int start, int end) {
                return objects;
            }

            @Override
            protected Glide.Request getRequest(Object item) {
                loadedObjects.add(item);
                return super.getRequest(item);
            }
        };

        preloader.onScroll(null, 1, 10, 30);

        assertEquals(objects.size(), loadedObjects.size());
        for (Object object : objects) {
            assertTrue(loadedObjects.contains(object));
        }
    }

    private static class ListPreloaderAdapter extends ListPreloader<Object> {
        public Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

        public ListPreloaderAdapter(Context context, int maxPreload) {
            super(context, maxPreload);
        }

        @Override
        protected int[] getDimensions(Object item) {
            return new int[] { 100, 100 };
        }

        @Override
        protected List<Object> getItems(int start, int end) {
            ArrayList<Object> result = new ArrayList<Object>(end - start);
            Collections.fill(result, new Object());
            return result;
        }

        @Override
        protected Glide.Request getRequest(Object item) {
            return Glide.using(new StreamModelLoader<Object>() {
                @Override
                public ResourceFetcher<InputStream> getResourceFetcher(final Object model, int width, int height) {
                    return new ResourceFetcher<InputStream>() {
                        @Override
                        public InputStream loadResource() throws Exception {
                            ByteArrayOutputStream os = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                            return new ByteArrayInputStream(os.toByteArray());
                        }

                        @Override
                        public String getId() {
                            return model.toString();
                        }

                        @Override
                        public void cancel() {
                        }
                    };
                }

                @Override
                public String getId(Object model) {
                    return String.valueOf(model.hashCode());
                }
            }).load(item);
        }
    }
}
