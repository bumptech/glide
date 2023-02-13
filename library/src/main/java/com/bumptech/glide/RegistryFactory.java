package com.bumptech.glide;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import androidx.annotation.Nullable;
import androidx.tracing.Trace;
import com.bumptech.glide.GlideBuilder.EnableImageDecoderForBitmaps;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.data.InputStreamRewinder;
import com.bumptech.glide.load.data.ParcelFileDescriptorRewinder;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.model.AssetUriLoader;
import com.bumptech.glide.load.model.ByteArrayLoader;
import com.bumptech.glide.load.model.ByteBufferEncoder;
import com.bumptech.glide.load.model.ByteBufferFileLoader;
import com.bumptech.glide.load.model.DataUrlLoader;
import com.bumptech.glide.load.model.DirectResourceLoader;
import com.bumptech.glide.load.model.FileLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.MediaStoreFileLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.ResourceLoader;
import com.bumptech.glide.load.model.ResourceUriLoader;
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
import com.bumptech.glide.load.resource.bitmap.InputStreamBitmapImageDecoderResourceDecoder;
import com.bumptech.glide.load.resource.bitmap.ParcelFileDescriptorBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.ResourceBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.UnitBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.VideoDecoder;
import com.bumptech.glide.load.resource.bytes.ByteBufferRewinder;
import com.bumptech.glide.load.resource.drawable.AnimatedImageDecoder;
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
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.module.GlideModule;
import com.bumptech.glide.util.GlideSuppliers.GlideSupplier;
import com.bumptech.glide.util.Synthetic;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;

final class RegistryFactory {

  private RegistryFactory() {}

  static GlideSupplier<Registry> lazilyCreateAndInitializeRegistry(
      final Glide glide,
      final List<GlideModule> manifestModules,
      @Nullable final AppGlideModule annotationGeneratedModule) {
    return new GlideSupplier<Registry>() {
      // Rely on callers using memoization if they want to avoid duplicate work, but
      // rely on ourselves to verify that no recursive initialization occurs.
      private boolean isInitializing;

      @Override
      public Registry get() {
        if (isInitializing) {
          throw new IllegalStateException(
              "Recursive Registry initialization! In your"
                  + " AppGlideModule and LibraryGlideModules, Make sure you're using the provided "
                  + "Registry rather calling glide.getRegistry()!");
        }
        Trace.beginSection("Glide registry");
        isInitializing = true;
        try {
          return createAndInitRegistry(glide, manifestModules, annotationGeneratedModule);
        } finally {
          isInitializing = false;
          Trace.endSection();
        }
      }
    };
  }

  @Synthetic
  static Registry createAndInitRegistry(
      Glide glide,
      List<GlideModule> manifestModules,
      @Nullable AppGlideModule annotationGeneratedModule) {

    BitmapPool bitmapPool = glide.getBitmapPool();
    ArrayPool arrayPool = glide.getArrayPool();
    Context context = glide.getGlideContext().getApplicationContext();

    GlideExperiments experiments = glide.getGlideContext().getExperiments();

    Registry registry = new Registry();
    initializeDefaults(context, registry, bitmapPool, arrayPool, experiments);
    initializeModules(context, glide, registry, manifestModules, annotationGeneratedModule);
    return registry;
  }

  private static void initializeDefaults(
      Context context,
      Registry registry,
      BitmapPool bitmapPool,
      ArrayPool arrayPool,
      GlideExperiments experiments) {
    registry.register(new DefaultImageHeaderParser());
    // Right now we're only using this parser for HEIF images, which are only supported on OMR1+.
    // If we need this for other file types, we should consider removing this restriction.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      registry.register(new ExifInterfaceImageHeaderParser());
    }

