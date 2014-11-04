package com.bumptech.glide;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.file_descriptor.FileDescriptorModelLoader;
import com.bumptech.glide.load.model.stream.MediaStoreStreamLoader;
import com.bumptech.glide.load.model.stream.StreamByteArrayLoader;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.bumptech.glide.manager.ConnectivityMonitor;
import com.bumptech.glide.manager.ConnectivityMonitorFactory;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.LifecycleListener;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.signature.ApplicationVersionSignature;
import com.bumptech.glide.signature.MediaStoreSignature;
import com.bumptech.glide.signature.StringSignature;
import com.bumptech.glide.util.Util;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

/**
 * A class for managing and starting requests for Glide. Can use activity, fragment and connectivity lifecycle events to
 * intelligently stop, start, and restart requests. Retrieve either by instantiating a new object, or to take advantage
 * built in Activity and Fragment lifecycle handling, use the static Glide.load methods with your Fragment or Activity.
 *
 * @see Glide#with(android.app.Activity)
 * @see Glide#with(android.support.v4.app.FragmentActivity)
 * @see Glide#with(android.app.Fragment)
 * @see Glide#with(android.support.v4.app.Fragment)
 * @see Glide#with(Context)
 */
public class RequestManager implements LifecycleListener {
    private final Context context;
    private final Lifecycle lifecycle;
    private final RequestTracker requestTracker;
    private final Glide glide;
    private final OptionsApplier optionsApplier;
    private DefaultOptions options;

    public RequestManager(Context context, Lifecycle lifecycle) {
        this(context, lifecycle, new RequestTracker(), new ConnectivityMonitorFactory());
    }

