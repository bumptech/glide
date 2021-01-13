package com.bumptech.glide;

import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.MessageQueue.IdleHandler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.bumptech.glide.GlideBuilder.EnableImageDecoderForBitmaps;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.data.InputStreamRewinder;
import com.bumptech.glide.load.data.ParcelFileDescriptorRewinder;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.engine.prefill.BitmapPreFiller;
import com.bumptech.glide.load.engine.prefill.PreFillType;
import com.bumptech.glide.load.engine.prefill.PreFillType.Builder;
import com.bumptech.glide.load.model.AssetUriLoader;
import com.bumptech.glide.load.model.ByteArrayLoader;
import com.bumptech.glide.load.model.ByteBufferEncoder;
import com.bumptech.glide.load.model.ByteBufferFileLoader;
import com.bumptech.glide.load.model.DataUrlLoader;
import com.bumptech.glide.load.model.FileLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.MediaStoreFileLoader;
import com.bumptech.glide.load.model.ResourceLoader;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.model.StringLoader;
import com.bumptech.glide.load.model.UnitModelLoader;
import com.bumptech.glide.load.model.UriLoader;
import com.bumptech.glide.load.model.UrlUriLoader;
import com.bumptech.glide.load.model.stream.HttpGlideUrlLoader;
import com.bumptech.glide.load.model.stream.MediaStoreImageThumbLoader;
import com.bumptech.glide.load.model.stream.MediaStoreVideoThumbLoader;
import com.bumptech.glide.load.model.stream.QMediaStoreUriLoader;
import com.bumptech.glide.load.model.stream.UrlLoader;
import com.bumptech.glide.load.resource.bitmap.BitmapDrawableDecoder;
import com.bumptech.glide.load.resource.bitmap.BitmapDrawableEncoder;
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder;
import com.bumptech.glide.load.resource.bitmap.ByteBufferBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.ByteBufferBitmapImageDecoderResourceDecoder;
import com.bumptech.glide.load.resource.bitmap.DefaultImageHeaderParser;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.load.resource.bitmap.ExifInterfaceImageHeaderParser;
import com.bumptech.glide.load.resource.bitmap.HardwareConfigState;
import com.bumptech.glide.load.resource.bitmap.InputStreamBitmapImageDecoderResourceDecoder;
import com.bumptech.glide.load.resource.bitmap.ParcelFileDescriptorBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.ResourceBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.UnitBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.VideoDecoder;
import com.bumptech.glide.load.resource.bytes.ByteBufferRewinder;
import com.bumptech.glide.load.resource.drawable.ResourceDrawableDecoder;
import com.bumptech.glide.load.resource.drawable.UnitDrawableDecoder;
import com.bumptech.glide.load.resource.file.FileDecoder;
import com.bumptech.glide.load.resource.gif.ByteBufferGifDecoder;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawableEncoder;
import com.bumptech.glide.load.resource.gif.GifFrameResourceDecoder;
import com.bumptech.glide.load.resource.gif.StreamGifDecoder;
import com.bumptech.glide.load.resource.transcode.BitmapBytesTranscoder;
import com.bumptech.glide.load.resource.transcode.BitmapDrawableTranscoder;
import com.bumptech.glide.load.resource.transcode.DrawableBytesTranscoder;
import com.bumptech.glide.load.resource.transcode.GifDrawableBytesTranscoder;
import com.bumptech.glide.manager.ConnectivityMonitorFactory;
import com.bumptech.glide.manager.RequestManagerRetriever;
import com.bumptech.glide.module.ManifestParser;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.ImageViewTargetFactory;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A singleton to present a simple static interface for building requests with {@link
 * RequestBuilder} and maintaining an {@link Engine}, {@link BitmapPool}, {@link
 * com.bumptech.glide.load.engine.cache.DiskCache} and {@link MemoryCache}.
 */
public class Glide implements ComponentCallbacks2 {
  private static final String DEFAULT_DISK_CACHE_DIR = "image_manager_disk_cache";
  private static final String TAG = "Glide";

  @GuardedBy("Glide.class")
  private static volatile Glide glide;

  private static volatile boolean isInitializing;

  private final Engine engine;
  private final BitmapPool bitmapPool;
  private final MemoryCache memoryCache;
  private final GlideContext glideContext;
  private final Registry registry;
  private final ArrayPool arrayPool;
  private final RequestManagerRetriever requestManagerRetriever;
  private final ConnectivityMonitorFactory connectivityMonitorFactory;

  @GuardedBy("managers")
  private final List<RequestManager> managers = new ArrayList<>();

  private final RequestOptionsFactory defaultRequestOptionsFactory;
  private MemoryCategory memoryCategory = MemoryCategory.NORMAL;

  @GuardedBy("this")
  @Nullable
  private BitmapPreFiller bitmapPreFiller;

