package com.bumptech.glide;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.file_descriptor.FileDescriptorModelLoader;
import com.bumptech.glide.load.model.stream.MediaStoreStreamLoader;
import com.bumptech.glide.load.model.stream.StreamByteArrayLoader;
import com.bumptech.glide.load.model.stream.StreamFileLoader;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.bumptech.glide.load.model.stream.StreamResourceLoader;
import com.bumptech.glide.load.model.stream.StreamStringLoader;
import com.bumptech.glide.load.model.stream.StreamUriLoader;
import com.bumptech.glide.manager.RequestManager;
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
    private Glide glide;
    private RequestManager requestManager;

    ModelRequest(Context context, Glide glide, RequestManager requestManager) {
        this.context = context;
        this.glide = glide;
        this.requestManager = requestManager;
    }

    /**
     * Use the given generic model loader to load the given generic data class.
     * <p>
     *     Note that in most cases you will also need to specify an {@link ResourceDecoder} and an
     *     {@link ResourceEncoder} for the load to complete successfully.
     * </p>
     * @param modelLoader The {@link ModelLoader} class to use to load the model.
     * @param dataClass The type of data the {@link ModelLoader} will load.
     * @param <A> The type of the model to be loaded.
     * @param <T> The type of the data to be loaded from the mode.
     * @return A {@link GenericModelRequest} to set options for the load and ultimately the target to load the model
     * into.
     */
    public <A, T> GenericModelRequest<A, T> using(ModelLoader<A, T> modelLoader, Class<T> dataClass) {
        return new GenericModelRequest<A, T>(context, glide, modelLoader, dataClass, requestManager);
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
        return new ImageModelRequest<T>(context, modelLoader, glide, requestManager);
    }

    /**
     * A convenience method to use a {@link StreamByteArrayLoader} to decode an image from a byte array.
     *
     * @param modelLoader The byte array loader.
     * @return A new {@link ImageModelRequest}.
     */
    public ImageModelRequest<byte[]> using(StreamByteArrayLoader modelLoader) {
        return new ImageModelRequest<byte[]>(context, modelLoader, glide, requestManager);
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
        return new VideoModelRequest<T>(context, modelLoader, glide, requestManager);
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
     * @return A {@link DrawableTypeRequest} to set options for the load and ultimately the target to load the model
     * into.
     */
    public DrawableTypeRequest<String> load(String string) {
        return loadGeneric(string);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link Uri} to load the image at the given uri.
     * Defaults to {@link StreamUriLoader.Factory} and {@link StreamUriLoader}.
     *
     * @see #using(StreamModelLoader)
     *
     * @param uri The uri representing the image. Must be a uri handled by {@link StreamUriLoader}
     * @return A {@link DrawableTypeRequest} to set options for the load and ultimately the target to load the model
     * into.
     */
    public DrawableTypeRequest<Uri> load(Uri uri) {
        return loadGeneric(uri);
    }

    /**
     * Use {@link MediaStore.Images.Thumbnails} and {@link MediaStore.Video.Thumbnails} to retrieve pre-generated
     * thumbnails for the given uri. Falls back to the registered {@link ModelLoaderFactory} registered for {@link Uri}s
     * if the given uri is not a media store uri or if no pre-generated thumbnail exists for the given uri. In addition,
     * mixes the given mimeType, dateModified, and orientation into the cache key to detect and invalidate thumbnails
     * if content is changed locally.
     *
     * @param uri The uri representing the media.
     * @param mimeType The mime type of the media store media. Ok to default to empty string "". See
     *      {@link MediaStore.Images.ImageColumns#MIME_TYPE} or {@link MediaStore.Video.VideoColumns#MIME_TYPE}.
     * @param dateModified The date modified time of the media store media. Ok to default to 0. See
     *      {@link MediaStore.Images.ImageColumns#DATE_MODIFIED} or {@link MediaStore.Video.VideoColumns#DATE_MODIFIED}.
     * @param orientation The orientation of the media store media. Ok to default to 0. See
     *      {@link MediaStore.Images.ImageColumns#ORIENTATION}.
     * @return A new {@link DrawableRequestBuilder} to set options for the load and ultimately the target to load the
     *      uri into.
     */
    public DrawableTypeRequest<Uri> loadFromMediaStore(Uri uri, String mimeType, long dateModified, int orientation) {
        ModelLoader<Uri, InputStream> genericStreamLoader = Glide.buildStreamModelLoader(uri, context);
        ModelLoader<Uri, InputStream> mediaStoreLoader = new MediaStoreStreamLoader(context, genericStreamLoader,
                mimeType, dateModified, orientation);
        ModelLoader<Uri, ParcelFileDescriptor> fileDescriptorModelLoader = Glide.buildFileDescriptorModelLoader(uri,
                context);
        return new DrawableTypeRequest<Uri>(uri, mediaStoreLoader, fileDescriptorModelLoader, context, glide,
                requestManager);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link File} to load the image represented by the
     * given {@link File}. Defaults to {@link StreamFileLoader.Factory} and {@link StreamFileLoader} to load the
     * given model.
     *
     * @see #using(StreamModelLoader)
     *
     * @param file The File containing the image
     * @return A {@link DrawableTypeRequest} to set options for the load and ultimately the target to load the model
     * into.
     */
    public DrawableTypeRequest<File> load(File file) {
        return loadGeneric(file);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link Integer} to load the image represented by
     * the given {@link Integer} resource id. Defaults to {@link StreamResourceLoader.Factory} and
     * {@link StreamResourceLoader} to load the given model.
     *
     * @see #using(StreamModelLoader)
     *
     * @param resourceId the id of the resource containing the image
     * @return A {@link DrawableTypeRequest} to set options for the load and ultimately the target to load the model
     * into.
     */
    public DrawableTypeRequest<Integer> load(Integer resourceId) {
        return loadGeneric(resourceId);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for the given model type to load the image
     * represented by the given model.
     *
     * @param model The model to load.
     * @param <T> The type of the model to load.
     * @return A {@link DrawableTypeRequest} to set options for the load and ultimately the target to load the image
     * into.
     */
    @SuppressWarnings("unused")
    public <T> DrawableTypeRequest<T> loadFromImage(T model) {
        return loadGeneric(model);
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for {@link URL} to load the image represented by the
     * given {@link URL}. Defaults to {@link VolleyUrlLoader.Factory} and {@link VolleyUrlLoader} to load the given
     * model.
     *
     * @see #using(StreamModelLoader)
     *
     * @param url The URL representing the image.
     * @return A {@link DrawableTypeRequest} to set options for the load and ultimately the target to load the model
     * into.
     */
    public DrawableTypeRequest<URL> loadFromImage(URL url) {
        return loadGeneric(url);
    }

    /**
     * Use a new {@link StreamByteArrayLoader} to load an image from the given model.
     *
     * @see #loadFromImage(byte[])
     *
     * @param model The data to load.
     * @param id A unique id that identifies the image represented by the model suitable for use as a cache key
     *           (url, filepath etc). If there is no suitable id, use {@link #loadFromImage(byte[])} instaed.
     * @return A {@link DrawableTypeRequest} to set options for the load and ultimately the target to load the image
     * into.
     */
    public DrawableTypeRequest<byte[]> loadFromImage(byte[] model, final String id) {
        StreamByteArrayLoader loader = new StreamByteArrayLoader() {
            @Override
            public String getId(byte[] model) {
                return id;
            }
        };
        return new DrawableTypeRequest<byte[]>(model, loader, null, context, glide, requestManager);
    }

    /**
     * Use a new {@link StreamByteArrayLoader} to load an image from the given model. Suitable when there is no
     * simple id that represents the given data.
     *
     * @param model the data to load.
     * @return A {@link DrawableTypeRequest} to set options for the load and ultimately the target to load the image
     * into.
     */
    public DrawableTypeRequest<byte[]> loadFromImage(byte[] model) {
        return loadFromImage(model, UUID.randomUUID().toString());
    }

    /**
     * Use the {@link ModelLoaderFactory} currently registered for the given model type for
     * {@link ParcelFileDescriptor}s to load a thumbnail for the video represented by the given model.
     *
     * @param model The model to load.
     * @param <T> The type of the model to load.
     * @return A {@link DrawableTypeRequest} to set options for the load and ultimately the target to load the image
     * into.
     */
    @SuppressWarnings("unused")
    public <T> DrawableTypeRequest<T> loadFromVideo(T model) {
        return loadGeneric(model);
    }

    /**
     * Use the {@link ModelLoaderFactory}s currently registered for the given model type for
     * {@link InputStream}s and {@link ParcelFileDescriptor}s to load a thumbnail from either the image or the video
     * represented by the given model.
     *
     * @param model The model the load.
     * @param <T> The type of the model to load.
     * @return A {@link DrawableTypeRequest} to set options for the load and ultimately the target to load the image
     * into.
     */
    public <T> DrawableTypeRequest<T> load(T model) {
        return loadGeneric(model);
    }

    private <T> DrawableTypeRequest<T> loadGeneric(T model) {
        ModelLoader<T, InputStream> streamModelLoader = Glide.buildStreamModelLoader(model, context);
        ModelLoader<T, ParcelFileDescriptor> fileDescriptorModelLoader =
                Glide.buildFileDescriptorModelLoader(model, context);
        return new DrawableTypeRequest<T>(model, streamModelLoader, fileDescriptorModelLoader, context, glide,
                requestManager);
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
        private Glide glide;
        private RequestManager requestManager;

        private VideoModelRequest(Context context, ModelLoader<T, ParcelFileDescriptor> loader, Glide glide,
                RequestManager requestManager) {
            this.context = context;
            this.loader = loader;
            this.glide = glide;
            this.requestManager = requestManager;
        }

        public BitmapTypeRequest<T> loadFromVideo(T model) {
            return new BitmapTypeRequest<T>(context, model, null, loader, glide, requestManager);
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
        private Glide glide;
        private RequestManager requestManager;

        private ImageModelRequest(Context context, ModelLoader<T, InputStream> loader, Glide glide,
                RequestManager requestManager) {
            this.context = context;
            this.loader = loader;
            this.glide = glide;
            this.requestManager = requestManager;
        }

        public DrawableTypeRequest<T> load(T model) {
            return new DrawableTypeRequest<T>(model, loader, null, context, glide, requestManager);
        }
    }

    /**
     * A helper class for building requests with custom {@link ModelLoader}s that translate models to
     * {@link InputStream} resources for loading images.
     *
     * @param <T> The type of the model.
     */
    public static class GenericModelRequest<A, T> {
        private final Context context;
        private final Glide glide;
        private final ModelLoader<A, T> modelLoader;
        private final Class<T> dataClass;
        private RequestManager requestManager;

        private GenericModelRequest(Context context, Glide glide, ModelLoader<A, T> modelLoader, Class<T> dataClass,
                RequestManager requestManager) {
            this.context = context;
            this.glide = glide;
            this.modelLoader = modelLoader;
            this.dataClass = dataClass;
            this.requestManager = requestManager;
        }

        public GenericTypeRequest<A, T> load(A model) {
            return new GenericTypeRequest<A, T>(context, glide, model, modelLoader, dataClass, requestManager);
        }

        public static class GenericTypeRequest<A, T> {
            private final Context context;
            private final Glide glide;
            private final A model;
            private final ModelLoader<A, T> modelLoader;
            private final Class<T> dataClass;
            private RequestManager requestManager;

            private GenericTypeRequest(Context context, Glide glide, A model, ModelLoader<A, T> modelLoader,
                    Class<T> dataClass, RequestManager requestManager) {
                this.context = context;
                this.glide = glide;
                this.model = model;
                this.modelLoader = modelLoader;
                this.dataClass = dataClass;
                this.requestManager = requestManager;
            }

            public <Z> GenericTranscodeRequest<A, T, Z> as(Class<Z> resourceClass) {
                return new GenericTranscodeRequest<A, T, Z>(context, glide, model, modelLoader, dataClass,
                        resourceClass, requestManager);
            }
        }
    }
}
