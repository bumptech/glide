package com.bumptech.glide.load.resource.gif;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.load.Transformation;

/**
 * An animated {@link android.graphics.drawable.Drawable} that plays the frames of an animated GIF.
 */
public class GifDrawable extends Drawable implements Animatable, GifFrameManager.FrameCallback {

    private final Paint paint = new Paint();
    private GifFrameManager frameManager;
    private GifState state;
    private int width = -1;
    private int height = -1;
    private GifDecoder decoder;
    private boolean isRunning;
    private Bitmap currentFrame;
    private boolean isRecycled;

    public GifDrawable(String id, GifHeader gifHeader, byte[] data, Context context,
            Transformation<Bitmap> frameTransformation, int targetWidth, int targetHeight,
            GifDecoder.BitmapProvider bitmapProvider) {
        this(new GifState(id, gifHeader, data, context, frameTransformation, targetWidth, targetHeight,
                bitmapProvider));
    }

    private GifDrawable(GifState state) {
        this.state = state;
        this.decoder = new GifDecoder(state.bitmapProvider);
        decoder.setData(state.id, state.gifHeader, state.data);
        frameManager = new GifFrameManager(state.context, decoder, state.frameTransformation, state.targetWidth,
                state.targetHeight);
    }

    // For testing.
    GifDrawable(GifDecoder decoder, GifFrameManager frameManager) {
        this.decoder = decoder;
        this.frameManager = frameManager;
        this.state = new GifState(null);
    }


    @Override
    public void start() {
        if (!isRunning) {
            isRunning = true;
            frameManager.getNextFrame(this);
            invalidateSelf();
        }
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        if (!visible) {
            stop();
        } else {
            start();
        }
        return super.setVisible(visible, restart);
    }

    @Override
    public int getIntrinsicWidth() {
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }

    @Override
    public void stop() {
        isRunning = false;
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
    public void draw(Canvas canvas) {
        if (currentFrame != null) {
            canvas.drawBitmap(currentFrame, 0, 0, paint);
        }
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
        return decoder.isTransparent() ? PixelFormat.TRANSPARENT : PixelFormat.OPAQUE;
    }

    @TargetApi(11)
    @Override
    public void onFrameRead(Bitmap frame) {
        if (Build.VERSION.SDK_INT >= 11 && getCallback() == null) {
            stop();
            return;
        } if (!isRunning) {
            return;
        }

        if (width == -1) {
            width = frame.getWidth();
        }
        if (height == -1) {
            height = frame.getHeight();
        }

        if (frame != null) {
            currentFrame = frame;
            invalidateSelf();
        }

        frameManager.getNextFrame(this);
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
        frameManager.clear();
    }

    boolean isRecycled() {
        return isRecycled;
    }

    static class GifState extends ConstantState {
        String id;
        GifHeader gifHeader;
        byte[] data;
        Context context;
        Transformation<Bitmap> frameTransformation;
        int targetWidth;
        int targetHeight;
        GifDecoder.BitmapProvider bitmapProvider;

        public GifState(String id, GifHeader header, byte[] data, Context context,
                Transformation<Bitmap> frameTransformation, int targetWidth, int targetHeight,
                GifDecoder.BitmapProvider provider) {
            this.id = id;
            gifHeader = header;
            this.data = data;
            this.context = context.getApplicationContext();
            this.frameTransformation = frameTransformation;
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
            bitmapProvider = provider;
        }

        public GifState(GifState original) {
            if (original != null) {
                id = original.id;
                gifHeader = original.gifHeader;
                data = original.data;
                context = original.context;
                frameTransformation = original.frameTransformation;
                targetWidth = original.targetWidth;
                targetHeight = original.targetHeight;
                bitmapProvider = original.bitmapProvider;
            }
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return super.newDrawable(res);
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
