package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import com.android.volley.RequestQueue;
import com.bumptech.glide.resize.Engine;
import com.bumptech.glide.resize.EngineBuilder;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.RequestContext;
import com.bumptech.glide.resize.bitmap.BitmapEncoder;
import com.bumptech.glide.resize.bitmap.FileDescriptorBitmapDecoder;
import com.bumptech.glide.resize.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.resize.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.volley.RequestQueueWrapper;

import java.io.InputStream;

public class GlideBuilder {
    private RequestQueue requestQueue;
    private Context context;
    private Engine engine;
    private BitmapPool bitmapPool;

    public GlideBuilder(Context context) {
        this.context = context.getApplicationContext();
    }

    public GlideBuilder setRequestQueue(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
        return this;
    }

    public GlideBuilder setEngine(Engine engine) {
        this.engine = engine;
        return this;
    }

    public GlideBuilder setBitmapPool(BitmapPool bitmapPool) {
        this.bitmapPool = bitmapPool;
        return this;
    }

    Glide createGlide() {
        if (requestQueue == null) {
            requestQueue = RequestQueueWrapper.getRequestQueue(context);
        }
        if (engine == null) {
            engine = new EngineBuilder(context).build();
        }

        //TODO: reconcile this with resource cache.
        if (bitmapPool == null) {
            if (Build.VERSION.SDK_INT >= 11) {
                final int safeCacheSize = ImageManager.getSafeMemoryCacheSize(context);
                final boolean isLowMemoryDevice = ImageManager.isLowMemoryDevice(context);
                bitmapPool = new LruBitmapPool(
                        isLowMemoryDevice ? safeCacheSize : 2 * safeCacheSize);
            } else {
                bitmapPool = new BitmapPoolAdapter();
            }
        }

        // Order matters here, this must be last.
        RequestContext requestContext = new RequestContext();
        requestContext.register(new BitmapEncoder(), Bitmap.class);
        requestContext.register(new StreamBitmapDecoder(bitmapPool), InputStream.class, Bitmap.class);
        requestContext.register(new FileDescriptorBitmapDecoder(bitmapPool), ParcelFileDescriptor.class,
                Bitmap.class);

        return new Glide(engine, requestQueue, requestContext, bitmapPool);
    }
}