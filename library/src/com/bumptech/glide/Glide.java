package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import com.android.volley.RequestQueue;
import com.bumptech.glide.loader.GlideUrl;
import com.bumptech.glide.loader.bitmap.model.GenericLoaderFactory;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.model.ModelLoaderFactory;
import com.bumptech.glide.loader.bitmap.model.file_descriptor.FileDescriptorFileLoader;
import com.bumptech.glide.loader.bitmap.model.file_descriptor.FileDescriptorModelLoader;
import com.bumptech.glide.loader.bitmap.model.file_descriptor.FileDescriptorResourceLoader;
import com.bumptech.glide.loader.bitmap.model.file_descriptor.FileDescriptorStringLoader;
import com.bumptech.glide.loader.bitmap.model.file_descriptor.FileDescriptorUriLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamByteArrayLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamFileLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamModelLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamResourceLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamStringLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamUriLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamUrlLoader;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import com.bumptech.glide.resize.cache.DiskCache;
import com.bumptech.glide.resize.cache.MemoryCache;
import com.bumptech.glide.resize.request.Request;
import com.bumptech.glide.resize.target.Target;
import com.bumptech.glide.resize.target.ViewTarget;
import com.bumptech.glide.volley.VolleyUrlLoader;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

/**
 * A singleton to present a simple static interface for building requests with {@link RequestBuilder} and maintaining
 * an {@link ImageManager} and it's {@link BitmapPool}, {@link DiskCache} and {@link MemoryCache}.
 *
 * <p>
 * Note - This class is not thread safe.
 * </p>
 */
public class Glide {
    private static final String TAG = "Glide";
    private static Glide GLIDE;
    private final GenericLoaderFactory loaderFactory = new GenericLoaderFactory();
    private final RequestQueue requestQueue;
    private final ImageManager imageManager;

    /**
     * Get the singleton.
     *
     * @return the singleton
     */
    public static Glide get(Context context) {
        if (GLIDE == null) {
            GLIDE = new GlideBuilder(context).createGlide();
        }

        return GLIDE;
    }

    /**
     * Returns false if the {@link Glide} singleton has not yet been created and can therefore be setup using
     * {@link #setup(GlideBuilder)}.
     *
     * @see #setup(GlideBuilder)
     */
    public static boolean isSetup() {
        return GLIDE != null;
    }

    /**
     * Creates the {@link Glide} singleton using the given builder. Can be used to set options like cache sizes and
     * locations.
     *
     * @see #isSetup()
     *
     * @param builder The builder.
     * @throws IllegalArgumentException if the Glide singleton has already been created.
     */
    public static void setup(GlideBuilder builder) {
        if (GLIDE != null) {
            throw new IllegalArgumentException("Glide is already setup, check with isSetup() first");
        }

        GLIDE = builder.createGlide();
    }

    Glide(ImageManager imageManager, RequestQueue requestQueue) {
        this.imageManager = imageManager;
        this.requestQueue = requestQueue;
        register(File.class, ParcelFileDescriptor.class, new FileDescriptorFileLoader.Factory());
        register(File.class, InputStream.class, new StreamFileLoader.Factory());
        register(Integer.class, ParcelFileDescriptor.class, new FileDescriptorResourceLoader.Factory());
        register(Integer.class, InputStream.class, new StreamResourceLoader.Factory());
        register(String.class, ParcelFileDescriptor.class, new FileDescriptorStringLoader.Factory());
        register(String.class, InputStream.class, new StreamStringLoader.Factory());
        register(Uri.class, ParcelFileDescriptor.class, new FileDescriptorUriLoader.Factory());
        register(Uri.class, InputStream.class, new StreamUriLoader.Factory());
        register(URL.class, InputStream.class, new StreamUrlLoader.Factory());
        register(GlideUrl.class, InputStream.class, new VolleyUrlLoader.Factory(requestQueue));

    }

    /**
     * Returns the {@link ImageManager} Glide is using to load images.
     */
    public ImageManager getImageManager() {
        return imageManager;
    }

    /**
     * Returns the {@link RequestQueue} Glide is using to fetch images over http/https.
     */
    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

