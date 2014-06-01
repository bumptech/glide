package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.load.data.transcode.BitmapDrawableTranscoder;
import com.bumptech.glide.load.data.transcode.ResourceTranscoder;
import com.bumptech.glide.load.data.transcode.TranscoderFactories;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.file_descriptor.FileDescriptorModelLoader;
import com.bumptech.glide.load.model.stream.StreamByteArrayLoader;
import com.bumptech.glide.load.model.stream.StreamFileLoader;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.bumptech.glide.load.model.stream.StreamResourceLoader;
import com.bumptech.glide.load.model.stream.StreamStringLoader;
import com.bumptech.glide.load.model.stream.StreamUriLoader;
import com.bumptech.glide.provider.FixedLoadProvider;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.target.ImageViewTargetFactory;
import com.bumptech.glide.volley.VolleyUrlLoader;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

 /**
 * A {@link BitmapRequestBuilder} builder that returns a request for a model that represents an image.
 */
public class ModelRequest {
    private final Context context;
    private final DataLoadProvider<InputStream, Bitmap> streamDataProvider;
    private final DataLoadProvider<ParcelFileDescriptor, Bitmap> fileDescriptorDataProvider;
    private BitmapPool bitmapPool;
    private ImageViewTargetFactory factory;
    private TranscoderFactories transcoderFactories;
    private Engine engine;

    ModelRequest(Context context, DataLoadProvider<InputStream, Bitmap> streamDataProvider,
            DataLoadProvider<ParcelFileDescriptor, Bitmap> fileDescriptorDataProvider,
            BitmapPool bitmapPool, ImageViewTargetFactory factory, TranscoderFactories transcoderFactories,
            Engine engine) {
        this.context = context;
        this.streamDataProvider = streamDataProvider;
        this.fileDescriptorDataProvider = fileDescriptorDataProvider;
        this.bitmapPool = bitmapPool;
        this.factory = factory;
        this.transcoderFactories = transcoderFactories;
        this.engine = engine;
    }

    /**
     * Set the {@link ModelLoader} to use for for a new load where the model loader translates from a model to an
     * {@link InputStream} resource for loading images.
     *
     * @param modelLoader The model loader to use.
     * @param <T> The type of the model.
     * @return A new {@link ImageModelRequest}.
     */
    public <T> ImageModelRequest<T> using(final StreamModelLoader<T> modelLoader) {
        return new ImageModelRequest<T>(context, modelLoader, streamDataProvider, bitmapPool, factory,
                transcoderFactories, engine);
    }

    /**
     * A convenience method to use a {@link StreamByteArrayLoader} to decode an image from a byte array.
     *
     * @param modelLoader The byte array loader.
     * @return A new {@link ImageModelRequest}.
     */
    public ImageModelRequest<byte[]> using(StreamByteArrayLoader modelLoader) {
        return new ImageModelRequest<byte[]>(context, modelLoader, streamDataProvider, bitmapPool, factory,
                transcoderFactories, engine);
    }

