package com.bumptech.glide.load.resource.gif;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;

import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Preconditions;

import java.nio.ByteBuffer;

/**
 * An animated {@link android.graphics.drawable.Drawable} that plays the frames of an animated GIF.
 */
public class GifDrawable extends Drawable implements GifFrameLoader.FrameCallback,
    Animatable {
  /**
   * A constant indicating that an animated drawable should loop continuously.
   */
  public static final int LOOP_FOREVER = -1;
  /**
   * A constant indicating that an animated drawable should loop for its default number of times.
   * For animated GIFs, this constant indicates the GIF should use the netscape loop count if
   * present.
   */
  public static final int LOOP_INTRINSIC = 0;

  private final GifState state;
  /**
   * True if the drawable is currently animating.
   */
  private boolean isRunning;
  /**
   * True if the drawable should animate while visible.
   */
  private boolean isStarted;
  /**
   * True if the drawable's resources have been recycled.
   */
  private boolean isRecycled;
  /**
   * True if the drawable is currently visible. Default to true because on certain platforms (at
   * least 4.1.1), setVisible is not called on {@link android.graphics.drawable.Drawable Drawables}
   * during {@link android.widget.ImageView#setImageDrawable(android.graphics.drawable.Drawable)}.
   * See issue #130.
   */
  private boolean isVisible = true;
  /**
   * The number of times we've looped over all the frames in the gif.
   */
  private int loopCount;
  /**
   * The number of times to loop through the gif animation.
   */
  private int maxLoopCount = LOOP_FOREVER;

  private boolean applyGravity;
  private Paint paint;
  private Rect destRect;


  /**
   * Constructor for GifDrawable.
   *
   * @param context             A context.
   * @param bitmapProvider      An {@link com.bumptech.glide.gifdecoder.GifDecoder.BitmapProvider}
   *                            that can be used to retrieve re-usable
   *                            {@link android.graphics.Bitmap}s.
   * @param bitmapPool          A {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool}
   *                            that can be used to return the first frame when this drawable is
   *                            recycled.
   * @param frameTransformation An {@link com.bumptech.glide.load.Transformation} that can be
   *                            applied to each frame.
   * @param targetFrameWidth    The desired width of the frames displayed by this drawable (the
   *                            width of the view or
   *                            {@link com.bumptech.glide.request.target.Target}
   *                            this drawable is being loaded into).
   * @param targetFrameHeight   The desired height of the frames displayed by this drawable (the
   *                            height of the view or
   *                            {@link com.bumptech.glide.request.target.Target}
   *                            this drawable is being loaded into).
   * @param gifHeader           The header data for this gif.
   * @param firstFrame          The decoded and transformed first frame of this gif.
   * @see #setFrameTransformation(com.bumptech.glide.load.Transformation, android.graphics.Bitmap)
   */
  public GifDrawable(Context context, GifDecoder.BitmapProvider bitmapProvider,
      BitmapPool bitmapPool, ByteBuffer byteBuffer, Transformation<Bitmap> frameTransformation,
      int targetFrameWidth, int targetFrameHeight, GifHeader gifHeader, Bitmap firstFrame) {
    this(new GifState(context, bitmapPool,
        new GifFrameLoader(context, bitmapProvider, gifHeader, byteBuffer, targetFrameWidth,
            targetFrameHeight, frameTransformation, firstFrame)));
  }

  GifDrawable(GifState state) {
    this.state = Preconditions.checkNotNull(state);
  }

  // Visible for testing.
  GifDrawable(Context context, GifFrameLoader frameLoader, BitmapPool bitmapPool, Paint paint) {
    this(new GifState(context, bitmapPool, frameLoader));
    this.paint = paint;
  }

  public int getSize() {
    return state.frameLoader.getSize();
  }

  public Bitmap getFirstFrame() {
    return state.frameLoader.getFirstFrame();
  }

  public void setFrameTransformation(Transformation<Bitmap> frameTransformation,
      Bitmap firstFrame) {
    state.frameLoader.setFrameTransformation(frameTransformation, firstFrame);
  }

  public Transformation<Bitmap> getFrameTransformation() {
    return state.frameLoader.getFrameTransformation();
  }

  public ByteBuffer getBuffer() {
    return state.frameLoader.getBuffer();
  }

  public int getFrameCount() {
    return state.frameLoader.getFrameCount();
  }

  private void resetLoopCount() {
    loopCount = 0;
  }

  @Override
  public void start() {
    isStarted = true;
    resetLoopCount();
    if (isVisible) {
      startRunning();
    }
  }

  @Override
  public void stop() {
    isStarted = false;
    stopRunning();
  }

  private void startRunning() {
    // If we have only a single frame, we don't want to decode it endlessly.
    if (state.frameLoader.getFrameCount() == 1) {
      invalidateSelf();
    } else if (!isRunning) {
      isRunning = true;
      state.frameLoader.subscribe(this);
      invalidateSelf();
    }
  }

  private void stopRunning() {
    isRunning = false;
    state.frameLoader.unsubscribe(this);
  }

  @Override
  public boolean setVisible(boolean visible, boolean restart) {
    isVisible = visible;
    if (!visible) {
      stopRunning();
    } else if (isStarted) {
      startRunning();
    }
    return super.setVisible(visible, restart);
  }

  @Override
  public int getIntrinsicWidth() {
    return state.frameLoader.getWidth();
  }

  @Override
  public int getIntrinsicHeight() {
    return state.frameLoader.getHeight();
  }

  @Override
  public boolean isRunning() {
    return isRunning;
  }

  // For testing.
  void setIsRunning(boolean isRunning) {
    this.isRunning = isRunning;
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    applyGravity = true;
  }

  @Override
  public void draw(Canvas canvas) {
    if (isRecycled) {
      return;
    }

    if (applyGravity) {
      Gravity.apply(GifState.GRAVITY, getIntrinsicWidth(), getIntrinsicHeight(), getBounds(),
          getDestRect());
      applyGravity = false;
    }

    Bitmap currentFrame = state.frameLoader.getCurrentFrame();
    canvas.drawBitmap(currentFrame, null, getDestRect(), getPaint());
  }

  @Override
  public void setAlpha(int i) {
    getPaint().setAlpha(i);
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter) {
    getPaint().setColorFilter(colorFilter);
  }

  private Rect getDestRect() {
    if (destRect == null) {
      destRect = new Rect();
    }
    return destRect;
  }

  private Paint getPaint() {
    if (paint == null) {
      paint = new Paint();
    }
    return paint;
  }

  @Override
  public int getOpacity() {
    // We can't tell, so default to transparent to be safe.
    return PixelFormat.TRANSPARENT;
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  @Override
  public void onFrameReady(int frameIndex) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && getCallback() == null) {
      stop();
      invalidateSelf();
      return;
    }

    invalidateSelf();

    if (frameIndex == state.frameLoader.getFrameCount() - 1) {
      loopCount++;
    }

    if (maxLoopCount != LOOP_FOREVER && loopCount >= maxLoopCount) {
      stop();
    }
  }

  @Override
  public ConstantState getConstantState() {
    return state;
  }

  /**
   * Clears any resources for loading frames that are currently held on to by this object.
   */
  public void recycle() {
    isRecycled = true;
    state.frameLoader.clear();
  }

  // For testing.
  boolean isRecycled() {
    return isRecycled;
  }

  public void setLoopCount(int loopCount) {
    if (loopCount <= 0 && loopCount != LOOP_FOREVER && loopCount != LOOP_INTRINSIC) {
      throw new IllegalArgumentException("Loop count must be greater than 0, or equal to "
          + "GlideDrawable.LOOP_FOREVER, or equal to GlideDrawable.LOOP_INTRINSIC");
    }

    if (loopCount == LOOP_INTRINSIC) {
      maxLoopCount = state.frameLoader.getLoopCount();
    } else {
      maxLoopCount = loopCount;
    }
  }

  static class GifState extends ConstantState {
    static final int GRAVITY = Gravity.FILL;
    final Context context;
    final BitmapPool bitmapPool;
    final GifFrameLoader frameLoader;

    public GifState(Context context, BitmapPool bitmapPool, GifFrameLoader frameLoader) {
      this.bitmapPool = bitmapPool;
      this.context = context.getApplicationContext();
      this.frameLoader = frameLoader;
    }

    @Override
    public Drawable newDrawable(Resources res) {
      return newDrawable();
    }

    @Override
    public Drawable newDrawable() {
      return new GifDrawable(this);
    }

    @Override
    public int getChangingConfigurations() {
      return 0;
    }
  }
}