  /**
   * Returns a directory with a default name in the private cache directory of the application to
   * use to store retrieved media and thumbnails.
   *
   * @param context A context.
   * @see #getPhotoCacheDir(android.content.Context, String)
   */
  @Nullable
  public static File getPhotoCacheDir(@NonNull Context context) {
    return getPhotoCacheDir(context, DEFAULT_DISK_CACHE_DIR);
  }

  /**
   * Returns a directory with the given name in the private cache directory of the application to
   * use to store retrieved media and thumbnails.
   *
   * @param context A context.
   * @param cacheName The name of the subdirectory in which to store the cache.
   * @see #getPhotoCacheDir(android.content.Context)
   */
  @Nullable
  public static File getPhotoCacheDir(@NonNull Context context, @NonNull String cacheName) {
    File cacheDir = context.getCacheDir();
    if (cacheDir != null) {
      File result = new File(cacheDir, cacheName);
      if (result.isDirectory() || result.mkdirs()) {
        return result;
      }
      // File wasn't able to create a directory, or the result exists but not a directory
      return null;
    }
    if (Log.isLoggable(TAG, Log.ERROR)) {
      Log.e(TAG, "default disk cache dir is null");
    }
    return null;
  }

  /**
   * Get the singleton.
   *
   * @return the singleton
   */
  @NonNull
  // Double checked locking is safe here.
  @SuppressWarnings("GuardedBy")
  public static Glide get(@NonNull Context context) {
    if (glide == null) {
      GeneratedAppGlideModule annotationGeneratedModule =
          getAnnotationGeneratedGlideModules(context.getApplicationContext());
      synchronized (Glide.class) {
        if (glide == null) {
          checkAndInitializeGlide(context, annotationGeneratedModule);
        }
      }
    }

    return glide;
  }

  @GuardedBy("Glide.class")
  private static void checkAndInitializeGlide(
      @NonNull Context context, @Nullable GeneratedAppGlideModule generatedAppGlideModule) {
    // In the thread running initGlide(), one or more classes may call Glide.get(context).
    // Without this check, those calls could trigger infinite recursion.
    if (isInitializing) {
      throw new IllegalStateException(
          "You cannot call Glide.get() in registerComponents(),"
              + " use the provided Glide instance instead");
    }
    isInitializing = true;
    initializeGlide(context, generatedAppGlideModule);
    isInitializing = false;
  }

  /**
   * @deprecated Use {@link #init(Context, GlideBuilder)} to get a singleton compatible with Glide's
   *     generated API.
   *     <p>This method will be removed in a future version of Glide.
   */
  @VisibleForTesting
  @Deprecated
  public static synchronized void init(Glide glide) {
    if (Glide.glide != null) {
      tearDown();
    }
    Glide.glide = glide;
  }

  @VisibleForTesting
  public static void init(@NonNull Context context, @NonNull GlideBuilder builder) {
    GeneratedAppGlideModule annotationGeneratedModule = getAnnotationGeneratedGlideModules(context);
    synchronized (Glide.class) {
      if (Glide.glide != null) {
        tearDown();
      }
      initializeGlide(context, builder, annotationGeneratedModule);
    }
  }

  /**
   * Allows hardware Bitmaps to be used prior to the first frame in the app being drawn as soon as
   * this method is called.
   *
   * <p>If you use this method in non-test code, your app will experience native crashes on some
   * versions of Android if you try to decode a hardware Bitmap. This method is only useful for
   * testing.
   */
  @VisibleForTesting
  public static void enableHardwareBitmaps() {
    HardwareConfigState.getInstance().unblockHardwareBitmaps();
  }

  @VisibleForTesting
  public static void tearDown() {
    synchronized (Glide.class) {
      if (glide != null) {
        glide.getContext().getApplicationContext().unregisterComponentCallbacks(glide);
        glide.engine.shutdown();
      }
      glide = null;
    }
  }

  @GuardedBy("Glide.class")
  private static void initializeGlide(
      @NonNull Context context, @Nullable GeneratedAppGlideModule generatedAppGlideModule) {
    initializeGlide(context, new GlideBuilder(), generatedAppGlideModule);
  }

  @GuardedBy("Glide.class")
  @SuppressWarnings("deprecation")
  private static void initializeGlide(
      @NonNull Context context,
      @NonNull GlideBuilder builder,
      @Nullable GeneratedAppGlideModule annotationGeneratedModule) {
    Context applicationContext = context.getApplicationContext();
    List<com.bumptech.glide.module.GlideModule> manifestModules = Collections.emptyList();
    if (annotationGeneratedModule == null || annotationGeneratedModule.isManifestParsingEnabled()) {
      manifestModules = new ManifestParser(applicationContext).parse();
    }

    if (annotationGeneratedModule != null
        && !annotationGeneratedModule.getExcludedModuleClasses().isEmpty()) {
      Set<Class<?>> excludedModuleClasses = annotationGeneratedModule.getExcludedModuleClasses();
      Iterator<com.bumptech.glide.module.GlideModule> iterator = manifestModules.iterator();
      while (iterator.hasNext()) {
        com.bumptech.glide.module.GlideModule current = iterator.next();
        if (!excludedModuleClasses.contains(current.getClass())) {
          continue;
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "AppGlideModule excludes manifest GlideModule: " + current);
        }
        iterator.remove();
      }
    }

