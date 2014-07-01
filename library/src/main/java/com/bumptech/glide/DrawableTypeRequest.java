package com.bumptech.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.load.model.ImageVideoModelLoader;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapper;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.provider.FixedLoadProvider;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.io.InputStream;

public class DrawableTypeRequest<A> extends DrawableRequestBuilder<A> {
    private final ModelLoader<A, InputStream> streamModelLoader;
    private final ModelLoader<A, ParcelFileDescriptor> fileDescriptorModelLoader;
    private final Context context;
    private final Glide glide;
    private RequestTracker requestTracker;
    private RequestManager.OptionsApplier optionsApplier;
    private final A model;

    private static <A, Z, R> FixedLoadProvider<A, ImageVideoWrapper, Z, R> buildProvider(Glide glide,
            ModelLoader<A, InputStream> streamModelLoader,
            ModelLoader<A, ParcelFileDescriptor> fileDescriptorModelLoader, Class<Z> resourceClass,
            Class<R> transcodedClass,
            ResourceTranscoder<Z, R> transcoder) {
        if (streamModelLoader == null && fileDescriptorModelLoader == null) {
            return null;
        }

        if (transcoder == null) {
            transcoder = glide.buildTranscoder(resourceClass, transcodedClass);
        }
        DataLoadProvider<ImageVideoWrapper, Z> dataLoadProvider = glide.buildDataProvider(ImageVideoWrapper.class,
                resourceClass);
        ImageVideoModelLoader<A> modelLoader = new ImageVideoModelLoader<A>(streamModelLoader,
                fileDescriptorModelLoader);
        return new FixedLoadProvider<A, ImageVideoWrapper, Z, R>(modelLoader, transcoder, dataLoadProvider);
    }

    DrawableTypeRequest(A model, ModelLoader<A, InputStream> streamModelLoader,
            ModelLoader<A, ParcelFileDescriptor> fileDescriptorModelLoader, Context context, Glide glide,
            RequestTracker requestTracker, RequestManager.OptionsApplier optionsApplier) {
        super(context, model,
                buildProvider(glide, streamModelLoader, fileDescriptorModelLoader, GifBitmapWrapper.class,
                        Drawable.class, null),
                glide, requestTracker);
        this.model = model;
        this.streamModelLoader = streamModelLoader;
        this.fileDescriptorModelLoader = fileDescriptorModelLoader;
        this.context = context;
        this.glide = glide;
        this.requestTracker = requestTracker;
        this.optionsApplier = optionsApplier;
    }

    public BitmapTypeRequest<A> asBitmap() {
        return optionsApplier.apply(model, new BitmapTypeRequest<A>(context, model, streamModelLoader,
                fileDescriptorModelLoader, glide, requestTracker, optionsApplier));
    }

    public GifTypeRequest<A> asGif() {
        return optionsApplier.apply(model, new GifTypeRequest<A>(context, model, streamModelLoader, glide,
                requestTracker, optionsApplier));
    }

    /**
     * Loads the original unmodified data into the cache and calls the given Target with the cache File
     * @param target The Target that will receive the cache File when the load completes
     * @param <Y> The type of Target.
     * @return The given Target.
     */
    public <Y extends Target<File>> Y downloadOnly(Y target) {
        return getDownloadOnlyRequest().downloadOnly(target);
    }

    /**
     * Loads the original unmodified data into the cache and returns a {@link java.util.concurrent.Future} that can be
     * used to retrieve the cache File containing the data.
     * @param width The width to use to fetch the data.
     * @param height The height to use to fetch the data.
     * @return A {@link java.util.concurrent.Future} that can be used to retrieve the cache File containing the data.
     */
    public FutureTarget<File> downloadOnly(int width, int height) {
        return getDownloadOnlyRequest().downloadOnly(width, height);
    }

    private GenericTranscodeRequest<A, InputStream, File> getDownloadOnlyRequest() {
        return optionsApplier.apply(model, new GenericTranscodeRequest<A, InputStream, File>(context, glide, model,
                streamModelLoader, InputStream.class, File.class, requestTracker, optionsApplier));
    }
}
