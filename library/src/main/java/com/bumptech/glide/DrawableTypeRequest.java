package com.bumptech.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.load.model.ImageVideoModelLoader;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapper;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.provider.FixedLoadProvider;

import java.io.InputStream;

public class DrawableTypeRequest<A> extends DrawableRequestBuilder<A> {
    private final ModelLoader<A, InputStream> streamModelLoader;
    private final ModelLoader<A, ParcelFileDescriptor> fileDescriptorModelLoader;
    private final Context context;
    private final Glide glide;
    private final A model;

    private static <A, Z, R> FixedLoadProvider<A, ImageVideoWrapper, Z, R> buildProvider(Glide glide,
            ModelLoader<A, InputStream> streamModelLoader,
            ModelLoader<A, ParcelFileDescriptor> fileDescriptorModelLoader, Class<Z> resourceClass,
            Class<R> transcodedClass,
            ResourceTranscoder<Z, R> transcoder) {
            return streamModelLoader == null && fileDescriptorModelLoader == null ? null :
                    new FixedLoadProvider<A, ImageVideoWrapper, Z, R>(
                            new ImageVideoModelLoader<A>(streamModelLoader, fileDescriptorModelLoader),
                            transcoder != null ? transcoder : glide.buildTranscoder(resourceClass, transcodedClass),
                            glide.buildDataProvider(ImageVideoWrapper.class, resourceClass));
    }


    DrawableTypeRequest(A model, ModelLoader<A, InputStream> streamModelLoader,
            ModelLoader<A, ParcelFileDescriptor> fileDescriptorModelLoader, Context context, Glide glide) {
        super(context, model,
                buildProvider(glide, streamModelLoader, fileDescriptorModelLoader, GifBitmapWrapper.class, Drawable.class,
                        null), glide);
        this.model = model;
        this.streamModelLoader = streamModelLoader;
        this.fileDescriptorModelLoader = fileDescriptorModelLoader;
        this.context = context;
        this.glide = glide;
    }

    public BitmapTypeRequest<A> asBitmap() {
        return new BitmapTypeRequest<A>(context, model, streamModelLoader, fileDescriptorModelLoader, glide);
    }
}
