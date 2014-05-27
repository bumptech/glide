package com.bumptech.glide.resize;

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

public class DefaultResourceReferenceCounter implements ResourceReferenceCounter {
    private static class InnerTrackerPool {
        private ArrayList<InnerTracker> pool = new ArrayList<InnerTracker>();

        public InnerTracker get() {
            InnerTracker result;
            if (pool.size() == 0) {
                result = new InnerTracker();
            } else {
                result = pool.remove(pool.size() - 1);
            }

            return result;
        }

        public void release(InnerTracker innerTracker) {
            pool.add(innerTracker);
        }
    }

    private static class InnerTracker {
        private int refs = 0;

        public void acquire() {
            refs++;
        }

        public boolean release() {
            refs--;

            return refs == 0;
        }
    }

    private final Map<Resource, InnerTracker> counter = new WeakHashMap<Resource, InnerTracker>();
    private final InnerTrackerPool pool = new InnerTrackerPool();

    private void initResource(Resource resource) {
        final InnerTracker tracker = counter.get(resource);
        if (tracker == null) {
            counter.put(resource, pool.get());
        }
    }

    @Override
    public void acquireResource(Resource resource) {
        initResource(resource);
        counter.get(resource).acquire();
    }

    @Override
    public void releaseResource(Resource resource) {
        final InnerTracker tracker = counter.get(resource);
        if (tracker.release()) {
            resource.recycle();
        }
    }
}
