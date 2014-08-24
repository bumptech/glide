package com.bumptech.svgsample.app;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

public class SvgDrawable extends Drawable {
    private final Paint paint;
    private Svg svg;

    public SvgDrawable(Svg svg) {
        paint = new Paint();
        paint.setColor(0xFF0000AA);
        this.svg = svg;
    }

    @Override
    public void draw(Canvas canvas) {
        // Actually draw your svg here, see GlideDrawables
        canvas.drawCircle(getBounds().centerX(), getBounds().centerY(), 100, paint);
    }

    @Override
    public void setAlpha(int i) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }
}
