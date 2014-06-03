package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.load.data.transcode.BitmapBytesTranscoder;
import com.bumptech.glide.load.data.transcode.ResourceTranscoder;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.provider.FixedLoadProvider;
import com.bumptech.glide.provider.LoadProvider;

import java.io.InputStream;

public class BitmapTypeRequest<A> extends BitmapRequestBuilder<A, BitmapDrawable> {
    private final ModelLoader<A, InputStream> streamModelLoader;
    private final ModelLoader<A, ParcelFileDescriptor> fileDescriptorModelLoader;
    private final Context context;
    private final Glide glide;
    private final A model;

    private static <T, Z, R> FixedLoadProvider<T, InputStream, Z, R> buildImageProvider(Glide glide,
            ModelLoader <T, InputStream> modelLoader, Class<Z> resourceClass, Class<R> transcodeClass,
            ResourceTranscoder<Z, R> transcoder) {
        return buildProvider(glide, modelLoader, InputStream.class, resourceClass, transcodeClass, transcoder);
    }

    private static <A, T, Z, R> FixedLoadProvider<A, T, Z, R> buildProvider(Glide glide,
            ModelLoader<A, T> modelLoader, Class<T> dataClass, Class<Z> resourceClass, Class<R> transcodedClass,
            ResourceTranscoder<Z, R> transcoder) {
            return modelLoader == null ? null : new FixedLoadProvider<A, T, Z, R>(
                    modelLoader,
                    transcoder != null ? transcoder : glide.buildTranscoder(resourceClass, transcodedClass),
                    glide.buildDataProvider(dataClass, resourceClass));
    }

    private static <T, Z, R> FixedLoadProvider<T, ParcelFileDescriptor, Z, R> buildVideoLoadProvider(
            Glide glide, ModelLoader<T, ParcelFileDescriptor> modelLoader, Class<Z> resourceClass,
            Class<R> transcodeClass, ResourceTranscoder<Z, R> transcoder) {
        return buildProvider(glide, modelLoader, ParcelFileDescriptor.class, resourceClass, transcodeClass,
                transcoder);
    }

    BitmapTypeRequest(A model, ModelLoader<A, InputStream> streamModelLoader,
            ModelLoader<A, ParcelFileDescriptor> fileDescriptorModelLoader, Context context, Glide glide) {
        super(context, model,
                buildImageProvider(glide, streamModelLoader, Bitmap.class, BitmapDrawable.class, null),
                buildVideoLoadProvider(glide, fileDescriptorModelLoader, Bitmap.class,BitmapDrawable.class, null),
                BitmapDrawable.class, glide);
        this.model = model;
        this.streamModelLoader = streamModelLoader;
        this.fileDescriptorModelLoader = fileDescriptorModelLoader;
        this.context = context;
        this.glide = glide;
    }

    public BitmapRequestBuilder<A, Bitmap> asBitmap() {
        return new BitmapRequestBuilder<A, Bitmap>(context, model,
                buildImageProvider(glide, streamModelLoader, Bitmap.class, Bitmap.class, null),
                buildVideoLoadProvider(glide, fileDescriptorModelLoader, Bitmap.class, Bitmap.class, null),
                Bitmap.class, glide);
    }

    public BitmapRequestBuilder<A, byte[]> toBytes() {
        return transcode(new BitmapBytesTranscoder(), byte[].class);
    }

    public BitmapRequestBuilder<A, byte[]> toBytes(Bitmap.CompressFormat compressFormat, int quality) {
        return transcode(new BitmapBytesTranscoder(compressFormat, quality), byte[].class);
    }

    public <R> BitmapRequestBuilder<A, R> transcode(ResourceTranscoder<Bitmap, R> transcoder, Class<R> transcodeClass) {
        LoadProvider<A, InputStream, Bitmap, R> streamLoadProvider = buildImageProvider(glide, streamModelLoader,
                Bitmap.class, transcodeClass, transcoder);
        LoadProvider<A, ParcelFileDescriptor, Bitmap, R> fileDescriptorLoadProvider = buildVideoLoadProvider(glide,
                fileDescriptorModelLoader, Bitmap.class, transcodeClass, transcoder);

        return new BitmapRequestBuilder<A, R>(context, model, streamLoadProvider, fileDescriptorLoadProvider,
                transcodeClass, glide).transcoder(transcoder);
    }
}
