package com.bumptech.glide.load.engine;

import android.os.Looper;
import android.os.MessageQueue;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.EngineResource.ResourceListener;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

final class ActiveResources {
  @VisibleForTesting
  final Map<Key, ResourceWeakReference> activeEngineResources = new HashMap<>();
  // Lazily instantiate to avoid exceptions if Glide is initialized on a background thread. See
  // #295.
  @Nullable
  private ReferenceQueue<EngineResource<?>> resourceReferenceQueue;
  private ResourceListener listener;

  void setListener(ResourceListener listener) {
    this.listener = listener;
  }

  void activate(Key key, EngineResource<?> resource) {
    ResourceWeakReference removed =
        activeEngineResources.put(
            key, new ResourceWeakReference(key, resource, getReferenceQueue()));
    if (removed != null) {
      removed.reset();
    }
  }

  void deactivate(Key key) {
    ResourceWeakReference removed = activeEngineResources.remove(key);
    if (removed != null) {
      removed.reset();
    }
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

    if (!ref.isCacheable || ref.resource == null) {
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

  @VisibleForTesting
  static final class ResourceWeakReference extends WeakReference<EngineResource<?>> {
    @SuppressWarnings("WeakerAccess") @Synthetic final Key key;
    @SuppressWarnings("WeakerAccess") @Synthetic final boolean isCacheable;

    @Nullable @SuppressWarnings("WeakerAccess") @Synthetic Resource<?> resource;

    @Synthetic
    @SuppressWarnings("WeakerAccess")
    ResourceWeakReference(
        @NonNull Key key,
        @NonNull EngineResource<?> referent,
        @NonNull ReferenceQueue<? super EngineResource<?>> queue) {
      super(referent, queue);
      this.key = Preconditions.checkNotNull(key);
      this.resource =
          referent.isCacheable() ? Preconditions.checkNotNull(referent.getResource()) : null;
      isCacheable = referent.isCacheable();
    }

    void reset() {
      resource = null;
      clear();
    }
  }
}
