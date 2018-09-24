package com.bumptech.glide.load.engine;

import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.EngineResource.ResourceListener;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;
import com.bumptech.glide.util.Util;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class ActiveResources {
  private static final int MSG_CLEAN_REF = 1;

  private final boolean isActiveResourceRetentionAllowed;
  private final Handler mainHandler = new Handler(Looper.getMainLooper(), new Callback() {
    @Override
    public boolean handleMessage(Message msg) {
      if (msg.what == MSG_CLEAN_REF) {
        cleanupActiveReference((ResourceWeakReference) msg.obj);
        return true;
      }
      return false;
    }
  });
  @VisibleForTesting
  final Map<Key, ResourceWeakReference> activeEngineResources = new HashMap<>();

  private ResourceListener listener;

  /**
   * Lazily instantiate to avoid exceptions if Glide is initialized on a background thread.
   *
   * @see <a href="https://github.com/bumptech/glide/issues/295">#295</a>
   */
  @Nullable
  private ReferenceQueue<EngineResource<?>> resourceReferenceQueue;
  @Nullable
  private Thread cleanReferenceQueueThread;
  private volatile boolean isShutdown;
  @Nullable
  private volatile DequeuedResourceCallback cb;

  ActiveResources(boolean isActiveResourceRetentionAllowed) {
    this.isActiveResourceRetentionAllowed = isActiveResourceRetentionAllowed;
  }

  void setListener(ResourceListener listener) {
    this.listener = listener;
  }

  void activate(Key key, EngineResource<?> resource) {
    ResourceWeakReference toPut =
        new ResourceWeakReference(
            key,
            resource,
            getReferenceQueue(),
            isActiveResourceRetentionAllowed);

    ResourceWeakReference removed = activeEngineResources.put(key, toPut);
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

  @SuppressWarnings("WeakerAccess")
  @Synthetic void cleanupActiveReference(@NonNull ResourceWeakReference ref) {
    Util.assertMainThread();
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
      cleanReferenceQueueThread = new Thread(new Runnable() {
        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
          Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
          cleanReferenceQueue();
        }
      }, "glide-active-resources");
      cleanReferenceQueueThread.start();
    }
    return resourceReferenceQueue;
  }

  @SuppressWarnings("WeakerAccess")
  @Synthetic void cleanReferenceQueue() {
    while (!isShutdown) {
      try {
        ResourceWeakReference ref = (ResourceWeakReference) resourceReferenceQueue.remove();
        mainHandler.obtainMessage(MSG_CLEAN_REF, ref).sendToTarget();

        // This section for testing only.
        DequeuedResourceCallback current = cb;
        if (current != null) {
          current.onResourceDequeued();
        }
        // End for testing only.
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @VisibleForTesting
  void setDequeuedResourceCallback(DequeuedResourceCallback cb) {
    this.cb = cb;
  }

  @VisibleForTesting
  interface DequeuedResourceCallback {
    void onResourceDequeued();
  }

  @VisibleForTesting
  void shutdown() {
    isShutdown = true;
    if (cleanReferenceQueueThread == null) {
      return;
    }

    cleanReferenceQueueThread.interrupt();
    try {
      cleanReferenceQueueThread.join(TimeUnit.SECONDS.toMillis(5));
      if (cleanReferenceQueueThread.isAlive()) {
        throw new RuntimeException("Failed to join in time");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
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
        @NonNull ReferenceQueue<? super EngineResource<?>> queue,
        boolean isActiveResourceRetentionAllowed) {
      super(referent, queue);
      this.key = Preconditions.checkNotNull(key);
      this.resource =
          referent.isCacheable() && isActiveResourceRetentionAllowed
              ? Preconditions.checkNotNull(referent.getResource()) : null;
      isCacheable = referent.isCacheable();
    }

    void reset() {
      resource = null;
      clear();
    }
  }
}
