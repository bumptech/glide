package com.bumptech.glide.load.engine;

import android.os.Handler;
import android.os.Message;

import com.bumptech.glide.util.Util;

/**
 * A class that can safely recycle recursive resources.
 */
class ResourceRecycler {
    private boolean isRecycling;
    private final Handler handler = new Handler(new ResourceRecyclerCallback());

    public void recycle(Resource<?> resource) {
        Util.assertMainThread();

        if (isRecycling) {
            // If a resource has sub-resources, releasing a sub resource can cause it's parent to be synchronously
            // evicted which leads to a recycle loop when the parent releases it's children. Posting breaks this loop.
            handler.obtainMessage(ResourceRecyclerCallback.RECYCLE_RESOURCE, resource).sendToTarget();
        } else {
            isRecycling = true;
            resource.recycle();
            isRecycling = false;
        }
    }

    private static class ResourceRecyclerCallback implements Handler.Callback {
        public static final int RECYCLE_RESOURCE = 1;

        @Override
        public boolean handleMessage(Message message) {
            if (message.what == RECYCLE_RESOURCE) {
                Resource resource = (Resource) message.obj;
                resource.recycle();
                return true;
            }
            return false;
        }
    }
}
