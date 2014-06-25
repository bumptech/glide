package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.GlideAnimation;
import com.bumptech.glide.request.Request;

public class PreloadTarget implements Target {
    private Request request;
    private int width;
    private int height;

    public PreloadTarget(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void onResourceReady(Object resource, GlideAnimation glideAnimation) {
        Glide.clear(this);
    }

    @Override
    public void setPlaceholder(Drawable placeholder) {

    }

    @Override
    public void getSize(SizeReadyCallback cb) {
        cb.onSizeReady(width, height);
    }

    @Override
    public void setRequest(Request request) {
        this.request = request;
    }

    @Override
    public Request getRequest() {
        return request;
    }
}
