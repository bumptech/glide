package com.bumptech.glide.load.resource.gif;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.load.resource.gif.decoder.GifDecoder;

public class GifDrawable extends Drawable implements Animatable, GifFrameManager.FrameCallback {

    private final Paint paint;
    private final GifFrameManager frameManager;
    private GifDecoder decoder;
    private boolean isRunning;
    private Bitmap currentFrame;
    private boolean isRecycled;

    public GifDrawable(GifDecoder decoder, GifFrameManager frameManager) {
        this.decoder = decoder;
        this.frameManager = frameManager;

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
        return decoder.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return decoder.getHeight();
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

    @Override
    public void onFrameRead(Bitmap frame) {
        if (!isRunning) {
            return;
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
