package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.NonNull;
import com.bumptech.glide.util.Util;

/**
 * A simple {@link com.bumptech.glide.request.target.Target} base class with default (usually no-op)
 * implementations of non essential methods that allows the caller to specify an exact width/height.
 * Typically use cases look something like this:
 *
 * <pre>{@code
 * Target<Bitmap> target =
 *     Glide.with(fragment)
 *       .asBitmap()
 *       .load("http://somefakeurl.com/fakeImage.jpeg")
 *       .apply(fitCenterTransform())
 *       .into(new SimpleTarget<Bitmap>(250, 250) {
 *
 *         {@literal @Override}
 *         public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
 *           // Do something with bitmap here.
 *         }
 *
 *       });
 * }
 * // At some later point, clear the Target to release the resources, prevent load queues from
 * // blowing out proportion, and to improve load times for future requests:
 * Glide.with(fragment).clear(target);
 * }</pre>
 *
 * <p><em>Warning!</em> this class is extremely prone to mis-use. Use SimpleTarget only as a last
 * resort. {@link ViewTarget} or a subclass of {@link ViewTarget} is almost always a better choice.
 *
 * <p><em>Don't forget to clear instances of this class!</em>. If you must use this class, keep in
 * mind that unlike {@link ViewTarget} it is not safe to load into new instances of this class
 * repeatedly if every instance updates the same underlying {@link View} or caller. If you need to
 * load into the same {@link View} or caller repeatedly using this class, always retain a reference
 * to the previous instance and either call {@link com.bumptech.glide.RequestManager#clear(Target)}
 * on the old instance before starting a new load or you must re-use the old instance for the new
 * load. Glide's {@link com.bumptech.glide.RequestBuilder#into(Target)} method returns the {@link
 * Target} instance you provided to make retaining a reference to the {@link Target} as easy as
 * possible. That said, you must wait until you're completely finished with the resource before
 * calling {@link com.bumptech.glide.RequestManager#clear(Target)} and you should always null out
 * references to any loaded resources in {@link Target#onLoadCleared(Drawable)}.
 *
 * <p>Always try to provide a size when using this class. Use {@link SimpleTarget#SimpleTarget(int,
 * int)} whenever possible with values that are <em>not</em> {@link Target#SIZE_ORIGINAL}. Using
 * {@link Target#SIZE_ORIGINAL} is unsafe if you're loading large images or are running your
 * application on older or memory constrained devices because it can cause Glide to load very large
 * images into memory. In some cases those images may throw {@link OutOfMemoryError} and in others
 * they may exceed the texture limit for the device, which will prevent them from being rendered.
 * Providing a valid size allows Glide to downsample large images, which can avoid issues with
 * texture size or memory limitations. You don't have to worry about providing a size in most cases
 * if you use {@link ViewTarget} so prefer {@link ViewTarget} over this class whenver possible.
 *
 * @see <a href="http://bumptech.github.io/glide/doc/targets.html">Glide's Target docs page</a>
 * @param <Z> The type of resource that this target will receive.
 * @deprecated Use {@link CustomViewTarget} if loading the content into a view, the download API if
 *     in the background
 *     (http://bumptech.github.io/glide/doc/getting-started.html#background-threads), or a {@link
 *     CustomTarget} for any specialized use-cases. Using {@link SimpleTarget} or {@link BaseTarget}
 *     is unsafe if the user does not implement {@link #onLoadCleared}, resulting in recycled
 *     bitmaps being referenced from the UI and hard to root-cause crashes.
 */
@Deprecated
public abstract class SimpleTarget<Z> extends BaseTarget<Z> {
  private final int width;
  private final int height;

  /**
   * Constructor for the target that uses {@link Target#SIZE_ORIGINAL} as the target width and
   * height.
   */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  public SimpleTarget() {
    this(SIZE_ORIGINAL, SIZE_ORIGINAL);
  }

  /**
   * Constructor for the target that takes the desired dimensions of the decoded and/or transformed
   * resource.
   *
   * @param width The width in pixels of the desired resource.
   * @param height The height in pixels of the desired resource.
   */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  public SimpleTarget(int width, int height) {
    this.width = width;
    this.height = height;
  }

  /**
   * Immediately calls the given callback with the sizes given in the constructor.
   *
   * @param cb {@inheritDoc}
   */
  @Override
  public final void getSize(@NonNull SizeReadyCallback cb) {
    if (!Util.isValidDimensions(width, height)) {
      throw new IllegalArgumentException(
          "Width and height must both be > 0 or Target#SIZE_ORIGINAL, but given"
              + " width: "
              + width
              + " and height: "
              + height
              + ", either provide dimensions in the constructor"
              + " or call override()");
    }
    cb.onSizeReady(width, height);
  }

  @Override
  public void removeCallback(@NonNull SizeReadyCallback cb) {
    // Do nothing, we never retain a reference to the callback.
  }
}
