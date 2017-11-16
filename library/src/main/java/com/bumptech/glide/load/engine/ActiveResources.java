package com.bumptech.glide.load.engine;

import android.os.Looper;
import android.os.MessageQueue;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.EngineResource.ResourceListener;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

final class ActiveResources {
  private final Map<Key, ResourceWeakReference> activeEngineResources = new HashMap<>();
  // Lazily instantiate to avoid exceptions if Glide is initialized on a background thread. See
  // #295.
  @Nullable
  private ReferenceQueue<EngineResource<?>> resourceReferenceQueue;
  private ResourceListener listener;

  void setListener(ResourceListener listener) {
    this.listener = listener;
  }

  void activate(Key key, EngineResource<?> resource) {
    activeEngineResources.put(key, new ResourceWeakReference(key, resource, getReferenceQueue()));
  }

  void deactivate(Key key) {
    activeEngineResources.remove(key);
  }

  @Nullable
  EngineResource<?> get(Key key) {
    ResourceWeakReference activeRef = activeEngineResources.get(key);
    if (activeRef == null) {
      return null;
    }

    EngineResource<?> active = activeRef.get();
    if (active == null) {
      cleanupActiveReference(activeRef);
    }
    return active;
  }

  private void cleanupActiveReference(@NonNull ResourceWeakReference ref) {
    activeEngineResources.remove(ref.key);

    if (!ref.isCacheable) {
      return;
    }
    EngineResource<?> newResource =
        new EngineResource<>(ref.resource, /*isCacheable=*/ true, /*isRecyclable=*/ false);
    newResource.setResourceListener(ref.key, listener);
    listener.onResourceReleased(ref.key, newResource);
  }

  private ReferenceQueue<EngineResource<?>> getReferenceQueue() {
    if (resourceReferenceQueue == null) {
      resourceReferenceQueue = new ReferenceQueue<>();
      MessageQueue queue = Looper.myQueue();
      queue.addIdleHandler(new RefQueueIdleHandler());
    }
    return resourceReferenceQueue;
  }

  // Responsible for cleaning up the active resource map by remove weak references that have been
  // cleared.
  private class RefQueueIdleHandler implements MessageQueue.IdleHandler {
    @Override
    public boolean queueIdle() {
      ResourceWeakReference ref;
      while ((ref = (ResourceWeakReference) getReferenceQueue().poll()) != null) {
        cleanupActiveReference(ref);
      }
      return true;
    }
  }

  private static class ResourceWeakReference extends WeakReference<EngineResource<?>> {
    @Synthetic final Key key;
    @Synthetic final Resource<?> resource;
    @Synthetic final boolean isCacheable;

    ResourceWeakReference(
        Key key, EngineResource<?> r, ReferenceQueue<? super EngineResource<?>> q) {
      super(r, q);
      this.key = Preconditions.checkNotNull(key);
      this.resource = Preconditions.checkNotNull(r.getResource());
      isCacheable = r.isCacheable();
    }
  }
}