    if (Log.isLoggable(TAG, Log.DEBUG)) {
      for (com.bumptech.glide.module.GlideModule glideModule : manifestModules) {
        Log.d(TAG, "Discovered GlideModule from manifest: " + glideModule.getClass());
      }
    }

    RequestManagerRetriever.RequestManagerFactory factory =
        annotationGeneratedModule != null
            ? annotationGeneratedModule.getRequestManagerFactory()
            : null;
    builder.setRequestManagerFactory(factory);
    for (com.bumptech.glide.module.GlideModule module : manifestModules) {
      module.applyOptions(applicationContext, builder);
    }
    if (annotationGeneratedModule != null) {
      annotationGeneratedModule.applyOptions(applicationContext, builder);
    }
    Glide glide = builder.build(applicationContext);
    for (com.bumptech.glide.module.GlideModule module : manifestModules) {
      try {
        module.registerComponents(applicationContext, glide, glide.registry);
      } catch (AbstractMethodError e) {
        throw new IllegalStateException(
            "Attempting to register a Glide v3 module. If you see this, you or one of your"
                + " dependencies may be including Glide v3 even though you're using Glide v4."
                + " You'll need to find and remove (or update) the offending dependency."
                + " The v3 module name is: "
                + module.getClass().getName(),
            e);
      }
    }
    if (annotationGeneratedModule != null) {
      annotationGeneratedModule.registerComponents(applicationContext, glide, glide.registry);
    }
    applicationContext.registerComponentCallbacks(glide);
    Glide.glide = glide;
  }

  @Nullable
  @SuppressWarnings({"unchecked", "TryWithIdenticalCatches", "PMD.UnusedFormalParameter"})
  private static GeneratedAppGlideModule getAnnotationGeneratedGlideModules(Context context) {
    GeneratedAppGlideModule result = null;
    try {
      Class<GeneratedAppGlideModule> clazz =
          (Class<GeneratedAppGlideModule>)
              Class.forName("com.bumptech.glide.GeneratedAppGlideModuleImpl");
      result =
          clazz.getDeclaredConstructor(Context.class).newInstance(context.getApplicationContext());
    } catch (ClassNotFoundException e) {
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(
            TAG,
            "Failed to find GeneratedAppGlideModule. You should include an"
                + " annotationProcessor compile dependency on com.github.bumptech.glide:compiler"
                + " in your application and a @GlideModule annotated AppGlideModule implementation"
                + " or LibraryGlideModules will be silently ignored");
      }
      // These exceptions can't be squashed across all versions of Android.
    } catch (InstantiationException e) {
      throwIncorrectGlideModule(e);
    } catch (IllegalAccessException e) {
      throwIncorrectGlideModule(e);
    } catch (NoSuchMethodException e) {
      throwIncorrectGlideModule(e);
    } catch (InvocationTargetException e) {
      throwIncorrectGlideModule(e);
    }
    return result;
  }

  private static void throwIncorrectGlideModule(Exception e) {
    throw new IllegalStateException(
        "GeneratedAppGlideModuleImpl is implemented incorrectly."
            + " If you've manually implemented this class, remove your implementation. The"
            + " Annotation processor will generate a correct implementation.",
        e);
  }

  @SuppressWarnings("PMD.UnusedFormalParameter")
  Glide(
      @NonNull Context context,
      @NonNull Engine engine,
      @NonNull MemoryCache memoryCache,
      @NonNull BitmapPool bitmapPool,
      @NonNull ArrayPool arrayPool,
      @NonNull RequestManagerRetriever requestManagerRetriever,
      @NonNull ConnectivityMonitorFactory connectivityMonitorFactory,
      int logLevel,
      @NonNull RequestOptionsFactory defaultRequestOptionsFactory,
      @NonNull Map<Class<?>, TransitionOptions<?, ?>> defaultTransitionOptions,
      @NonNull List<RequestListener<Object>> defaultRequestListeners,
      GlideExperiments experiments) {
    this.engine = engine;
    this.bitmapPool = bitmapPool;
    this.arrayPool = arrayPool;
    this.memoryCache = memoryCache;
    this.requestManagerRetriever = requestManagerRetriever;
    this.connectivityMonitorFactory = connectivityMonitorFactory;
    this.defaultRequestOptionsFactory = defaultRequestOptionsFactory;

    final Resources resources = context.getResources();

    registry = new Registry();
    registry.register(new DefaultImageHeaderParser());
    // Right now we're only using this parser for HEIF images, which are only supported on OMR1+.
    // If we need this for other file types, we should consider removing this restriction.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      registry.register(new ExifInterfaceImageHeaderParser());
    }

    List<ImageHeaderParser> imageHeaderParsers = registry.getImageHeaderParsers();

    ByteBufferGifDecoder byteBufferGifDecoder =
        new ByteBufferGifDecoder(context, imageHeaderParsers, bitmapPool, arrayPool);
    ResourceDecoder<ParcelFileDescriptor, Bitmap> parcelFileDescriptorVideoDecoder =
        VideoDecoder.parcel(bitmapPool);

    // TODO(judds): Make ParcelFileDescriptorBitmapDecoder work with ImageDecoder.
    Downsampler downsampler =
        new Downsampler(
            registry.getImageHeaderParsers(), resources.getDisplayMetrics(), bitmapPool, arrayPool);

    ResourceDecoder<ByteBuffer, Bitmap> byteBufferBitmapDecoder;
    ResourceDecoder<InputStream, Bitmap> streamBitmapDecoder;
    if (experiments.isEnabled(EnableImageDecoderForBitmaps.class)
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      streamBitmapDecoder = new InputStreamBitmapImageDecoderResourceDecoder();
      byteBufferBitmapDecoder = new ByteBufferBitmapImageDecoderResourceDecoder();
    } else {
      byteBufferBitmapDecoder = new ByteBufferBitmapDecoder(downsampler);
      streamBitmapDecoder = new StreamBitmapDecoder(downsampler, arrayPool);
    }

    ResourceDrawableDecoder resourceDrawableDecoder = new ResourceDrawableDecoder(context);
    ResourceLoader.StreamFactory resourceLoaderStreamFactory =
        new ResourceLoader.StreamFactory(resources);
    ResourceLoader.UriFactory resourceLoaderUriFactory = new ResourceLoader.UriFactory(resources);
    ResourceLoader.FileDescriptorFactory resourceLoaderFileDescriptorFactory =
        new ResourceLoader.FileDescriptorFactory(resources);
    ResourceLoader.AssetFileDescriptorFactory resourceLoaderAssetFileDescriptorFactory =
        new ResourceLoader.AssetFileDescriptorFactory(resources);
    BitmapEncoder bitmapEncoder = new BitmapEncoder(arrayPool);

    BitmapBytesTranscoder bitmapBytesTranscoder = new BitmapBytesTranscoder();
    GifDrawableBytesTranscoder gifDrawableBytesTranscoder = new GifDrawableBytesTranscoder();

    ContentResolver contentResolver = context.getContentResolver();

    registry
        .append(ByteBuffer.class, new ByteBufferEncoder())
        .append(InputStream.class, new StreamEncoder(arrayPool))
        /* Bitmaps */
        .append(Registry.BUCKET_BITMAP, ByteBuffer.class, Bitmap.class, byteBufferBitmapDecoder)
        .append(Registry.BUCKET_BITMAP, InputStream.class, Bitmap.class, streamBitmapDecoder);

    if (ParcelFileDescriptorRewinder.isSupported()) {
      registry.append(
          Registry.BUCKET_BITMAP,
          ParcelFileDescriptor.class,
          Bitmap.class,
          new ParcelFileDescriptorBitmapDecoder(downsampler));
    }

    registry
        .append(
            Registry.BUCKET_BITMAP,
            ParcelFileDescriptor.class,
            Bitmap.class,
            parcelFileDescriptorVideoDecoder)
        .append(
            Registry.BUCKET_BITMAP,
            AssetFileDescriptor.class,
            Bitmap.class,
            VideoDecoder.asset(bitmapPool))
        .append(Bitmap.class, Bitmap.class, UnitModelLoader.Factory.<Bitmap>getInstance())
        .append(Registry.BUCKET_BITMAP, Bitmap.class, Bitmap.class, new UnitBitmapDecoder())
        .append(Bitmap.class, bitmapEncoder)
        /* BitmapDrawables */
        .append(
            Registry.BUCKET_BITMAP_DRAWABLE,
            ByteBuffer.class,
            BitmapDrawable.class,
            new BitmapDrawableDecoder<>(resources, byteBufferBitmapDecoder))
        .append(
            Registry.BUCKET_BITMAP_DRAWABLE,
            InputStream.class,
            BitmapDrawable.class,
            new BitmapDrawableDecoder<>(resources, streamBitmapDecoder))
        .append(
            Registry.BUCKET_BITMAP_DRAWABLE,
            ParcelFileDescriptor.class,
            BitmapDrawable.class,
            new BitmapDrawableDecoder<>(resources, parcelFileDescriptorVideoDecoder))
        .append(BitmapDrawable.class, new BitmapDrawableEncoder(bitmapPool, bitmapEncoder))
        /* GIFs */
        .append(
            Registry.BUCKET_GIF,
            InputStream.class,
            GifDrawable.class,
            new StreamGifDecoder(imageHeaderParsers, byteBufferGifDecoder, arrayPool))
        .append(Registry.BUCKET_GIF, ByteBuffer.class, GifDrawable.class, byteBufferGifDecoder)
        .append(GifDrawable.class, new GifDrawableEncoder())
        /* GIF Frames */
        // Compilation with Gradle requires the type to be specified for UnitModelLoader here.
        .append(
            GifDecoder.class, GifDecoder.class, UnitModelLoader.Factory.<GifDecoder>getInstance())
        .append(
            Registry.BUCKET_BITMAP,
            GifDecoder.class,
            Bitmap.class,
            new GifFrameResourceDecoder(bitmapPool))
        /* Drawables */
        .append(Uri.class, Drawable.class, resourceDrawableDecoder)
        .append(
            Uri.class, Bitmap.class, new ResourceBitmapDecoder(resourceDrawableDecoder, bitmapPool))
        /* Files */
        .register(new ByteBufferRewinder.Factory())
        .append(File.class, ByteBuffer.class, new ByteBufferFileLoader.Factory())
        .append(File.class, InputStream.class, new FileLoader.StreamFactory())
        .append(File.class, File.class, new FileDecoder())
        .append(File.class, ParcelFileDescriptor.class, new FileLoader.FileDescriptorFactory())
        // Compilation with Gradle requires the type to be specified for UnitModelLoader here.
        .append(File.class, File.class, UnitModelLoader.Factory.<File>getInstance())
        /* Models */
        .register(new InputStreamRewinder.Factory(arrayPool));

    if (ParcelFileDescriptorRewinder.isSupported()) {
      registry.register(new ParcelFileDescriptorRewinder.Factory());
    }

    registry
        .append(int.class, InputStream.class, resourceLoaderStreamFactory)
        .append(int.class, ParcelFileDescriptor.class, resourceLoaderFileDescriptorFactory)
        .append(Integer.class, InputStream.class, resourceLoaderStreamFactory)
        .append(Integer.class, ParcelFileDescriptor.class, resourceLoaderFileDescriptorFactory)
        .append(Integer.class, Uri.class, resourceLoaderUriFactory)
        .append(int.class, AssetFileDescriptor.class, resourceLoaderAssetFileDescriptorFactory)
        .append(Integer.class, AssetFileDescriptor.class, resourceLoaderAssetFileDescriptorFactory)
        .append(int.class, Uri.class, resourceLoaderUriFactory)
        .append(String.class, InputStream.class, new DataUrlLoader.StreamFactory<String>())
        .append(Uri.class, InputStream.class, new DataUrlLoader.StreamFactory<Uri>())
        .append(String.class, InputStream.class, new StringLoader.StreamFactory())
        .append(String.class, ParcelFileDescriptor.class, new StringLoader.FileDescriptorFactory())
        .append(
            String.class, AssetFileDescriptor.class, new StringLoader.AssetFileDescriptorFactory())
        .append(Uri.class, InputStream.class, new AssetUriLoader.StreamFactory(context.getAssets()))
        .append(
            Uri.class,
            ParcelFileDescriptor.class,
            new AssetUriLoader.FileDescriptorFactory(context.getAssets()))
        .append(Uri.class, InputStream.class, new MediaStoreImageThumbLoader.Factory(context))
        .append(Uri.class, InputStream.class, new MediaStoreVideoThumbLoader.Factory(context));
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      registry.append(
          Uri.class, InputStream.class, new QMediaStoreUriLoader.InputStreamFactory(context));
      registry.append(
          Uri.class,
          ParcelFileDescriptor.class,
          new QMediaStoreUriLoader.FileDescriptorFactory(context));
    }
    registry
        .append(Uri.class, InputStream.class, new UriLoader.StreamFactory(contentResolver))
        .append(
            Uri.class,
            ParcelFileDescriptor.class,
            new UriLoader.FileDescriptorFactory(contentResolver))
        .append(
            Uri.class,
            AssetFileDescriptor.class,
            new UriLoader.AssetFileDescriptorFactory(contentResolver))
        .append(Uri.class, InputStream.class, new UrlUriLoader.StreamFactory())
        .append(URL.class, InputStream.class, new UrlLoader.StreamFactory())
        .append(Uri.class, File.class, new MediaStoreFileLoader.Factory(context))
        .append(GlideUrl.class, InputStream.class, new HttpGlideUrlLoader.Factory())
        .append(byte[].class, ByteBuffer.class, new ByteArrayLoader.ByteBufferFactory())
        .append(byte[].class, InputStream.class, new ByteArrayLoader.StreamFactory())
        .append(Uri.class, Uri.class, UnitModelLoader.Factory.<Uri>getInstance())
        .append(Drawable.class, Drawable.class, UnitModelLoader.Factory.<Drawable>getInstance())
        .append(Drawable.class, Drawable.class, new UnitDrawableDecoder())
        /* Transcoders */
        .register(Bitmap.class, BitmapDrawable.class, new BitmapDrawableTranscoder(resources))
        .register(Bitmap.class, byte[].class, bitmapBytesTranscoder)
        .register(
            Drawable.class,
            byte[].class,
            new DrawableBytesTranscoder(
                bitmapPool, bitmapBytesTranscoder, gifDrawableBytesTranscoder))
        .register(GifDrawable.class, byte[].class, gifDrawableBytesTranscoder);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      ResourceDecoder<ByteBuffer, Bitmap> byteBufferVideoDecoder =
          VideoDecoder.byteBuffer(bitmapPool);
      registry.append(ByteBuffer.class, Bitmap.class, byteBufferVideoDecoder);
      registry.append(
          ByteBuffer.class,
          BitmapDrawable.class,
          new BitmapDrawableDecoder<>(resources, byteBufferVideoDecoder));
    }

    ImageViewTargetFactory imageViewTargetFactory = new ImageViewTargetFactory();
    glideContext =
        new GlideContext(
            context,
            arrayPool,
            registry,
            imageViewTargetFactory,
            defaultRequestOptionsFactory,
            defaultTransitionOptions,
            defaultRequestListeners,
            engine,
            experiments,
            logLevel);
  }

  /**
   * Returns the {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} used to
   * temporarily store {@link android.graphics.Bitmap}s so they can be reused to avoid garbage
   * collections.
   *
   * <p>Note - Using this pool directly can lead to undefined behavior and strange drawing errors.
   * Any {@link android.graphics.Bitmap} added to the pool must not be currently in use in any other
   * part of the application. Any {@link android.graphics.Bitmap} added to the pool must be removed
   * from the pool before it is added a second time.
   *
   * <p>Note - To make effective use of the pool, any {@link android.graphics.Bitmap} removed from
   * the pool must eventually be re-added. Otherwise the pool will eventually empty and will not
   * serve any useful purpose.
   *
   * <p>The primary reason this object is exposed is for use in custom {@link
   * com.bumptech.glide.load.ResourceDecoder}s and {@link com.bumptech.glide.load.Transformation}s.
   * Use outside of these classes is not generally recommended.
   */
  @NonNull
  public BitmapPool getBitmapPool() {
    return bitmapPool;
  }

  @NonNull
  public ArrayPool getArrayPool() {
    return arrayPool;
  }

  /** @return The context associated with this instance. */
  @NonNull
  public Context getContext() {
    return glideContext.getBaseContext();
  }

  ConnectivityMonitorFactory getConnectivityMonitorFactory() {
    return connectivityMonitorFactory;
  }

  @NonNull
  GlideContext getGlideContext() {
    return glideContext;
  }

  /**
   * Pre-fills the {@link BitmapPool} using the given sizes.
   *
   * <p>Enough Bitmaps are added to completely fill the pool, so most or all of the Bitmaps
   * currently in the pool will be evicted. Bitmaps are allocated according to the weights of the
   * given sizes, where each size gets (weight / prefillWeightSum) percent of the pool to fill.
   *
   * <p>Note - Pre-filling is done asynchronously using and {@link IdleHandler}. Any currently
   * running pre-fill will be cancelled and replaced by a call to this method.
   *
   * <p>This method should be used with caution, overly aggressive pre-filling is substantially
   * worse than not pre-filling at all. Pre-filling should only be started in onCreate to avoid
   * constantly clearing and re-filling the {@link BitmapPool}. Rotation should be carefully
   * considered as well. It may be worth calling this method only when no saved instance state
   * exists so that pre-filling only happens when the Activity is first created, rather than on
   * every rotation.
   *
   * @param bitmapAttributeBuilders The list of {@link Builder Builders} representing individual
   *     sizes and configurations of {@link Bitmap}s to be pre-filled.
   */
  @SuppressWarnings("unused") // Public API
  public synchronized void preFillBitmapPool(
      @NonNull PreFillType.Builder... bitmapAttributeBuilders) {
    if (bitmapPreFiller == null) {
      DecodeFormat decodeFormat =
          defaultRequestOptionsFactory.build().getOptions().get(Downsampler.DECODE_FORMAT);
      bitmapPreFiller = new BitmapPreFiller(memoryCache, bitmapPool, decodeFormat);
    }

    bitmapPreFiller.preFill(bitmapAttributeBuilders);
  }

  /**
   * Clears as much memory as possible.
   *
   * @see android.content.ComponentCallbacks#onLowMemory()
   * @see android.content.ComponentCallbacks2#onLowMemory()
   */
  public void clearMemory() {
    // Engine asserts this anyway when removing resources, fail faster and consistently
    Util.assertMainThread();
    // memory cache needs to be cleared before bitmap pool to clear re-pooled Bitmaps too. See #687.
    memoryCache.clearMemory();
    bitmapPool.clearMemory();
    arrayPool.clearMemory();
  }

  /**
   * Clears some memory with the exact amount depending on the given level.
   *
   * @see android.content.ComponentCallbacks2#onTrimMemory(int)
   */
  public void trimMemory(int level) {
    // Engine asserts this anyway when removing resources, fail faster and consistently
    Util.assertMainThread();
    // Request managers need to be trimmed before the caches and pools, in order for the latter to
    // have the most benefit.
    synchronized (managers) {
      for (RequestManager manager : managers) {
        manager.onTrimMemory(level);
      }
    }
    // memory cache needs to be trimmed before bitmap pool to trim re-pooled Bitmaps too. See #687.
    memoryCache.trimMemory(level);
    bitmapPool.trimMemory(level);
    arrayPool.trimMemory(level);
  }

  /**
   * Clears disk cache.
   *
   * <p>This method should always be called on a background thread, since it is a blocking call.
   */
  // Public API.
  @SuppressWarnings({"unused", "WeakerAccess"})
  public void clearDiskCache() {
    Util.assertBackgroundThread();
    engine.clearDiskCache();
  }

  /** Internal method. */
  @NonNull
  public RequestManagerRetriever getRequestManagerRetriever() {
    return requestManagerRetriever;
  }

  /**
   * Adjusts Glide's current and maximum memory usage based on the given {@link MemoryCategory}.
   *
   * <p>The default {@link MemoryCategory} is {@link MemoryCategory#NORMAL}. {@link
   * MemoryCategory#HIGH} increases Glide's maximum memory usage by up to 50% and {@link
   * MemoryCategory#LOW} decreases Glide's maximum memory usage by 50%. This method should be used
   * to temporarily increase or decrease memory usage for a single Activity or part of the app. Use
   * {@link GlideBuilder#setMemoryCache(MemoryCache)} to put a permanent memory size if you want to
   * change the default.
   *
   * @return the previous MemoryCategory used by Glide.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  @NonNull
  public MemoryCategory setMemoryCategory(@NonNull MemoryCategory memoryCategory) {
    // Engine asserts this anyway when removing resources, fail faster and consistently
    Util.assertMainThread();
    // memory cache needs to be trimmed before bitmap pool to trim re-pooled Bitmaps too. See #687.
    memoryCache.setSizeMultiplier(memoryCategory.getMultiplier());
    bitmapPool.setSizeMultiplier(memoryCategory.getMultiplier());
    MemoryCategory oldCategory = this.memoryCategory;
    this.memoryCategory = memoryCategory;
    return oldCategory;
  }

  @NonNull
  private static RequestManagerRetriever getRetriever(@Nullable Context context) {
    // Context could be null for other reasons (ie the user passes in null), but in practice it will
    // only occur due to errors with the Fragment lifecycle.
    Preconditions.checkNotNull(
        context,
        "You cannot start a load on a not yet attached View or a Fragment where getActivity() "
            + "returns null (which usually occurs when getActivity() is called before the Fragment "
            + "is attached or after the Fragment is destroyed).");
    return Glide.get(context).getRequestManagerRetriever();
  }

  /**
   * Begin a load with Glide by passing in a context.
   *
   * <p>Any requests started using a context will only have the application level options applied
   * and will not be started or stopped based on lifecycle events. In general, loads should be
   * started at the level the result will be used in. If the resource will be used in a view in a
   * child fragment, the load should be started with {@link #with(android.app.Fragment)}} using that
   * child fragment. Similarly, if the resource will be used in a view in the parent fragment, the
   * load should be started with {@link #with(android.app.Fragment)} using the parent fragment. In
   * the same vein, if the resource will be used in a view in an activity, the load should be
   * started with {@link #with(android.app.Activity)}}.
   *
   * <p>This method is appropriate for resources that will be used outside of the normal fragment or
   * activity lifecycle (For example in services, or for notification thumbnails).
   *
   * @param context Any context, will not be retained.
   * @return A RequestManager for the top level application that can be used to start a load.
   * @see #with(android.app.Activity)
   * @see #with(android.app.Fragment)
   * @see #with(androidx.fragment.app.Fragment)
   * @see #with(androidx.fragment.app.FragmentActivity)
   */
  @NonNull
  public static RequestManager with(@NonNull Context context) {
    return getRetriever(context).get(context);
  }

  /**
   * Begin a load with Glide that will be tied to the given {@link android.app.Activity}'s lifecycle
   * and that uses the given {@link Activity}'s default options.
   *
   * @param activity The activity to use.
   * @return A RequestManager for the given activity that can be used to start a load.
   */
  @NonNull
  public static RequestManager with(@NonNull Activity activity) {
    return getRetriever(activity).get(activity);
  }

  /**
   * Begin a load with Glide that will tied to the give {@link
   * androidx.fragment.app.FragmentActivity}'s lifecycle and that uses the given {@link
   * androidx.fragment.app.FragmentActivity}'s default options.
   *
   * @param activity The activity to use.
   * @return A RequestManager for the given FragmentActivity that can be used to start a load.
   */
  @NonNull
  public static RequestManager with(@NonNull FragmentActivity activity) {
    return getRetriever(activity).get(activity);
  }

  /**
   * Begin a load with Glide that will be tied to the given {@link androidx.fragment.app.Fragment}'s
   * lifecycle and that uses the given {@link androidx.fragment.app.Fragment}'s default options.
   *
   * @param fragment The fragment to use.
   * @return A RequestManager for the given Fragment that can be used to start a load.
   */
  @NonNull
  public static RequestManager with(@NonNull Fragment fragment) {
    return getRetriever(fragment.getContext()).get(fragment);
  }

  /**
   * Begin a load with Glide that will be tied to the given {@link android.app.Fragment}'s lifecycle
   * and that uses the given {@link android.app.Fragment}'s default options.
   *
   * @param fragment The fragment to use.
   * @return A RequestManager for the given Fragment that can be used to start a load.
   * @deprecated Prefer support Fragments and {@link #with(Fragment)} instead, {@link
   *     android.app.Fragment} will be deprecated. See
   *     https://github.com/android/android-ktx/pull/161#issuecomment-363270555.
   */
  @SuppressWarnings("deprecation")
  @Deprecated
  @NonNull
  public static RequestManager with(@NonNull android.app.Fragment fragment) {
    return getRetriever(fragment.getActivity()).get(fragment);
  }

  /**
   * Begin a load with Glide that will be tied to the lifecycle of the {@link Fragment}, {@link
   * android.app.Fragment}, or {@link Activity} that contains the View.
   *
   * <p>A {@link Fragment} or {@link android.app.Fragment} is assumed to contain a View if the View
   * is a child of the View returned by the {@link Fragment#getView()}} method.
   *
   * <p>This method will not work if the View is not attached. Prefer the Activity and Fragment
   * variants unless you're loading in a View subclass.
   *
   * <p>This method may be inefficient aways and is definitely inefficient for large hierarchies.
   * Consider memoizing the result after the View is attached or again, prefer the Activity and
   * Fragment variants whenever possible.
   *
   * <p>When used in Applications that use the non-support {@link android.app.Fragment} classes,
   * calling this method will produce noisy logs from {@link android.app.FragmentManager}. Consider
   * avoiding entirely or using the {@link Fragment}s from the support library instead.
   *
   * <p>If the support {@link FragmentActivity} class is used, this method will only attempt to
   * discover support {@link Fragment}s. Any non-support {@link android.app.Fragment}s attached to
   * the {@link FragmentActivity} will be ignored.
   *
   * @param view The view to search for a containing Fragment or Activity from.
   * @return A RequestManager that can be used to start a load.
   */
  @NonNull
  public static RequestManager with(@NonNull View view) {
    return getRetriever(view.getContext()).get(view);
  }

  @NonNull
  public Registry getRegistry() {
    return registry;
  }

  boolean removeFromManagers(@NonNull Target<?> target) {
    synchronized (managers) {
      for (RequestManager requestManager : managers) {
        if (requestManager.untrack(target)) {
          return true;
        }
      }
    }

    return false;
  }

  void registerRequestManager(RequestManager requestManager) {
    synchronized (managers) {
      if (managers.contains(requestManager)) {
        throw new IllegalStateException("Cannot register already registered manager");
      }
      managers.add(requestManager);
    }
  }

  void unregisterRequestManager(RequestManager requestManager) {
    synchronized (managers) {
      if (!managers.contains(requestManager)) {
        throw new IllegalStateException("Cannot unregister not yet registered manager");
      }
      managers.remove(requestManager);
    }
  }

  @Override
  public void onTrimMemory(int level) {
    trimMemory(level);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    // Do nothing.
  }

  @Override
  public void onLowMemory() {
    clearMemory();
  }

  /** Creates a new instance of {@link RequestOptions}. */
  public interface RequestOptionsFactory {

    /** Returns a non-null {@link RequestOptions} object. */
    @NonNull
    RequestOptions build();
  }
}