    RequestManager(Context context, final Lifecycle lifecycle, RequestTracker requestTracker,
            ConnectivityMonitorFactory factory) {
        this.context = context.getApplicationContext();
        this.lifecycle = lifecycle;
        this.requestTracker = requestTracker;
        this.glide = Glide.get(context);
        this.optionsApplier = new OptionsApplier();

        ConnectivityMonitor connectivityMonitor = factory.build(context,
                new RequestManagerConnectivityListener(requestTracker));

        // If we're the application level request manager, we may be created on a background thread. In that case we
        // cannot risk synchronously pausing or resuming requests, so we hack around the issue by delaying adding
        // ourselves as a lifecycle listener by posting to the main thread. This should be entirely safe.
        if (Util.isOnBackgroundThread()) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    lifecycle.addListener(RequestManager.this);
                }
            });
        } else {
            lifecycle.addListener(this);
        }
        lifecycle.addListener(connectivityMonitor);
    }

    /**
     * An interface that allows a default set of options to be applied to all requests started from an
     * {@link com.bumptech.glide.RequestManager}.
     */
    public interface DefaultOptions {
        /**
         * Allows the implementor to apply some options to the given request.
         *
         * @param requestBuilder The request builder being used to construct the load.
         * @param <T> The type of the model.
         */
        <T> void apply(GenericRequestBuilder<T, ?, ?, ?> requestBuilder);
    }

    /**
     * Sets an interface that can apply some default options to all Requests started using this {@link RequestManager}.
     *
     * <p>
     *     Note - These options will be retained for the life the of this {@link com.bumptech.glide.RequestManager}
     *     so be wary of using
     *     {@link com.bumptech.glide.GenericRequestBuilder#listener(com.bumptech.glide.request.RequestListener)}} when
     *     starting requests using an {@link android.content.Context} or {@link android.app.Application} to avoid
     *     leaking memory. Any option that does not use an anonymous inner class is generally safe.
     * </p>
     *
     * @param options The default options to apply to all requests.
     */
    public void setDefaultOptions(DefaultOptions options) {
        this.options = options;
    }

    /**
     * Returns true if loads for this {@link RequestManager} are currently paused.
     *
     * @see #pauseRequests()
     * @see #resumeRequests()
     */
    public boolean isPaused() {
        Util.assertMainThread();
        return requestTracker.isPaused();
    }

    /**
     * Cancels any in progress loads, but does not clear resources of completed loads.
     *
     * @see #isPaused()
     * @see #resumeRequests()
     */
    public void pauseRequests() {
        Util.assertMainThread();
        requestTracker.pauseRequests();
    }

    /**
     * Restarts any loads that have not yet completed.
     *
     * @see #isPaused()
     * @see #pauseRequests()
     */
    public void resumeRequests() {
        Util.assertMainThread();
        requestTracker.resumeRequests();
    }

    /**
     * Lifecycle callback that registers for connectivity events (if the android.permission.ACCESS_NETWORK_STATE
     * permission is present) and restarts failed or paused requests.
     */
    @Override
    public void onStart() {
        // onStart might not be called because this object may be created after the fragment/activity's onStart method.
        resumeRequests();
    }

    /**
     * Lifecycle callback that unregisters for connectivity events (if the android.permission.ACCESS_NETWORK_STATE
     * permission is present) and pauses in progress loads.
     */
    @Override
    public void onStop() {
        pauseRequests();
    }

    /**
     * Lifecycle callback that cancels all in progress requests and clears and recycles resources for all completed
     * requests.
     */
    @Override
    public void onDestroy() {
        requestTracker.clearRequests();
    }

    /**
     * Returns a request builder that uses the given {@link com.bumptech.glide.load.model.ModelLoader} to fetch a
     * generic data type.
     *
     * <p>
     *     Warning - This is an experimental api that may change without a change in major version.
     * </p>
     *
     * @param modelLoader The {@link ModelLoader} class to use to load the model.
     * @param dataClass The type of data the {@link ModelLoader} will load.
     * @param <A> The type of the model to be loaded.
     * @param <T> The type of the data to be loaded from the mode.
     */
    public <A, T> GenericModelRequest<A, T> using(ModelLoader<A, T> modelLoader, Class<T> dataClass) {
        return new GenericModelRequest<A, T>(modelLoader, dataClass);
    }

    /**
     * Returns a request builder that uses the given {@link com.bumptech.glide.load.model.stream.StreamModelLoader} to
     * fetch an {@link InputStream} for loading images.
     *
     * @param modelLoader The model loader to use.
     * @param <T> The type of the model.
     */
    public <T> ImageModelRequest<T> using(final StreamModelLoader<T> modelLoader) {
        return new ImageModelRequest<T>(modelLoader);
    }

    /**
     * Returns a request builder that uses the given
     * {@link com.bumptech.glide.load.model.stream.StreamByteArrayLoader} to fetch an {@link java.io.InputStream} for
     * loading Bitmaps.
     *
     * @param modelLoader The byte array loader.
     */
    public ImageModelRequest<byte[]> using(StreamByteArrayLoader modelLoader) {
        return new ImageModelRequest<byte[]>(modelLoader);
    }

    /**
     * Returns a new request builder that uses the given {@link ModelLoader} to fetch a
     * {@link ParcelFileDescriptor} for loading video thumbnails.
     *
     * @param modelLoader The model loader to use.
     * @param <T> The type of the model.
     */
    public <T> VideoModelRequest<T> using(final FileDescriptorModelLoader<T> modelLoader) {
        return new VideoModelRequest<T>(modelLoader);
    }

    /**
     * Returns a request builder to load the given {@link java.lang.String}.
     * signature.
     *
     * @see #fromString()
     * @see #load(Object)
     *
     * @param string A file path, or a uri or url handled by {@link com.bumptech.glide.load.model.UriLoader}.
     */
    public DrawableTypeRequest<String> load(String string) {
        return (DrawableTypeRequest<String>) fromString().load(string);
    }

    /**
     * Returns a request builder that loads data from {@link String}s using an empty signature.
     *
     * <p>
     *     Note - this method caches data using only the given String as the cache key. If the data is a Uri outside of
     *     your control, or you otherwise expect the data represented by the given String to change without the String
     *     identifier changing, Consider using
     *     {@link com.bumptech.glide.GenericRequestBuilder#signature(com.bumptech.glide.load.Key)} to mixin a signature
     *     you create that identifies the data currently at the given String that will invalidate the cache if that data
     *     changes. Alternatively, using {@link com.bumptech.glide.load.engine.DiskCacheStrategy#NONE} and/or
     *     {@link com.bumptech.glide.DrawableRequestBuilder#skipMemoryCache(boolean)} may be appropriate.
     * </p>
     *
     * @see #from(Class)
     * @see #load(String)
     */
    public DrawableTypeRequest<String> fromString() {
        return loadGeneric(String.class);
    }

    /**
     * Returns a request builder to load the given {@link Uri}.
     *
     * @see #fromUri()
     * @see #load(Object)
     *
     * @param uri The Uri representing the image. Must be of a type handled by
     * {@link com.bumptech.glide.load.model.UriLoader}.
     */
    public DrawableTypeRequest<Uri> load(Uri uri) {
        return (DrawableTypeRequest<Uri>) fromUri().load(uri);
    }

    /**
     * Returns a request builder to load data from {@link android.net.Uri}s using no signature.
     *
     * <p>
     *     Note - this method caches data at Uris using only the Uri itself as the cache key. The data represented by
     *     Uris from some content providers may change without the Uri changing, which means using this method
     *     can lead to displaying stale data. Consider using
     *     {@link com.bumptech.glide.GenericRequestBuilder#signature(com.bumptech.glide.load.Key)} to mixin a signature
     *     you create based on the data at the given Uri that will invalidate the cache if that data changes.
     *     Alternatively, using {@link com.bumptech.glide.load.engine.DiskCacheStrategy#NONE} and/or
     *     {@link com.bumptech.glide.DrawableRequestBuilder#skipMemoryCache(boolean)} may be appropriate.
     * </p>
     *
     * @see #from(Class)
     * @see #loadFromMediaStore(android.net.Uri)
     * @see #loadFromMediaStore(android.net.Uri, String, long, int)
     * @see com.bumptech.glide.GenericRequestBuilder#signature(com.bumptech.glide.load.Key)
     */
    public DrawableTypeRequest<Uri> fromUri() {
        return loadGeneric(Uri.class);
    }

    /**
     * Returns a request builder that uses {@link android.provider.MediaStore.Images.Thumbnails} and
     * {@link android.provider.MediaStore.Video.Thumbnails} to retrieve pre-generated thumbnails for the given uri if
     * available and uses the given additional data to build a unique signature for cache invalidation.
     *
     * @see #loadFromMediaStore(android.net.Uri)
     * @see #load(android.net.Uri)
     * @see com.bumptech.glide.GenericRequestBuilder#signature(com.bumptech.glide.load.Key)
     * @see com.bumptech.glide.signature.MediaStoreSignature
     *
     * @deprecated Use {@link #loadFromMediaStore(android.net.Uri)},
     * {@link com.bumptech.glide.signature.MediaStoreSignature}, and
     * {@link com.bumptech.glide.DrawableRequestBuilder#signature(com.bumptech.glide.load.Key)} instead. Scheduled to be
     * removed in Glide 4.0.
     * @param uri The uri representing the media.
     * @param mimeType The mime type of the media store media. Ok to default to empty string "". See
     *      {@link android.provider.MediaStore.Images.ImageColumns#MIME_TYPE} or
     *      {@link android.provider.MediaStore.Video.VideoColumns#MIME_TYPE}.
     * @param dateModified The date modified time of the media store media. Ok to default to 0. See
     *      {@link android.provider.MediaStore.Images.ImageColumns#DATE_MODIFIED} or
     *      {@link android.provider.MediaStore.Video.VideoColumns#DATE_MODIFIED}.
     * @param orientation The orientation of the media store media. Ok to default to 0. See
     *      {@link android.provider.MediaStore.Images.ImageColumns#ORIENTATION}.
     */
    @Deprecated
    public DrawableTypeRequest<Uri> loadFromMediaStore(Uri uri, String mimeType, long dateModified, int orientation) {
        Key signature = new MediaStoreSignature(mimeType, dateModified, orientation);
        return (DrawableTypeRequest<Uri>) loadFromMediaStore(uri).signature(signature);
    }

    /**
     * Returns a request builder to load the given media store {@link android.net.Uri}.
     *
     * @see #fromMediaStore()
     * @see #load(Object)
     *
     * @param uri The uri representing the media.
     */
    public DrawableTypeRequest<Uri> loadFromMediaStore(Uri uri) {
        return (DrawableTypeRequest<Uri>) fromMediaStore().load(uri);
    }

    /**
     * Returns a request builder that uses {@link android.provider.MediaStore.Images.Thumbnails} and
     * {@link android.provider.MediaStore.Video.Thumbnails} to retrieve pre-generated thumbnails for
     * {@link android.net.Uri}s.
     *
     * <p>
     *  Falls back to the registered {@link com.bumptech.glide.load.model.ModelLoaderFactory} registered for
     *  {@link Uri}s if the given uri is not a media store uri or if no pre-generated thumbnail exists for the given
     *  uri.
     * </p>
     *
     * <p>
     *     Note - This method by default caches data using the given Uri as the key. Since content in the media store
     *     can change at any time, you should use
     *     {@link com.bumptech.glide.GenericRequestBuilder#signature(com.bumptech.glide.load.Key)} to mix in some
     *     additional data identifying the current state of the Uri, preferably using
     *     {@link com.bumptech.glide.signature.MediaStoreSignature}. Alternatively consider avoiding the memory and
     *     disk caches entirely using
     *     {@link GenericRequestBuilder#diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy)}
     *     and {@link com.bumptech.glide.load.engine.DiskCacheStrategy#NONE} and/or
     *     {@link com.bumptech.glide.GenericRequestBuilder#skipMemoryCache(boolean)}.
     * </p>
     *
     * @see #from(Class)
     * @see #loadFromMediaStore(android.net.Uri, String, long, int)
     * @see #load(android.net.Uri)
     * @see com.bumptech.glide.signature.MediaStoreSignature
     */
    public DrawableTypeRequest<Uri> fromMediaStore() {
        ModelLoader<Uri, InputStream> genericStreamLoader = Glide.buildStreamModelLoader(Uri.class, context);
        ModelLoader<Uri, InputStream> mediaStoreLoader = new MediaStoreStreamLoader(context, genericStreamLoader);
        ModelLoader<Uri, ParcelFileDescriptor> fileDescriptorModelLoader =
                Glide.buildFileDescriptorModelLoader(Uri.class, context);

        return optionsApplier.apply(new DrawableTypeRequest<Uri>(Uri.class, mediaStoreLoader,
                fileDescriptorModelLoader, context, glide, requestTracker, lifecycle, optionsApplier));
    }

    /**
     * Returns a request builder to load the given {@link File}.
     *
     * @see #fromFile()
     * @see #load(Object)
     *
     * @param file The File containing the image
     */
    public DrawableTypeRequest<File> load(File file) {
        return (DrawableTypeRequest<File>) fromFile().load(file);
    }

    /**
     * Returns a request builder that uses the {@link com.bumptech.glide.load.model.ModelLoaderFactory} currently
     * registered for {@link File} to load the image represented by the given {@link File}. Defaults to
     * {@link com.bumptech.glide.load.model.stream.StreamFileLoader.Factory} and
     * {@link com.bumptech.glide.load.model.stream.StreamFileLoader} to load images from {@link File}s.
     *
     *  <p>
     *     Note - this method caches data for Files using only the file path itself as the cache key. The data in the
     *     File can change so using this method can lead to displaying stale data. If you expect the data in the File to
     *     change, Consider using
     *     {@link com.bumptech.glide.GenericRequestBuilder#signature(com.bumptech.glide.load.Key)} to mixin a signature
     *     you create that identifies the data currently in the File that will invalidate the cache if that data
     *     changes. Alternatively, using {@link com.bumptech.glide.load.engine.DiskCacheStrategy#NONE} and/or
     *     {@link com.bumptech.glide.DrawableRequestBuilder#skipMemoryCache(boolean)} may be appropriate.
     * </p>
     *
     * @see #load(java.io.File)
     * @see #from(Class)
     */
    public DrawableTypeRequest<File> fromFile() {
        return loadGeneric(File.class);
    }

    /**
     * Returns a request builder to load the given resource id.
     *
     * @see #fromResource()
     * @see #load(Object)
     *
     * @param resourceId the id of the resource containing the image
     */
    public DrawableTypeRequest<Integer> load(Integer resourceId) {
        return (DrawableTypeRequest<Integer>) fromResource().load(resourceId);
    }

    /**
     * Returns a request builder that uses the {@link com.bumptech.glide.load.model.ModelLoaderFactory} currently
     * registered for {@link Integer} to load the image represented by the given {@link Integer} resource id. Defaults
     * to {@link com.bumptech.glide.load.model.stream.StreamResourceLoader.Factory} and
     * {@link com.bumptech.glide.load.model.stream.StreamResourceLoader} to load resource id models.
     *
     * <p>
     *     By default this method adds a version code based signature to the cache key used to cache this resource in
     *     Glide. This signature is sufficient to guarantee that end users will see the most up to date versions of
     *     your Drawables, but during development if you do not increment your version code before each install and
     *     you replace a Drawable with different data without changing the Drawable name, you may see inconsistent
     *     cached data. To get around this, consider using {@link com.bumptech.glide.load.engine.DiskCacheStrategy#NONE}
     *     via {@link GenericRequestBuilder#diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy)}
     *     during development, and re-enabling the default
     *     {@link com.bumptech.glide.load.engine.DiskCacheStrategy#RESULT} for release builds.
     * </p>
     *
     * @see #from(Class)
     * @see #load(Integer)
     * @see com.bumptech.glide.signature.ApplicationVersionSignature
     * @see com.bumptech.glide.GenericRequestBuilder#signature(com.bumptech.glide.load.Key)
     */
    public DrawableTypeRequest<Integer> fromResource() {
        return (DrawableTypeRequest<Integer>) loadGeneric(Integer.class)
                .signature(ApplicationVersionSignature.obtain(context));
    }

    /**
     * Returns a request builder to load the given {@link URL}.
     *
     * @see #fromUrl()
     * @see #load(Object)
     *
     * @deprecated The {@link java.net.URL} class has
     * <a href="http://goo.gl/c4hHNu">a number of performance problems</a> and should generally be avoided when
     * possible. Prefer {@link #load(android.net.Uri)} or {@link #load(String)}.
     * @param url The URL representing the image.
     */
    @Deprecated
    public DrawableTypeRequest<URL> load(URL url) {
        return (DrawableTypeRequest<URL>) fromUrl().load(url);
    }

    /**
     * Returns a request builder that uses the {@link com.bumptech.glide.load.model.ModelLoaderFactory} currently
     * registered for {@link URL} to load the image represented by the given {@link URL}. Defaults to
     * {@link com.bumptech.glide.load.model.stream.HttpUrlGlideUrlLoader} and
     * {@link com.bumptech.glide.load.data.HttpUrlFetcher} to load {@link java.net.URL} models.
     *
     * @see #from(Class)
     * @see #load(java.net.URL)
     *
     * @deprecated The {@link java.net.URL} class has
     * <a href="http://goo.gl/c4hHNu">a number of performance problems</a> and should generally be avoided when
     * possible. Prefer {@link #load(android.net.Uri)} or {@link #load(String)}.
     */
    @Deprecated
    public DrawableTypeRequest<URL> fromUrl() {
        return loadGeneric(URL.class);
    }

    /**
     * Returns a request builder that uses a {@link StreamByteArrayLoader} to load an image from the given byte array.
     *
     *
     * <p>
     *     Note - by default loads for bytes are not cached in either the memory or the disk cache.
     * </p>
     *
     * @see #load(byte[])
     *
     * @deprecated Use {@link #load(byte[])} along with
     * {@link com.bumptech.glide.GenericRequestBuilder#signature(com.bumptech.glide.load.Key)} instead. Scheduled to be
     * removed in Glide 4.0.
     * @param model The data to load.
     * @param id A unique id that identifies the image represented by the model suitable for use as a cache key
     *           (url, filepath etc). If there is no suitable id, use {@link #load(byte[])} instead.
     */
    @Deprecated
    public DrawableTypeRequest<byte[]> load(byte[] model, final String id) {
        return (DrawableTypeRequest<byte[]>) load(model).signature(new StringSignature(id));
    }

    /**
     * Returns a request to load the given byte array.
     *
     * @see #fromBytes()
     * @see #load(Object)
     *
     * @param model the data to load.
     */
    public DrawableTypeRequest<byte[]> load(byte[] model) {
        return (DrawableTypeRequest<byte[]>) fromBytes().load(model);
    }

    /**
     * Returns a request builder that uses {@link com.bumptech.glide.load.model.stream.StreamByteArrayLoader} to load
     * images from byte arrays.
     *
     * <p>
     *     Note - by default loads for bytes are not cached in either the memory or the disk cache.
     * </p>
     *
     * @see #from(Class)
     * @see #load(byte[])
     */
    public DrawableTypeRequest<byte[]> fromBytes() {
        return (DrawableTypeRequest<byte[]>) loadGeneric(byte[].class)
                .signature(new StringSignature(UUID.randomUUID().toString()))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true /*skipMemoryCache*/);
    }

    /**
     * Returns a request builder that uses the {@link com.bumptech.glide.load.model.ModelLoaderFactory}s currently
     * registered for the given model class for {@link InputStream}s and {@link ParcelFileDescriptor}s to load a
     * thumbnail from either the image or the video represented by the given model.
     *
     * <p>
     *     Note - for maximum efficiency, consider using {@link #from(Class)}} to avoid repeatedly allocating builder
     *     objects.
     * </p>
     *
     * @see #from(Class)
     *
     * @param model The model the load.
     * @param <T> The type of the model to load.
     */
    public <T> DrawableTypeRequest<T> load(T model) {
        return (DrawableTypeRequest<T>) loadGeneric(getSafeClass(model)).load(model);
    }

    /**
     * Returns a request builder that can be used for multiple loads that uses the
     * {@link com.bumptech.glide.load.model.ModelLoaderFactory}s registered for the given model class for
     * {@link java.io.InputStream}s and {@link android.os.ParcelFileDescriptor}s to load a thumbnail from objects of
     * the given modelClass.
     *
     * <p>
     *     Note - you must use {@link com.bumptech.glide.DrawableRequestBuilder#load(Object)}} to set a concrete model
     *     to be loaded before calling
     *     {@link com.bumptech.glide.DrawableRequestBuilder#into(com.bumptech.glide.request.target.Target)}. You may
     *     also use this object for repeated loads by calling <code>request.load(model).into(target)</code>. You may
     *     also adjust the options after calling {@link com.bumptech.glide.DrawableRequestBuilder#load(Object)}} and/or
     *     {@link com.bumptech.glide.DrawableRequestBuilder#into(com.bumptech.glide.request.target.Target)}}. However,
     *     keep in mind that any changes in options will apply to all future loads.
     * </p>
     *
     * @param modelClass The class of model requests built by this class will load data from.
     * @param <T> The type of the model.
     */
    public <T> DrawableTypeRequest<T> from(Class<T> modelClass) {
        return loadGeneric(modelClass);
    }

    private <T> DrawableTypeRequest<T> loadGeneric(Class<T> modelClass) {
        ModelLoader<T, InputStream> streamModelLoader = Glide.buildStreamModelLoader(modelClass, context);
        ModelLoader<T, ParcelFileDescriptor> fileDescriptorModelLoader =
                Glide.buildFileDescriptorModelLoader(modelClass, context);
        if (modelClass != null && streamModelLoader == null && fileDescriptorModelLoader == null) {
            throw new IllegalArgumentException("Unknown type " + modelClass + ". You must provide a Model of a type for"
                    + " which there is a registered ModelLoader, if you are using a custom model, you must first call"
                    + " Glide#register with a ModelLoaderFactory for your custom model class");
        }

        return optionsApplier.apply(
                new DrawableTypeRequest<T>(modelClass, streamModelLoader, fileDescriptorModelLoader, context,
                        glide, requestTracker, lifecycle, optionsApplier));
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getSafeClass(T model) {
        return model != null ? (Class<T>) model.getClass() : null;
    }

    /**
     * A helper class for building requests with custom {@link ModelLoader}s that translate models to
     * {@link ParcelFileDescriptor} resources for loading video thumbnails.
     *
     * @param <T> The type of the model.
     */
    public final class VideoModelRequest<T> {
        private final ModelLoader<T, ParcelFileDescriptor> loader;

        VideoModelRequest(ModelLoader<T, ParcelFileDescriptor> loader) {
            this.loader = loader;
        }

        public DrawableTypeRequest<T> load(T model) {
            return (DrawableTypeRequest<T>) optionsApplier.apply(new DrawableTypeRequest<T>(getSafeClass(model), null,
                    loader, context, glide, requestTracker, lifecycle, optionsApplier))
                    .load(model);
        }
    }

    /**
     * A helper class for building requests with custom {@link ModelLoader}s that translate models to
     * {@link InputStream} resources for loading images.
     *
     * @param <T> The type of the model.
     */
    public final class ImageModelRequest<T> {
        private final ModelLoader<T, InputStream> loader;

        ImageModelRequest(ModelLoader<T, InputStream> loader) {
            this.loader = loader;
        }

        /**
         * Returns a request builder that uses the provided {@link com.bumptech.glide.load.model.ModelLoader} to load
         * images from an {@link java.io.InputStream}s obtained from models of the given model class.
         *
         * @param modelClass The class of model to load images from.
         */
        public DrawableTypeRequest<T> from(Class<T> modelClass) {
            return optionsApplier.apply(new DrawableTypeRequest<T>(modelClass, loader, null, context, glide,
                    requestTracker, lifecycle, optionsApplier));
        }

        /**
         * Returns a request builder that uses the provided {@link com.bumptech.glide.load.model.ModelLoader} to load
         * an image from an {@link java.io.InputStream} obtained from the given model.
         *
         * @see #from(Class)
         *
         * @param model The model to load an image from.
         */
        public DrawableTypeRequest<T> load(T model) {
            return (DrawableTypeRequest<T>) from(getSafeClass(model)).load(model);
        }
    }

    /**
     * A helper class for building requests with custom {@link ModelLoader}s that requires the user to provide a
     * specific model.
     *
     * @param <A> The type of the model.
     * @param <T> The type of data the {@link com.bumptech.glide.load.model.ModelLoader} provides an
     * {@link com.bumptech.glide.load.data.DataFetcher} to convert the model to.
     */
    public final class GenericModelRequest<A, T> {
        private final ModelLoader<A, T> modelLoader;
        private final Class<T> dataClass;

        GenericModelRequest(ModelLoader<A, T> modelLoader, Class<T> dataClass) {
            this.modelLoader = modelLoader;
            this.dataClass = dataClass;
        }

        /**
         * Sets the type of model that will be loaded.
         *
         * @param modelClass the class of model to use.
         * @return A request builder
         */
        public GenericTypeRequest from(Class<A> modelClass) {
            return new GenericTypeRequest(modelClass);
        }

        /**
         * Sets the specific model that will be loaded.
         *
         * @param model The model to use.
         * @return A request builder.
         */
        public GenericTypeRequest load(A model) {
            return new GenericTypeRequest(model);
        }

        /**
         * A helper class for building requests with custom {@link com.bumptech.glide.load.model.ModelLoader}s that
         * requires the user to specify a specific resource class that will be loaded.
         *
         */
        public final class GenericTypeRequest {
            private final A model;
            private final Class<A> modelClass;
            private final boolean providedModel;

            GenericTypeRequest(A model) {
                providedModel = true;
                this.model = model;
                this.modelClass = getSafeClass(model);
            }

            GenericTypeRequest(Class<A> modelClass) {
                providedModel = false;
                this.model = null;
                this.modelClass = modelClass;
            }

            /**
             * Sets the resource class that will be loaded.
             *
             * @param resourceClass The class of the resource that will be loaded.
             * @param <Z> The type of the resource that will be loaded.
             * @return This request builder.
             */
            public <Z> GenericTranscodeRequest<A, T, Z> as(Class<Z> resourceClass) {
                GenericTranscodeRequest<A, T, Z> result =
                        optionsApplier.apply(new GenericTranscodeRequest<A, T, Z>(context, glide, modelClass,
                                modelLoader, dataClass, resourceClass, requestTracker, lifecycle, optionsApplier));
                if (providedModel) {
                    result.load(model);
                }
                return result;
            }
        }
    }

    class OptionsApplier {

        public <A, X extends GenericRequestBuilder<A, ?, ?, ?>> X apply(X builder) {
            if (options != null) {
                options.apply(builder);
            }
            return builder;
        }
    }

    private static class RequestManagerConnectivityListener implements ConnectivityMonitor.ConnectivityListener {
        private final RequestTracker requestTracker;

        public RequestManagerConnectivityListener(RequestTracker requestTracker) {
            this.requestTracker = requestTracker;
        }

        @Override
        public void onConnectivityChanged(boolean isConnected) {
            if (isConnected) {
                requestTracker.restartRequests();
            }
        }
    }
}
