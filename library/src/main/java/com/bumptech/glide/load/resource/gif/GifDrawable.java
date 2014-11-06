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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;

import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;

/**
 * An animated {@link android.graphics.drawable.Drawable} that plays the frames of an animated GIF.
 */
public class GifDrawable extends GlideDrawable implements GifFrameManager.FrameCallback {
    private final Paint paint = new Paint();
    private final Rect destRect = new Rect();
    private final GifFrameManager frameManager;
    private final GifState state;
    private final GifDecoder decoder;

    /** True if the drawable is currently animating. */
    private boolean isRunning;
    /** True if the drawable should animate while visible. */
    private boolean isStarted;
    /** True if the drawable's resources have been recycled. */
    private boolean isRecycled;
    /**
     * True if the drawable is currently visible. Default to true because on certain platforms (at least 4.1.1),
     * setVisible is not called on {@link android.graphics.drawable.Drawable Drawables} during
     * {@link android.widget.ImageView#setImageDrawable(android.graphics.drawable.Drawable)}. See issue #130.
     */
    private boolean isVisible = true;
    /** The number of times we've looped over all the frames in the gif. */
    private int loopCount;
    /** The number of times to loop through the gif animation. */
    private int maxLoopCount = LOOP_FOREVER;

    private boolean applyGravity;

    /**
     * Constructor for GifDrawable.
     *
     * @see #setFrameTransformation(com.bumptech.glide.load.Transformation, android.graphics.Bitmap)
     *
     * @param context A context.
     * @param bitmapProvider An {@link com.bumptech.glide.gifdecoder.GifDecoder.BitmapProvider} that can be used to
     *                       retrieve re-usable {@link android.graphics.Bitmap}s.
     * @param bitmapPool A {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} that can be used to return
     *                   the first frame when this drawable is recycled.
     * @param frameTransformation An {@link com.bumptech.glide.load.Transformation} that can be applied to each frame.
     * @param targetFrameWidth The desired width of the frames displayed by this drawable (the width of the view or
     *                         {@link com.bumptech.glide.request.target.Target} this drawable is being loaded into).
     * @param targetFrameHeight The desired height of the frames displayed by this drawable (the height of the view or
     *                          {@link com.bumptech.glide.request.target.Target} this drawable is being loaded into).
     * @param gifHeader The header data for this gif.
     * @param data The full bytes of the gif.
     * @param firstFrame The decoded and transformed first frame of this gif.
     */
    public GifDrawable(Context context, GifDecoder.BitmapProvider bitmapProvider, BitmapPool bitmapPool,
            Transformation<Bitmap> frameTransformation, int targetFrameWidth, int targetFrameHeight,
            GifHeader gifHeader, byte[] data, Bitmap firstFrame) {
        this(new GifState(gifHeader, data, context, frameTransformation, targetFrameWidth, targetFrameHeight,
                bitmapProvider, bitmapPool, firstFrame));
    }

    GifDrawable(GifState state) {
        if (state == null) {
            throw new NullPointerException("GifState must not be null");
        }

        this.state = state;
        this.decoder = new GifDecoder(state.bitmapProvider);
        decoder.setData(state.gifHeader, state.data);
        frameManager = new GifFrameManager(state.context, decoder, state.targetWidth, state.targetHeight);
        frameManager.setFrameTransformation(state.frameTransformation);
    }

    // Visible for testing.
    GifDrawable(GifDecoder decoder, GifFrameManager frameManager, Bitmap firstFrame, BitmapPool bitmapPool) {
        this.decoder = decoder;
        this.frameManager = frameManager;
        this.state = new GifState(null);
        state.bitmapPool = bitmapPool;
        state.firstFrame = firstFrame;
    }

    public Bitmap getFirstFrame() {
        return state.firstFrame;
    }

    public void setFrameTransformation(Transformation<Bitmap> frameTransformation, Bitmap firstFrame) {
        if (firstFrame == null) {
            throw new NullPointerException("The first frame of the GIF must not be null");
        }
        if (frameTransformation == null) {
            throw new NullPointerException("The frame transformation must not be null");
        }
        state.frameTransformation = frameTransformation;
        state.firstFrame = firstFrame;
        frameManager.setFrameTransformation(frameTransformation);
    }

    public GifDecoder getDecoder() {
        return decoder;
    }

