package com.bumptech.glide.load.resource.gif;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.bumptech.glide.gifdecoder.GifDecoder;

public class GifDrawable extends Drawable implements Animatable, GifFrameManager.FrameCallback {

    private final Paint paint;
    private final GifFrameManager frameManager;
    private int width;
    private int height;
    private GifDecoder decoder;
    private boolean isRunning;
    private Bitmap currentFrame;
    private boolean isRecycled;

    public GifDrawable(GifDecoder decoder, GifFrameManager frameManager) {
        this.decoder = decoder;
        this.frameManager = frameManager;
        width = -1;
        height = -1;

        paint = new Paint();
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

    public void recycle() {
        isRecycled = true;
        frameManager.clear();
    }

    public boolean isRecycled() {
        return isRecycled;
    }
}
