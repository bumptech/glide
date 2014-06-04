package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.load.resource.transcode.BitmapBytesTranscoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.load.model.ImageVideoModelLoader;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.provider.FixedLoadProvider;

import java.io.InputStream;

public class BitmapTypeRequest<A> extends BitmapRequestBuilder<A, BitmapDrawable> {
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


    BitmapTypeRequest(A model, ModelLoader<A, InputStream> streamModelLoader,
            ModelLoader<A, ParcelFileDescriptor> fileDescriptorModelLoader, Context context, Glide glide) {
        super(context, model,
                buildProvider(glide, streamModelLoader, fileDescriptorModelLoader, Bitmap.class, BitmapDrawable.class,
                        null),
                BitmapDrawable.class, glide);
        this.model = model;
        this.streamModelLoader = streamModelLoader;
        this.fileDescriptorModelLoader = fileDescriptorModelLoader;
        this.context = context;
        this.glide = glide;
    }

    public BitmapRequestBuilder<A, Bitmap> asBitmap() {
        return new BitmapRequestBuilder<A, Bitmap>(context, model,
                buildProvider(glide, streamModelLoader, fileDescriptorModelLoader, Bitmap.class, Bitmap.class, null),
                Bitmap.class, glide);
    }

    public BitmapRequestBuilder<A, byte[]> toBytes() {
        return transcode(new BitmapBytesTranscoder(), byte[].class);
    }

    public BitmapRequestBuilder<A, byte[]> toBytes(Bitmap.CompressFormat compressFormat, int quality) {
        return transcode(new BitmapBytesTranscoder(compressFormat, quality), byte[].class);
    }

    public <R> BitmapRequestBuilder<A, R> transcode(ResourceTranscoder<Bitmap, R> transcoder, Class<R> transcodeClass) {
        FixedLoadProvider<A, ImageVideoWrapper, Bitmap, R> loadProvider = buildProvider(glide,
                streamModelLoader, fileDescriptorModelLoader, Bitmap.class, transcodeClass, transcoder);

        return new BitmapRequestBuilder<A, R>(context, model, loadProvider,
                transcodeClass, glide).transcoder(transcoder);
    }
}
