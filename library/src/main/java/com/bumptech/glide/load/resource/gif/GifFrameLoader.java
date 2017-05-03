package com.bumptech.glide.load.resource.gif;

import static com.bumptech.glide.request.RequestOptions.diskCacheStrategyOf;
import static com.bumptech.glide.request.RequestOptions.signatureOf;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;
import com.bumptech.glide.util.Util;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class GifFrameLoader {
  private final GifDecoder gifDecoder;
  private final Handler handler;
  private final List<FrameCallback> callbacks = new ArrayList<>();
  @Synthetic final RequestManager requestManager;
  private final BitmapPool bitmapPool;

  private boolean isRunning = false;
  private boolean isLoadPending = false;
  private boolean startFromFirstFrame = false;
  private RequestBuilder<Bitmap> requestBuilder;
  private DelayTarget current;
  private boolean isCleared;
  private DelayTarget next;
  private Bitmap firstFrame;
  private Transformation<Bitmap> transformation;

  public interface FrameCallback {
    void onFrameReady();
  }

  public GifFrameLoader(
      Glide glide,
      GifDecoder gifDecoder,
      int width,
      int height,
      Transformation<Bitmap> transformation,
      Bitmap firstFrame) {
    this(
        glide.getBitmapPool(),
        Glide.with(glide.getContext()),
        gifDecoder,
        null /*handler*/,
        getRequestBuilder(Glide.with(glide.getContext()), width, height),
        transformation,
        firstFrame);
  }

  @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
  GifFrameLoader(
      BitmapPool bitmapPool,
      RequestManager requestManager,
      GifDecoder gifDecoder,
      Handler handler,
      RequestBuilder<Bitmap> requestBuilder,
      Transformation<Bitmap> transformation,
      Bitmap firstFrame) {
    this.requestManager = requestManager;
    if (handler == null) {
      handler = new Handler(Looper.getMainLooper(), new FrameLoaderCallback());
    }
    this.bitmapPool = bitmapPool;
    this.handler = handler;
    this.requestBuilder = requestBuilder;

    this.gifDecoder = gifDecoder;

    setFrameTransformation(transformation, firstFrame);
  }

  void setFrameTransformation(Transformation<Bitmap> transformation, Bitmap firstFrame) {
    this.transformation = Preconditions.checkNotNull(transformation);
    this.firstFrame = Preconditions.checkNotNull(firstFrame);
    requestBuilder = requestBuilder.apply(new RequestOptions().transform(transformation));
  }

  Transformation<Bitmap> getFrameTransformation() {
    return transformation;
  }

  Bitmap getFirstFrame() {
    return firstFrame;
  }

  void subscribe(FrameCallback frameCallback) {
    if (isCleared) {
      throw new IllegalStateException("Cannot subscribe to a cleared frame loader");
    }
    boolean start = callbacks.isEmpty();
    if (callbacks.contains(frameCallback)) {
      throw new IllegalStateException("Cannot subscribe twice in a row");
    }
    callbacks.add(frameCallback);
    if (start) {
      start();
    }
  }

  void unsubscribe(FrameCallback frameCallback) {
    callbacks.remove(frameCallback);
    if (callbacks.isEmpty()) {
      stop();
    }
  }

  int getWidth() {
    return getCurrentFrame().getWidth();
  }

  int getHeight() {
    return getCurrentFrame().getHeight();
  }

  int getSize() {
    return gifDecoder.getByteSize() + getFrameSize();
  }

  int getCurrentIndex() {
    return current != null ? current.index : -1;
  }

  private int getFrameSize() {
    return Util.getBitmapByteSize(getCurrentFrame().getWidth(), getCurrentFrame().getHeight(),
        getCurrentFrame().getConfig());
  }

  ByteBuffer getBuffer() {
    return gifDecoder.getData().asReadOnlyBuffer();
  }

  int getFrameCount() {
    return gifDecoder.getFrameCount();
  }

  int getLoopCount() {
    return gifDecoder.getTotalIterationCount();
  }

  private void start() {
    if (isRunning) {
      return;
    }
    isRunning = true;
    isCleared = false;

    loadNextFrame();
  }

  private void stop() {
    isRunning = false;
  }

  void clear() {
    callbacks.clear();
    recycleFirstFrame();
    stop();
    if (current != null) {
      requestManager.clear(current);
      current = null;
    }
    if (next != null) {
      requestManager.clear(next);
      next = null;
    }
    gifDecoder.clear();
    isCleared = true;
  }

  Bitmap getCurrentFrame() {
    return current != null ? current.getResource() : firstFrame;
  }

  private void loadNextFrame() {
    if (!isRunning || isLoadPending) {
      return;
    }
    if (startFromFirstFrame) {
      gifDecoder.resetFrameIndex();
      startFromFirstFrame = false;
    }
    isLoadPending = true;
    // Get the delay before incrementing the pointer because the delay indicates the amount of time
    // we want to spend on the current frame.
    int delay = gifDecoder.getNextDelay();
    long targetTime = SystemClock.uptimeMillis() + delay;

    gifDecoder.advance();
    next = new DelayTarget(handler, gifDecoder.getCurrentFrameIndex(), targetTime);
    requestBuilder.clone().apply(signatureOf(new FrameSignature())).load(gifDecoder).into(next);
  }

  private void recycleFirstFrame() {
    if (firstFrame != null) {
      bitmapPool.put(firstFrame);
      firstFrame = null;
    }
  }

  void setNextStartFromFirstFrame() {
    Preconditions.checkArgument(!isRunning, "Can't restart a running animation");
    startFromFirstFrame = true;
  }

  // Visible for testing.
  void onFrameReady(DelayTarget delayTarget) {
    if (isCleared) {
      handler.obtainMessage(FrameLoaderCallback.MSG_CLEAR, delayTarget).sendToTarget();
      return;
    }

    if (delayTarget.getResource() != null) {
      recycleFirstFrame();
      DelayTarget previous = current;
      current = delayTarget;
      // The callbacks may unregister when onFrameReady is called, so iterate in reverse to avoid
      // concurrent modifications.
      for (int i = callbacks.size() - 1; i >= 0; i--) {
        FrameCallback cb = callbacks.get(i);
        cb.onFrameReady();
      }
      if (previous != null) {
        handler.obtainMessage(FrameLoaderCallback.MSG_CLEAR, previous).sendToTarget();
      }
    }

    isLoadPending = false;
    loadNextFrame();
  }

  private class FrameLoaderCallback implements Handler.Callback {
    public static final int MSG_DELAY = 1;
    public static final int MSG_CLEAR = 2;

    @Synthetic
    FrameLoaderCallback() { }

    @Override
    public boolean handleMessage(Message msg) {
      if (msg.what == MSG_DELAY) {
        GifFrameLoader.DelayTarget target = (DelayTarget) msg.obj;
        onFrameReady(target);
        return true;
      } else if (msg.what == MSG_CLEAR) {
        GifFrameLoader.DelayTarget target = (DelayTarget) msg.obj;
        requestManager.clear(target);
      }
      return false;
    }
  }

  // Visible for testing.
  static class DelayTarget extends SimpleTarget<Bitmap> {
    private final Handler handler;
    @Synthetic final int index;
    private final long targetTime;
    private Bitmap resource;

    DelayTarget(Handler handler, int index, long targetTime) {
      this.handler = handler;
      this.index = index;
      this.targetTime = targetTime;
    }

    Bitmap getResource() {
      return resource;
    }

    @Override
    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
      this.resource = resource;
      Message msg = handler.obtainMessage(FrameLoaderCallback.MSG_DELAY, this);
      handler.sendMessageAtTime(msg, targetTime);
    }
  }

  private static RequestBuilder<Bitmap> getRequestBuilder(
      RequestManager requestManager, int width, int height) {
    return requestManager
        .asBitmap()
        .apply(
            diskCacheStrategyOf(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .override(width, height));
  }

  // Visible for testing.
  static class FrameSignature implements Key {
    private final UUID uuid;

    public FrameSignature() {
      this(UUID.randomUUID());
    }

    // VisibleForTesting.
    FrameSignature(UUID uuid) {
      this.uuid = uuid;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof FrameSignature) {
        FrameSignature other = (FrameSignature) o;
        return other.uuid.equals(uuid);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return uuid.hashCode();
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
      throw new UnsupportedOperationException("Not implemented");
    }
  }
}