    final Resources resources = context.getResources();
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        && experiments.isEnabled(EnableImageDecoderForBitmaps.class)) {
      streamBitmapDecoder = new InputStreamBitmapImageDecoderResourceDecoder();
      byteBufferBitmapDecoder = new ByteBufferBitmapImageDecoderResourceDecoder();
    } else {
      byteBufferBitmapDecoder = new ByteBufferBitmapDecoder(downsampler);
      streamBitmapDecoder = new StreamBitmapDecoder(downsampler, arrayPool);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      registry.append(
          Registry.BUCKET_ANIMATION,
          InputStream.class,
          Drawable.class,
          AnimatedImageDecoder.streamDecoder(imageHeaderParsers, arrayPool));
      registry.append(
          Registry.BUCKET_ANIMATION,
          ByteBuffer.class,
          Drawable.class,
          AnimatedImageDecoder.byteBufferDecoder(imageHeaderParsers, arrayPool));
    }

    ResourceDrawableDecoder resourceDrawableDecoder = new ResourceDrawableDecoder(context);

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
            Registry.BUCKET_ANIMATION,
            InputStream.class,
            GifDrawable.class,
            new StreamGifDecoder(imageHeaderParsers, byteBufferGifDecoder, arrayPool))
        .append(
            Registry.BUCKET_ANIMATION, ByteBuffer.class, GifDrawable.class, byteBufferGifDecoder)
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

    // DirectResourceLoader and ResourceUriLoader handle resource IDs and Uris owned by this
    // package.
    ModelLoaderFactory<Integer, InputStream> directResourceLoaderStreamFactory =
        DirectResourceLoader.inputStreamFactory(context);
    ModelLoaderFactory<Integer, AssetFileDescriptor>
        directResourceLoaderAssetFileDescriptorFactory =
            DirectResourceLoader.assetFileDescriptorFactory(context);
    ModelLoaderFactory<Integer, Drawable> directResourceLaoderDrawableFactory =
        DirectResourceLoader.drawableFactory(context);
    registry
        .append(int.class, InputStream.class, directResourceLoaderStreamFactory)
        .append(Integer.class, InputStream.class, directResourceLoaderStreamFactory)
        .append(
            int.class, AssetFileDescriptor.class, directResourceLoaderAssetFileDescriptorFactory)
        .append(
            Integer.class,
            AssetFileDescriptor.class,
            directResourceLoaderAssetFileDescriptorFactory)
        .append(int.class, Drawable.class, directResourceLaoderDrawableFactory)
        .append(Integer.class, Drawable.class, directResourceLaoderDrawableFactory)
        .append(Uri.class, InputStream.class, ResourceUriLoader.newStreamFactory(context))
        .append(
            Uri.class,
            AssetFileDescriptor.class,
            ResourceUriLoader.newAssetFileDescriptorFactory(context));

    // ResourceLoader and UriLoader handle resource IDs and Uris owned by other packages.
    ResourceLoader.UriFactory resourceLoaderUriFactory = new ResourceLoader.UriFactory(resources);
    ResourceLoader.AssetFileDescriptorFactory resourceLoaderAssetFileDescriptorFactory =
        new ResourceLoader.AssetFileDescriptorFactory(resources);
    ResourceLoader.StreamFactory resourceLoaderStreamFactory =
        new ResourceLoader.StreamFactory(resources);
    registry
        .append(Integer.class, Uri.class, resourceLoaderUriFactory)
        .append(int.class, Uri.class, resourceLoaderUriFactory)
        .append(Integer.class, AssetFileDescriptor.class, resourceLoaderAssetFileDescriptorFactory)
        .append(int.class, AssetFileDescriptor.class, resourceLoaderAssetFileDescriptorFactory)
        .append(Integer.class, InputStream.class, resourceLoaderStreamFactory)
        .append(int.class, InputStream.class, resourceLoaderStreamFactory);

    registry
        .append(String.class, InputStream.class, new DataUrlLoader.StreamFactory<String>())
        .append(Uri.class, InputStream.class, new DataUrlLoader.StreamFactory<Uri>())
        .append(String.class, InputStream.class, new StringLoader.StreamFactory())
        .append(String.class, ParcelFileDescriptor.class, new StringLoader.FileDescriptorFactory())
        .append(
            String.class, AssetFileDescriptor.class, new StringLoader.AssetFileDescriptorFactory())
        .append(Uri.class, InputStream.class, new AssetUriLoader.StreamFactory(context.getAssets()))
        .append(
            Uri.class,
            AssetFileDescriptor.class,
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
  }

  private static void initializeModules(
      Context context,
      Glide glide,
      Registry registry,
      List<GlideModule> manifestModules,
      @Nullable AppGlideModule annotationGeneratedModule) {
    for (GlideModule module : manifestModules) {
      try {
        module.registerComponents(context, glide, registry);
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
      annotationGeneratedModule.registerComponents(context, glide, registry);
    }
  }
}