    /**
     * Cancel any pending loads Glide may have for the target and free any resources (such as {@link Bitmap}s) that may
     * have been loaded for the target so they may be reused.
     *
     * @param target The Target to cancel loads for.
     */
    public static void clear(Target target) {
        Request request = target.getRequest();
        if (request!= null) {
            request.clear();
        }
    }

    /**
     * Cancel any pending loads Glide may have for the view and free any resources that may have been loaded for the
     * view.
     *
     * <p>
     *     Note that this will only work if {@link View#setTag(Object)} is not called on this view outside of Glide.
     * </p>
     *
     * @see #clear(Target).
     *
     * @param view The view to cancel loads and free resources for.
     * @throws IllegalArgumentException if an object other than Glide's metadata is set as the view's tag.
     */
    public static void clear(View view) {
        Target viewTarget = new ClearTarget(view);
        clear(viewTarget);
    }

    /**
     * Use the given factory to build a {@link ModelLoader} for models of the given class. Generally the best use of
     * this method is to replace one of the default factories or add an implementation for other similar low level
     * models. Typically the {@link ModelRequest#using(StreamModelLoader)} or
     * {@link ModelRequest#using(FileDescriptorModelLoader)} syntax is preferred because it directly links the model
     * with the ModelLoader being used to load it. Any factory replaced by the given factory will have its
     * {@link ModelLoaderFactory#teardown()}} method called.
     *
     * <p>
     *     Note - If a factory already exists for the given class, it will be replaced. If that factory is not being
     *     used for any other model class, {@link ModelLoaderFactory#teardown()}
     *     will be called.
     * </p>
     *
     * <p>
     *     Note - The factory must not be an anonymous inner class of an Activity or another object that cannot be
     *     retained statically.
     * </p>
     *
     * @see ModelRequest#using(FileDescriptorModelLoader)
     * @see ModelRequest#using(StreamModelLoader)
     *
     * @param modelClass The model class.
     * @param resourceClass The resource class the model loader will translate the model type into.
     * @param factory The factory to use.
     * @param <T> The type of the model.
     * @param <Y> the type of the resource.
     */
    public <T, Y> void register(Class<T> modelClass, Class<Y> resourceClass, ModelLoaderFactory<T, Y> factory) {
        ModelLoaderFactory<T, Y> removed = loaderFactory.register(modelClass, resourceClass, factory);
        if (removed != null) {
            removed.teardown();
        }
    }

    /**
     * Removes any {@link ModelLoaderFactory} registered for the given model and resource classes if one exists. If a
     * {@link ModelLoaderFactory} is removed, its {@link ModelLoaderFactory#teardown()}} method will be called.
     *
     * @param modelClass The model class.
     * @param resourceClass The resource class.
     * @param <T> The type of the model.
     * @param <Y> The type of the resource.
     */
    public <T, Y> void unregister(Class<T> modelClass, Class<Y> resourceClass) {
        ModelLoaderFactory<T, Y> removed = loaderFactory.unregister(modelClass, resourceClass);
        if (removed != null) {
            removed.teardown();
        }
    }

    /**
     * Build a {@link ModelLoader} for the given model class using registered {@link ModelLoaderFactory}s.
     *
     * @see  #buildModelLoader(Object, Class, Context)
     * @see  #buildStreamModelLoader(Class, Context)
     * @see  #buildFileDescriptorModelLoader(Class, Context)
     *
     * @param modelClass The class to get a {@link ModelLoader} for.
     * @param resourceClass The resource class to get a {@link ModelLoader} for.
     * @param context Any context.
     * @param <T> The type of the model.
     * @param <Y> The type of the resource.
     * @return A new {@link ModelLoader} for the given model class.
     */
    public static <T, Y> ModelLoader<T, Y> buildModelLoader(Class<T> modelClass, Class<Y> resourceClass,
            Context context) {
        return Glide.get(context).getLoaderFactory().buildModelLoader(modelClass, resourceClass, context);
    }

