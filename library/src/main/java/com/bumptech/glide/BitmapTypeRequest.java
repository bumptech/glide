package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.load.model.ImageVideoModelLoader;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.transcode.BitmapBytesTranscoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.provider.DataLoadProvider;
import com.bumptech.glide.provider.FixedLoadProvider;

import java.io.InputStream;

/**
 * A class for creating a load request that either loads an {@link Bitmap} directly or that adds an
 * {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder} to transcode the {@link Bitmap} into another
 * resource type.
 *
 * @param <A> The type of model to load the {@link Bitmap} or transcoded class from.
 */
public class BitmapTypeRequest<A> extends BitmapRequestBuilder<A, Bitmap> {
    private final Context context;
    private final A model;
    private final ModelLoader<A, InputStream> streamModelLoader;
    private ModelLoader<A, ParcelFileDescriptor> fileDescriptorModelLoader;
    private final Glide glide;
    private RequestTracker requestTracker;
    private RequestManager.OptionsApplier optionsApplier;

    private static <A, R> FixedLoadProvider<A, ImageVideoWrapper, Bitmap, R> buildProvider(Glide glide,
            ModelLoader<A, InputStream> streamModelLoader,
            ModelLoader<A, ParcelFileDescriptor> fileDescriptorModelLoader,
            Class<R> transcodedClass, ResourceTranscoder<Bitmap, R> transcoder) {
        if (streamModelLoader == null && fileDescriptorModelLoader == null) {
            return null;
        }

        if (transcoder == null) {
            transcoder = glide.buildTranscoder(Bitmap.class, transcodedClass);
        }
        DataLoadProvider<ImageVideoWrapper, Bitmap> loadProvider = glide.buildDataProvider(ImageVideoWrapper.class,
                Bitmap.class);
        ImageVideoModelLoader<A> modelLoader = new ImageVideoModelLoader<A>(streamModelLoader,
                fileDescriptorModelLoader);

        return new FixedLoadProvider<A, ImageVideoWrapper, Bitmap, R>(modelLoader, transcoder, loadProvider);
    }

    BitmapTypeRequest(Context context, A model,
            ModelLoader<A, InputStream> streamModelLoader,
            ModelLoader<A, ParcelFileDescriptor> fileDescriptorModelLoader,
            Glide glide, RequestTracker requestTracker, RequestManager.OptionsApplier optionsApplier) {
        super(context, model,
                buildProvider(glide, streamModelLoader, fileDescriptorModelLoader, Bitmap.class, null),
                Bitmap.class,
                glide, requestTracker);
        this.context = context;
        this.model = model;
        this.streamModelLoader = streamModelLoader;
        this.fileDescriptorModelLoader = fileDescriptorModelLoader;
        this.glide = glide;
        this.requestTracker = requestTracker;
        this.optionsApplier = optionsApplier;
    }

    /**
     * Sets a transcoder to transcode the decoded and transformed {@link Bitmap} into another resource type.
     *
     * @param transcoder The transoder to use.
     * @param transcodeClass The {@link Class} of the resource the {@link Bitmap} will be transcoded to.
     * @param <R> The type of the resource the {@link Bitmap} will be transcoded to.
     * @return This request builder.
     */
    public <R> BitmapRequestBuilder<A, R> transcode(ResourceTranscoder<Bitmap, R> transcoder, Class<R> transcodeClass) {
        return optionsApplier.apply(model, new BitmapRequestBuilder<A, R>(context, model,
                buildProvider(glide, streamModelLoader, fileDescriptorModelLoader, transcodeClass, transcoder),
                transcodeClass, glide, requestTracker));
    }

    /**
     * Transcodes the decoded and transformed {@link Bitmap} to bytes by compressing it as a JPEG to a byte array.
     * array.
     *
     * @see #toBytes(android.graphics.Bitmap.CompressFormat, int)
     *
     * @return This request builder.
     */
    public BitmapRequestBuilder<A, byte[]> toBytes() {
        return transcode(new BitmapBytesTranscoder(), byte[].class);
    }

    /**
     * Transcodes the decoded and transformed {@link android.graphics.Bitmap} to bytes by compressing it using the
     * given format and quality to a byte array.
     *
     * @see android.graphics.Bitmap#compress(android.graphics.Bitmap.CompressFormat, int, java.io.OutputStream)
     * @see #toBytes()
     *
     * @param compressFormat The {@link android.graphics.Bitmap.CompressFormat} to use to compress the {@link Bitmap}.
     * @param quality The quality level from 0-100 to use to compress the {@link Bitmap}.
     * @return This request builder.
     */
    public BitmapRequestBuilder<A, byte[]> toBytes(Bitmap.CompressFormat compressFormat, int quality) {
        return transcode(new BitmapBytesTranscoder(compressFormat, quality), byte[].class);
    }
}
