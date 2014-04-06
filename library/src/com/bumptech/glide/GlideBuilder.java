package com.bumptech.glide;

import android.content.Context;
import com.android.volley.RequestQueue;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.volley.RequestQueueWrapper;

public class GlideBuilder {
    private ImageManager imageManager;
    private RequestQueue requestQueue;
    private Context context;

    public GlideBuilder(Context context) {
        this.context = context.getApplicationContext();
    }

    public GlideBuilder setImageManager(ImageManager imageManager) {
        this.imageManager = imageManager;
        return this;
    }

    public GlideBuilder setRequestQueue(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
        return this;
    }

    Glide createGlide() {
        if (imageManager == null) {
            imageManager = new ImageManager.Builder(context).build();
        }
        if (requestQueue == null) {
            requestQueue = RequestQueueWrapper.getRequestQueue(context);
        }
        return new Glide(imageManager, requestQueue);
    }
}