    /**
     * A convenience method to build a {@link ModelLoader} for a given model object using registered
     * {@link ModelLoaderFactory}s.
     *
     * @see #buildModelLoader(Class, Class, Context)
     *
     * @param model A non null model object whose class we will get a {@link ModelLoader} for.
     * @param resourceClass The resource class to get a {@link ModelLoader} for.
     * @param context Any context.
     * @param <T> The type of the model.
     * @param <Y> The type of the resource.
     * @return A new {@link ModelLoader} for the given model and resource classes, or null if model is null.
     */
    @SuppressWarnings("unchecked")
    public static <T, Y> ModelLoader<T, Y> buildModelLoader(T model, Class<Y> resourceClass, Context context) {
        if (model == null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Unable to load null model, setting placeholder only");
            }
            return null;
        }
        return buildModelLoader((Class<T>) model.getClass(), resourceClass, context);
    }

    /**
     * A method to build a {@link ModelLoader} for the given model that produces {@link InputStream}s using a registered
     * factory.
     *
     * @see #buildModelLoader(Class, Class, android.content.Context)
     */
    public static <T> ModelLoader<T, InputStream> buildStreamModelLoader(Class<T> modelClass, Context context) {
        return buildModelLoader(modelClass, InputStream.class, context);
    }

    /**
     * A method to build a {@link ModelLoader} for the given model that produces {@link InputStream}s using a registered
     * factory.
     *
     * @see #buildModelLoader(Object, Class, Context)
     */
    public static <T> ModelLoader<T, InputStream> buildStreamModelLoader(T model, Context context) {
        return buildModelLoader(model, InputStream.class, context);
    }

    /**
     * A method to build a {@link ModelLoader} for the given model class that produces
     * {@link ParcelFileDescriptor}s using a registered factory.
     *
     * @see #buildModelLoader(Class, Class, android.content.Context)
     */
    public static <T> ModelLoader<T, ParcelFileDescriptor> buildFileDescriptorModelLoader(Class<T> modelClass,
            Context context) {
        return buildModelLoader(modelClass, ParcelFileDescriptor.class, context);
    }

    /**
     * A method to build a {@link ModelLoader} for the given model class that produces
     * {@link ParcelFileDescriptor}s using a registered factory.
     *
     * @see #buildModelLoader(Object, Class, android.content.Context)
     */
    public static <T> ModelLoader<T, ParcelFileDescriptor> buildFileDescriptorModelLoader(T model, Context context) {
        return buildModelLoader(model, ParcelFileDescriptor.class, context);
    }

    /**
     * Begin a load with Glide by passing in a context.
     *
     * @param context Any context, will not be retained.
     * @return A model request to pass in the object representing the image to be loaded.
     */
    public static ModelRequest with(Context context) {
        return new ModelRequest(context);
    }

    private GenericLoaderFactory getLoaderFactory() {
        return loaderFactory;
    }

    /**
     * A {@link RequestBuilder} builder that returns a request for a model that represents an image.
     */
    public static class ModelRequest {
        private final Context context;

        private ModelRequest(Context context) {
            this.context = context;
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
            return new ImageModelRequest<T>(context, modelLoader);
        }

        /**
         * A convenience method to use a {@link StreamByteArrayLoader} to decode an image from a byte array.
         *
         * @param modelLoader The byte array loader.
         * @return A new {@link ImageModelRequest}.
         */
        public ImageModelRequest<byte[]> using(StreamByteArrayLoader modelLoader) {
            return new ImageModelRequest<byte[]>(context, modelLoader);
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
            return new VideoModelRequest<T>(context, modelLoader);
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
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the model
         * into.
         */
        public RequestBuilder<String> load(String string) {
            return new RequestBuilder<String>(context, string);
        }

        /**
         * Use the {@link ModelLoaderFactory} currently registered for {@link Uri} to load the image at the given uri.
         * Defaults to {@link StreamUriLoader.Factory} and {@link StreamUriLoader}.
         *
         * @see #using(StreamModelLoader)
         *
         * @param uri The uri representing the image. Must be a uri handled by {@link StreamUriLoader}
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the model
         * into.
         */
        public RequestBuilder<Uri> load(Uri uri) {
            return new RequestBuilder<Uri>(context, uri);
        }

        /**
         * Use the {@link ModelLoaderFactory} currently registered for {@link File} to load the image represented by the
         * given {@link File}. Defaults to {@link StreamFileLoader.Factory} and {@link StreamFileLoader} to load the
         * given model.
         *
         * @see #using(StreamModelLoader)
         *
         * @param file The File containing the image
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the model
         * into.
         */
        public RequestBuilder<File> load(File file) {
            return new RequestBuilder<File>(context, file);
        }

        /**
         * Use the {@link ModelLoaderFactory} currently registered for {@link Integer} to load the image represented by
         * the given {@link Integer} resource id. Defaults to {@link StreamResourceLoader.Factory} and
         * {@link StreamResourceLoader} to load the given model.
         *
         * @see #using(StreamModelLoader)
         *
         * @param resourceId the id of the resource containing the image
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the model
         * into.
         */
        public RequestBuilder<Integer> load(Integer resourceId) {
            return new RequestBuilder<Integer>(context, resourceId);
        }

        /**
         * Use the {@link ModelLoaderFactory} currently registered for the given model type to load the image
         * represented by the given model.
         *
         * @param model The model to load.
         * @param <T> The type of the model to load.
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the image
         * into.
         */
        @SuppressWarnings("unused")
        public <T> RequestBuilder<T> loadFromImage(T model) {
            return new RequestBuilder<T>(context, model, buildStreamModelLoader(model, context), null);
        }

        /**
         * Use the {@link ModelLoaderFactory} currently registered for {@link URL} to load the image represented by the
         * given {@link URL}. Defaults to {@link VolleyUrlLoader.Factory} and {@link VolleyUrlLoader} to load the given
         * model.
         *
         * @see #using(StreamModelLoader)
         *
         * @param url The URL representing the image.
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the model
         * into.
         */
        public RequestBuilder<URL> loadFromImage(URL url) {
            return new RequestBuilder<URL>(context, url, buildStreamModelLoader(url, context), null);
        }

        /**
         * Use a new {@link StreamByteArrayLoader} to load an image from the given model.
         *
         * @see #loadFromImage(byte[])
         *
         * @param model The data to load.
         * @param id A unique id that identifies the image represented by the model suitable for use as a cache key
         *           (url, filepath etc). If there is no suitable id, use {@link #loadFromImage(byte[])} instaed.
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the image
         * into.
         */
        public RequestBuilder<byte[]> loadFromImage(byte[] model, final String id) {
            return new RequestBuilder<byte[]>(context, model, new StreamByteArrayLoader() {
                @Override
                public String getId(byte[] model) {
                    return id;
                }
            }, null);
        }

        /**
         * Use a new {@link StreamByteArrayLoader} to load an image from the given model. Suitable when there is no
         * simple id that represents the given data.
         *
         * @param model the data to load.
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the image
         * into.
         */
        public RequestBuilder<byte[]> loadFromImage(byte[] model) {
            return loadFromImage(model, UUID.randomUUID()
                    .toString());
        }

        /**
         * Use the {@link ModelLoaderFactory} currently registered for the given model type for
         * {@link ParcelFileDescriptor}s to load a thumbnail for the video represented by the given model.
         *
         * @param model The model to load.
         * @param <T> The type of the model to load.
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the image
         * into.
         */
        @SuppressWarnings("unused")
        public <T> RequestBuilder<T> loadFromVideo(T model) {
            return new RequestBuilder<T>(context, model, null, buildFileDescriptorModelLoader(model, context));
        }

        /**
         * Use the {@link ModelLoaderFactory}s currently registered for the given model type for
         * {@link InputStream}s and {@link ParcelFileDescriptor}s to load a thumbnail from either the image or the video
         * represented by the given model.
         *
         * @param model The model the load.
         * @param <T> The type of the model to load.
         * @return A {@link RequestBuilder} to set options for the load and ultimately the target to load the image
         * into.
         */
        public <T> RequestBuilder<T> load(T model) {
            return new RequestBuilder<T>(context, model);
        }
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

        private VideoModelRequest(Context context, ModelLoader<T, ParcelFileDescriptor> loader) {
            this.context = context;
            this.loader = loader;
        }

        public RequestBuilder<T> loadFromVideo(T model) {
            return new RequestBuilder<T>(context, model, null, loader);
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

        private ImageModelRequest(Context context, ModelLoader<T, InputStream> loader) {
            this.context = context;
            this.loader = loader;
        }

        public RequestBuilder<T> load(T model) {
            return new RequestBuilder<T>(context, model, loader, null);
        }
    }

    private static class ClearTarget extends ViewTarget<View> {
        public ClearTarget(View view) {
            super(view);
        }

        @Override
        public void onImageReady(Bitmap bitmap) { }

        @Override
        public void setPlaceholder(Drawable placeholder) { }
    }
}
