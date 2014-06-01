package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.load.data.transcode.BitmapBytesTranscoder;
import com.bumptech.glide.load.data.transcode.ResourceTranscoder;
import com.bumptech.glide.load.data.transcode.TranscoderFactory;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.provider.DataLoadProviderFactory;
import com.bumptech.glide.provider.FixedLoadProvider;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.target.ImageViewTargetFactory;

import java.io.InputStream;

public class TypeRequest<T> extends BitmapRequestBuilder<T, BitmapDrawable> {
    private final ModelLoader<T, InputStream> streamModelLoader;
    private final ModelLoader<T, ParcelFileDescriptor> fileDescriptorModelLoader;
    private final Context context;
    private final DataLoadProviderFactory dataLoadProviderFactory;
    private final BitmapPool bitmapPool;
    private final ImageViewTargetFactory imageViewTargetFactory;
    private final TranscoderFactory transcoderFactory;
    private final Engine engine;
    private final T model;

    private static <T, R> FixedLoadProvider<T, InputStream, Bitmap, R> buildImageProvider(
            TranscoderFactory transcoderFactory, DataLoadProviderFactory dataLoadProviderFactory,
            ModelLoader<T, InputStream> modelLoader, Class<R> transcodeClass) {
        return modelLoader == null ? null : new FixedLoadProvider<T, InputStream, Bitmap, R>(
                modelLoader,
                transcoderFactory.get(Bitmap.class, transcodeClass),
                dataLoadProviderFactory.get(InputStream.class, Bitmap.class));
    }

    private static <T, R> FixedLoadProvider<T, ParcelFileDescriptor, Bitmap, R> buildFileDescriptorProvider(
            TranscoderFactory transcoderFactory, DataLoadProviderFactory dataLoadProviderFactory,
            ModelLoader<T, ParcelFileDescriptor> modelLoader, Class<R> transcodeClass) {
        return modelLoader == null ? null : new FixedLoadProvider<T, ParcelFileDescriptor, Bitmap, R>(
                modelLoader,
                transcoderFactory.get(Bitmap.class, transcodeClass),
                dataLoadProviderFactory.get(ParcelFileDescriptor.class, Bitmap.class));
    }

    TypeRequest(T model, ModelLoader<T, InputStream> streamModelLoader,
            ModelLoader<T, ParcelFileDescriptor> fileDescriptorModelLoader, Context context,
            DataLoadProviderFactory dataLoadProviderFactory, BitmapPool bitmapPool,
            ImageViewTargetFactory imageViewTargetFactory, TranscoderFactory transcoderFactory, Engine engine) {
        super(context, model,
                buildImageProvider(transcoderFactory, dataLoadProviderFactory, streamModelLoader,
                        BitmapDrawable.class),
                buildFileDescriptorProvider(transcoderFactory, dataLoadProviderFactory, fileDescriptorModelLoader,
                        BitmapDrawable.class),
                BitmapDrawable.class, bitmapPool, imageViewTargetFactory, engine);
        this.model = model;
        this.streamModelLoader = streamModelLoader;
        this.fileDescriptorModelLoader = fileDescriptorModelLoader;
        this.context = context;
        this.dataLoadProviderFactory = dataLoadProviderFactory;
        this.bitmapPool = bitmapPool;
        this.imageViewTargetFactory = imageViewTargetFactory;
        this.transcoderFactory = transcoderFactory;
        this.engine = engine;
    }

    public BitmapRequestBuilder<T, Bitmap> asBitmap() {
        return new BitmapRequestBuilder<T, Bitmap>(context, model,
                buildImageProvider(transcoderFactory, dataLoadProviderFactory, streamModelLoader, Bitmap.class),
                buildFileDescriptorProvider(transcoderFactory, dataLoadProviderFactory, fileDescriptorModelLoader,
                        Bitmap.class),
                Bitmap.class, bitmapPool, imageViewTargetFactory, engine);
    }

    public BitmapRequestBuilder<T, byte[]> toBytes() {
        return transcode(byte[].class, new BitmapBytesTranscoder());
    }

    public BitmapRequestBuilder<T, byte[]> toBytes(Bitmap.CompressFormat compressFormat, int quality) {
        return transcode(byte[].class, new BitmapBytesTranscoder(compressFormat, quality));
    }

    public <R> BitmapRequestBuilder<T, R> transcode(Class<R> transcodeClass, ResourceTranscoder<Bitmap, R> transcoder) {
        TranscoderFactory requestFactory = new TranscoderFactory();
        requestFactory.register(Bitmap.class, transcodeClass, transcoder);
        LoadProvider<T, InputStream, Bitmap, R> streamLoadProvider = null;
        if (streamModelLoader != null) {
            streamLoadProvider = new FixedLoadProvider<T, InputStream, Bitmap, R>(streamModelLoader, transcoder,
                    dataLoadProviderFactory.get(InputStream.class, Bitmap.class));
        }
        LoadProvider<T, ParcelFileDescriptor, Bitmap, R> fileDescriptorLoadProvider = null;
        if (fileDescriptorModelLoader != null) {
            fileDescriptorLoadProvider = new FixedLoadProvider<T, ParcelFileDescriptor, Bitmap, R>(
                    fileDescriptorModelLoader, transcoder,
                    dataLoadProviderFactory.get(ParcelFileDescriptor.class, Bitmap.class));
        }

        return new BitmapRequestBuilder<T, R>(context, model, streamLoadProvider, fileDescriptorLoadProvider,
                transcodeClass, bitmapPool, imageViewTargetFactory, engine);
    }
}