    public Transformation<Bitmap> getFrameTransformation() {
        return state.frameTransformation;
    }

    public byte[] getData() {
        return state.data;
    }

    public int getFrameCount() {
        return decoder.getFrameCount();
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

        // On APIs > honeycomb we know our drawable is not being displayed anymore when it's callback is cleared and so
        // we can use the absence of a callback as an indication that it's ok to clear our temporary data. Prior to
        // honeycomb we can't tell if our callback is null and instead eagerly reset to avoid holding on to resources we
        // no longer need.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            reset();
        }
    }

    /**
     * Clears temporary data and resets the drawable back to the first frame.
     */
    private void reset() {
        frameManager.clear();
        invalidateSelf();
    }

    private void startRunning() {
        // If we have only a single frame, we don't want to decode it endlessly.
        if (decoder.getFrameCount() == 1) {
            invalidateSelf();
        }  else if (!isRunning) {
            isRunning = true;
            frameManager.getNextFrame(this);
            invalidateSelf();
        }
    }

    private void stopRunning() {
        isRunning = false;
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
        return state.firstFrame.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return state.firstFrame.getHeight();
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
            Gravity.apply(GifState.GRAVITY, getIntrinsicWidth(), getIntrinsicHeight(), getBounds(), destRect);
            applyGravity = false;
        }

        Bitmap currentFrame = frameManager.getCurrentFrame();
        Bitmap toDraw = currentFrame != null ? currentFrame : state.firstFrame;
        canvas.drawBitmap(toDraw, null, destRect, paint);
    }

    @Override
    public void setAlpha(int i) {
        paint.setAlpha(i);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        // We can't tell, so default to transparent to be safe.
        return PixelFormat.TRANSPARENT;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onFrameRead(int frameIndex) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && getCallback() == null) {
            stop();
            reset();
            return;
        }
        if (!isRunning) {
            return;
        }

        invalidateSelf();

        if (frameIndex == decoder.getFrameCount() - 1) {
            loopCount++;
        }

        if (maxLoopCount != LOOP_FOREVER && loopCount >= maxLoopCount) {
            stop();
        } else {
            frameManager.getNextFrame(this);
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
        state.bitmapPool.put(state.firstFrame);
        frameManager.clear();
    }

    // For testing.
    boolean isRecycled() {
        return isRecycled;
    }

    @Override
    public boolean isAnimated() {
        return true;
    }

    @Override
    public void setLoopCount(int loopCount) {
        if (loopCount <= 0 && loopCount != LOOP_FOREVER && loopCount != LOOP_INTRINSIC) {
            throw new IllegalArgumentException("Loop count must be greater than 0, or equal to "
                    + "GlideDrawable.LOOP_FOREVER, or equal to GlideDrawable.LOOP_INTRINSIC");
        }

        if (loopCount == LOOP_INTRINSIC) {
            maxLoopCount = decoder.getLoopCount();
        } else {
            maxLoopCount = loopCount;
        }
    }

    static class GifState extends ConstantState {
        private static final int GRAVITY = Gravity.FILL;
        GifHeader gifHeader;
        byte[] data;
        Context context;
        Transformation<Bitmap> frameTransformation;
        int targetWidth;
        int targetHeight;
        GifDecoder.BitmapProvider bitmapProvider;
        BitmapPool bitmapPool;
        Bitmap firstFrame;

        public GifState(GifHeader header, byte[] data, Context context,
                Transformation<Bitmap> frameTransformation, int targetWidth, int targetHeight,
                GifDecoder.BitmapProvider provider, BitmapPool bitmapPool, Bitmap firstFrame) {
            if (firstFrame == null) {
                throw new NullPointerException("The first frame of the GIF must not be null");
            }
            gifHeader = header;
            this.data = data;
            this.bitmapPool = bitmapPool;
            this.firstFrame = firstFrame;
            this.context = context.getApplicationContext();
            this.frameTransformation = frameTransformation;
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
            bitmapProvider = provider;
        }

        public GifState(GifState original) {
            if (original != null) {
                gifHeader = original.gifHeader;
                data = original.data;
                context = original.context;
                frameTransformation = original.frameTransformation;
                targetWidth = original.targetWidth;
                targetHeight = original.targetHeight;
                bitmapProvider = original.bitmapProvider;
                bitmapPool = original.bitmapPool;
                firstFrame = original.firstFrame;
            }
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