    /**
     * Set the {@link ModelLoader} to use for a new load where the model loader translates from a model to an
     * {@link ParcelFileDescriptor} resource for loading video thumbnails.
     *
     * @param modelLoader The model loader to use.
     * @param <T> The type of the model.
     * @return A new {@link VideoModelRequest}.
     */
    public <T> VideoModelRequest<T> using(final FileDescriptorModelLoader<T> modelLoader) {
        return new VideoModelRequest<T>(context, modelLoader, fileDescriptorDataProvider, bitmapPool, factory,
                transcoderFactories, engine);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link String} to load the image represented by
     * the given {@link String}. Defaults to {@link StreamStringLoader.Factory} and {@link StreamStringLoader} to
     * load the given model.
     *
     * @see #using(StreamModelLoader)
     *
     * @param string The string representing the image. Must be either a path, or a uri handled by
     *      {@link StreamUriLoader}
     * @return A {@link BitmapRequestBuilder} to set options for the load and ultimately the target to load the model
     * into.
     */
    public BitmapRequestBuilder<String, BitmapDrawable> load(String string) {
        return loadGeneric(string, BitmapDrawable.class);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link Uri} to load the image at the given uri.
     * Defaults to {@link StreamUriLoader.Factory} and {@link StreamUriLoader}.
     *
     * @see #using(StreamModelLoader)
     *
     * @param uri The uri representing the image. Must be a uri handled by {@link StreamUriLoader}
     * @return A {@link BitmapRequestBuilder} to set options for the load and ultimately the target to load the model
     * into.
     */
    public BitmapRequestBuilder<Uri, BitmapDrawable> load(Uri uri) {
        return loadGeneric(uri, BitmapDrawable.class);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link File} to load the image represented by the
     * given {@link File}. Defaults to {@link StreamFileLoader.Factory} and {@link StreamFileLoader} to load the
     * given model.
     *
     * @see #using(StreamModelLoader)
     *
     * @param file The File containing the image
     * @return A {@link BitmapRequestBuilder} to set options for the load and ultimately the target to load the model
     * into.
     */
    public BitmapRequestBuilder<File, BitmapDrawable> load(File file) {
        return loadGeneric(file, BitmapDrawable.class);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link Integer} to load the image represented by
     * the given {@link Integer} resource id. Defaults to {@link StreamResourceLoader.Factory} and
     * {@link StreamResourceLoader} to load the given model.
     *
     * @see #using(StreamModelLoader)
     *
     * @param resourceId the id of the resource containing the image
     * @return A {@link BitmapRequestBuilder} to set options for the load and ultimately the target to load the model
     * into.
     */
    public BitmapRequestBuilder<Integer, BitmapDrawable> load(Integer resourceId) {
        return loadGeneric(resourceId, BitmapDrawable.class);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for the given model type to load the image
     * represented by the given model.
     *
     * @param model The model to load.
     * @param <T> The type of the model to load.
     * @return A {@link BitmapRequestBuilder} to set options for the load and ultimately the target to load the image
     * into.
     */
    @SuppressWarnings("unused")
    public <T> BitmapRequestBuilder<T, BitmapDrawable> loadFromImage(T model) {
        return loadGeneric(model, BitmapDrawable.class);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link URL} to load the image represented by the
     * given {@link URL}. Defaults to {@link VolleyUrlLoader.Factory} and {@link VolleyUrlLoader} to load the given
     * model.
     *
     * @see #using(StreamModelLoader)
     *
     * @param url The URL representing the image.
     * @return A {@link BitmapRequestBuilder} to set options for the load and ultimately the target to load the model
     * into.
     */
    public BitmapRequestBuilder<URL, BitmapDrawable> loadFromImage(URL url) {
        return loadGeneric(url, BitmapDrawable.class);
    }

    /**
     * Use a new {@link StreamByteArrayLoader} to load an image from the given model.
     *
     * @see #loadFromImage(byte[])
     *
     * @param model The data to load.
     * @param id A unique id that identifies the image represented by the model suitable for use as a cache key
     *           (url, filepath etc). If there is no suitable id, use {@link #loadFromImage(byte[])} instaed.
     * @return A {@link BitmapRequestBuilder} to set options for the load and ultimately the target to load the image
     * into.
     */
    public BitmapRequestBuilder<byte[], BitmapDrawable> loadFromImage(byte[] model, final String id) {
        StreamByteArrayLoader loader = new StreamByteArrayLoader() {
            @Override
            public String getId(byte[] model) {
                return id;
            }
        };

        ResourceTranscoder<Bitmap, BitmapDrawable> transcoder = new BitmapDrawableTranscoder(context.getResources(),
                bitmapPool);
        LoadProvider<byte[], InputStream, Bitmap, BitmapDrawable> loadProvider =
                new FixedLoadProvider<byte[], InputStream, Bitmap, BitmapDrawable>(loader, transcoder,
                        streamDataProvider);

        return new BitmapRequestBuilder<byte[], BitmapDrawable>(context, model, loadProvider, null,
                BitmapDrawable.class, bitmapPool, factory, engine);
    }

    /**
     * Use a new {@link StreamByteArrayLoader} to load an image from the given model. Suitable when there is no
     * simple id that represents the given data.
     *
     * @param model the data to load.
     * @return A {@link BitmapRequestBuilder} to set options for the load and ultimately the target to load the image
     * into.
     */
    public BitmapRequestBuilder<byte[], BitmapDrawable> loadFromImage(byte[] model) {
        return loadFromImage(model, UUID.randomUUID()
                .toString());
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for the given model type for
     * {@link ParcelFileDescriptor}s to load a thumbnail for the video represented by the given model.
     *
     * @param model The model to load.
     * @param <T> The type of the model to load.
     * @return A {@link BitmapRequestBuilder} to set options for the load and ultimately the target to load the image
     * into.
     */
    @SuppressWarnings("unused")
    public <T> BitmapRequestBuilder<T, BitmapDrawable> loadFromVideo(T model) {
        return loadGeneric(model, BitmapDrawable.class);
    }

    /**
     * Use the {@link ModelLoaderFactory}s currently registered for the given model type for
     * {@link InputStream}s and {@link ParcelFileDescriptor}s to load a thumbnail from either the image or the video
     * represented by the given model.
     *
     * @param model The model the load.
     * @param <T> The type of the model to load.
     * @return A {@link BitmapRequestBuilder} to set options for the load and ultimately the target to load the image
     * into.
     */
    public <T> BitmapRequestBuilder<T, BitmapDrawable> load(T model) {
        return loadGeneric(model, BitmapDrawable.class);
    }

    private <T, R> BitmapRequestBuilder<T, R> loadGeneric(T model, Class<R> transcodeClass) {
        ResourceTranscoder<Bitmap, R> transcoder = transcoderFactories.get(Bitmap.class, transcodeClass);
        LoadProvider<T, InputStream, Bitmap, R> streamLoadProvider = null;
        ModelLoader<T, InputStream> streamModelLoader = Glide.buildStreamModelLoader(model, context);
        if (streamModelLoader != null) {
            streamLoadProvider = new FixedLoadProvider<T, InputStream, Bitmap, R>(streamModelLoader,
                    transcoder, streamDataProvider);
        }

        LoadProvider<T, ParcelFileDescriptor, Bitmap, R> fileDescriptorLoadProvider = null;
        ModelLoader<T, ParcelFileDescriptor> fileDescriptorModelLoader =
                Glide.buildFileDescriptorModelLoader(model, context);
        if (fileDescriptorModelLoader != null) {
            fileDescriptorLoadProvider = new FixedLoadProvider<T, ParcelFileDescriptor, Bitmap, R>(
                    fileDescriptorModelLoader, transcoder, fileDescriptorDataProvider);
        }
        return new BitmapRequestBuilder<T, R>(context, model, streamLoadProvider, fileDescriptorLoadProvider,
                transcodeClass, bitmapPool, factory, engine);
    }

    /**
     * A helper class for building requests with custom {@link ModelLoader}s that translate models to
     * {@link ParcelFileDescriptor} resources for loading video thumbnails.
     *
     * @param <T> The type of the model.
     */
    public static class VideoModelRequest<T> {
        private final Context context;
        private final ModelLoader<T, ParcelFileDescriptor> loader;
        private DataLoadProvider<ParcelFileDescriptor, Bitmap> dataLoadProvider;
        private BitmapPool bitmapPool;
        private ImageViewTargetFactory factory;
        private TranscoderFactories transcoderFactories;
        private Engine engine;

        private VideoModelRequest(Context context, ModelLoader<T, ParcelFileDescriptor> loader,
                DataLoadProvider<ParcelFileDescriptor, Bitmap> dataLoadProvider,  BitmapPool bitmapPool,
                ImageViewTargetFactory factory, TranscoderFactories transcoderFactories, Engine engine) {
            this.context = context;
            this.loader = loader;
            this.dataLoadProvider = dataLoadProvider;
            this.bitmapPool = bitmapPool;
            this.factory = factory;
            this.transcoderFactories = transcoderFactories;
            this.engine = engine;
        }

        public BitmapRequestBuilder<T, BitmapDrawable> loadFromVideo(T model) {
            ResourceTranscoder<Bitmap, BitmapDrawable> transcoder = transcoderFactories.get(Bitmap.class,
                    BitmapDrawable.class);
            return new BitmapRequestBuilder<T, BitmapDrawable>(context, model, null,
                    new FixedLoadProvider<T, ParcelFileDescriptor, Bitmap, BitmapDrawable>(loader, transcoder,
                            dataLoadProvider), BitmapDrawable.class, bitmapPool, factory, engine);
        }
    }

    /**
     * A helper class for building requests with custom {@link ModelLoader}s that translate models to
     * {@link InputStream} resources for loading images.
     *
     * @param <T> The type of the model.
     */
    public static class ImageModelRequest<T> {
        private final Context context;
        private final ModelLoader<T, InputStream> loader;
        private DataLoadProvider<InputStream, Bitmap> dataLoadProvider;
        private BitmapPool bitmapPool;
        private ImageViewTargetFactory factory;
        private TranscoderFactories factories;
        private Engine engine;

        private ImageModelRequest(Context context, ModelLoader<T, InputStream> loader,
                DataLoadProvider<InputStream, Bitmap> dataLoadProvider, BitmapPool bitmapPool,
                ImageViewTargetFactory factory, TranscoderFactories factories, Engine engine) {
            this.context = context;
            this.loader = loader;
            this.dataLoadProvider = dataLoadProvider;
            this.bitmapPool = bitmapPool;
            this.factory = factory;
            this.factories = factories;
            this.engine = engine;
        }

        public BitmapRequestBuilder<T, BitmapDrawable> load(T model) {
            ResourceTranscoder<Bitmap, BitmapDrawable> transcoder = factories.get(Bitmap.class, BitmapDrawable.class);

            return new BitmapRequestBuilder<T, BitmapDrawable>(context, model,
                    new FixedLoadProvider<T, InputStream, Bitmap, BitmapDrawable>(loader, transcoder, dataLoadProvider),
                    null, BitmapDrawable.class, bitmapPool, factory, engine);
        }
    }
}
