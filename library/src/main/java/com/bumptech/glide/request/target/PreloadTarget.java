package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Synthetic;

/**
 * A one time use {@link com.bumptech.glide.request.target.Target} class that loads a resource into
 * memory and then clears itself.
 *
 * @param <Z> The type of resource that will be loaded into memory.
 */
public final class PreloadTarget<Z> extends CustomTarget<Z> {
  private static final int MESSAGE_CLEAR = 1;
  private static final Handler HANDLER =
      new Handler(
          Looper.getMainLooper(),
          new Callback() {
            @Override
            public boolean handleMessage(Message message) {
              if (message.what == MESSAGE_CLEAR) {
                ((PreloadTarget<?>) message.obj).clear();
                return true;
              }
              return false;
            }
          });

  private final RequestManager requestManager;

  /**
   * Returns a PreloadTarget.
   *
   * @param width The width in pixels of the desired resource.
   * @param height The height in pixels of the desired resource.
   * @param <Z> The type of the desired resource.
   */
  public static <Z> PreloadTarget<Z> obtain(RequestManager requestManager, int width, int height) {
    return new PreloadTarget<>(requestManager, width, height);
  }

  private PreloadTarget(RequestManager requestManager, int width, int height) {
    super(width, height);
    this.requestManager = requestManager;
  }

  @Override
  public void onResourceReady(@NonNull Z resource, @Nullable Transition<? super Z> transition) {
    HANDLER.obtainMessage(MESSAGE_CLEAR, this).sendToTarget();
  }

  @Override
  public void onLoadCleared(@Nullable Drawable placeholder) {
    // Do nothing, we don't retain a reference to our resource.
  }

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  void clear() {
    requestManager.clear(this);
  }
}
