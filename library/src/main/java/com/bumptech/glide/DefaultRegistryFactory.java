package com.bumptech.glide;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.data.InputStreamRewinder;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
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
import com.bumptech.glide.load.model.stream.HttpUriLoader;
import com.bumptech.glide.load.model.stream.MediaStoreImageThumbLoader;
import com.bumptech.glide.load.model.stream.MediaStoreVideoThumbLoader;
import com.bumptech.glide.load.model.stream.UrlLoader;
import com.bumptech.glide.load.resource.bitmap.BitmapDrawableDecoder;
import com.bumptech.glide.load.resource.bitmap.BitmapDrawableEncoder;
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder;
import com.bumptech.glide.load.resource.bitmap.ByteBufferBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.DefaultImageHeaderParser;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
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
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;

class DefaultRegistryFactory {

  private final @NonNull Context context;
  private final @NonNull BitmapPool bitmapPool;
  private final @NonNull ArrayPool arrayPool;
  private final @NonNull Resources resources;

  private Registry registry;
  private ByteBufferGifDecoder byteBufferGifDecoder;
  private ResourceDecoder<ParcelFileDescriptor, Bitmap> parcelFileDescriptorVideoDecoder;
  private ByteBufferBitmapDecoder byteBufferBitmapDecoder;
  private StreamBitmapDecoder streamBitmapDecoder;
  private ResourceDrawableDecoder resourceDrawableDecoder;
  private ResourceLoader.StreamFactory resourceLoaderStreamFactory;
  private ResourceLoader.UriFactory resourceLoaderUriFactory;
  private ResourceLoader.FileDescriptorFactory resourceLoaderFileDescriptorFactory;
  private ResourceLoader.AssetFileDescriptorFactory resourceLoaderAssetFileDescriptorFactory;
  private BitmapEncoder bitmapEncoder;
  private BitmapBytesTranscoder bitmapBytesTranscoder;
  private GifDrawableBytesTranscoder gifDrawableBytesTranscoder;
  private final ContentResolver contentResolver;

  DefaultRegistryFactory(
      @NonNull Context context, @NonNull BitmapPool bitmapPool, @NonNull ArrayPool arrayPool) {
    this.context = context;
    this.bitmapPool = bitmapPool;
    this.arrayPool = arrayPool;
    this.resources = context.getResources();
    this.contentResolver = context.getContentResolver();
  }

  @NonNull
  Registry create() {
    registry = new Registry();
    init();
    registerBasics();
    registerBitmaps();
    registerBitmapDrawables();
    registerGifs();
    registerGifFrames();
    registerDrawables();
    registerFiles();
    registerModels();
    registerTranscoders();
    return registry;
  }

  private void init() {
    registry.register(new DefaultImageHeaderParser());

    List<ImageHeaderParser> imageHeaderParsers = registry.getImageHeaderParsers();
    Downsampler downsampler =
        new Downsampler(imageHeaderParsers, resources.getDisplayMetrics(), bitmapPool, arrayPool);
    byteBufferGifDecoder =
        new ByteBufferGifDecoder(context, imageHeaderParsers, bitmapPool, arrayPool);
    parcelFileDescriptorVideoDecoder = VideoDecoder.parcel(bitmapPool);
    byteBufferBitmapDecoder = new ByteBufferBitmapDecoder(downsampler);
    streamBitmapDecoder = new StreamBitmapDecoder(downsampler, arrayPool);
    resourceDrawableDecoder = new ResourceDrawableDecoder(context);
    resourceLoaderStreamFactory = new ResourceLoader.StreamFactory(resources);
    resourceLoaderUriFactory = new ResourceLoader.UriFactory(resources);
    resourceLoaderFileDescriptorFactory = new ResourceLoader.FileDescriptorFactory(resources);
    resourceLoaderAssetFileDescriptorFactory =
        new ResourceLoader.AssetFileDescriptorFactory(resources);
    bitmapEncoder = new BitmapEncoder();

    bitmapBytesTranscoder = new BitmapBytesTranscoder();
    gifDrawableBytesTranscoder = new GifDrawableBytesTranscoder();
  }

  private void registerBasics() {
    registry
        .append(ByteBuffer.class, new ByteBufferEncoder())
        .append(InputStream.class, new StreamEncoder(arrayPool));
  }

  private void registerBitmaps() {
    registry
        .append(Registry.BUCKET_BITMAP, ByteBuffer.class, Bitmap.class, byteBufferBitmapDecoder)
        .append(Registry.BUCKET_BITMAP, InputStream.class, Bitmap.class, streamBitmapDecoder)
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
        .append(
            Registry.BUCKET_BITMAP, Bitmap.class, Bitmap.class, new UnitBitmapDecoder())
        .append(Bitmap.class, bitmapEncoder);
  }

  private void registerBitmapDrawables() {
    registry
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
        .append(BitmapDrawable.class, new BitmapDrawableEncoder(bitmapPool, bitmapEncoder));
  }

  private void registerGifs() {
    registry
        .append(
            Registry.BUCKET_GIF,
            InputStream.class,
            GifDrawable.class,
            new StreamGifDecoder(registry.getImageHeaderParsers(), byteBufferGifDecoder, arrayPool))
        .append(Registry.BUCKET_GIF, ByteBuffer.class, GifDrawable.class, byteBufferGifDecoder)
        .append(GifDrawable.class, new GifDrawableEncoder());
  }

  private void registerGifFrames() {
    registry
        // Compilation with Gradle requires the type to be specified for UnitModelLoader here.
        .append(
            GifDecoder.class, GifDecoder.class, UnitModelLoader.Factory.<GifDecoder>getInstance())
        .append(
            Registry.BUCKET_BITMAP,
            GifDecoder.class,
            Bitmap.class,
            new GifFrameResourceDecoder(bitmapPool));
  }

  private void registerDrawables() {
    registry
        .append(Uri.class, Drawable.class, resourceDrawableDecoder)
        .append(Uri.class, Bitmap.class,
            new ResourceBitmapDecoder(resourceDrawableDecoder, bitmapPool));
  }

  private void registerFiles() {
    registry
        .register(new ByteBufferRewinder.Factory())
        .append(File.class, ByteBuffer.class, new ByteBufferFileLoader.Factory())
        .append(File.class, InputStream.class, new FileLoader.StreamFactory())
        .append(File.class, File.class, new FileDecoder())
        .append(File.class, ParcelFileDescriptor.class, new FileLoader.FileDescriptorFactory())
        // Compilation with Gradle requires the type to be specified for UnitModelLoader here.
        .append(File.class, File.class, UnitModelLoader.Factory.<File>getInstance());
  }

  private void registerModels() {
    registry
        .register(new InputStreamRewinder.Factory(arrayPool))
        .append(int.class, InputStream.class, resourceLoaderStreamFactory)
        .append(
            int.class,
            ParcelFileDescriptor.class,
            resourceLoaderFileDescriptorFactory)
        .append(Integer.class, InputStream.class, resourceLoaderStreamFactory)
        .append(
            Integer.class,
            ParcelFileDescriptor.class,
            resourceLoaderFileDescriptorFactory)
        .append(Integer.class, Uri.class, resourceLoaderUriFactory)
        .append(
            int.class,
            AssetFileDescriptor.class,
            resourceLoaderAssetFileDescriptorFactory)
        .append(
            Integer.class,
            AssetFileDescriptor.class,
            resourceLoaderAssetFileDescriptorFactory)
        .append(int.class, Uri.class, resourceLoaderUriFactory)
        .append(String.class, InputStream.class, new DataUrlLoader.StreamFactory())
        .append(String.class, InputStream.class, new StringLoader.StreamFactory())
        .append(String.class, ParcelFileDescriptor.class, new StringLoader.FileDescriptorFactory())
        .append(
            String.class, AssetFileDescriptor.class, new StringLoader.AssetFileDescriptorFactory())
        .append(Uri.class, InputStream.class, new HttpUriLoader.Factory())
        .append(Uri.class, InputStream.class, new AssetUriLoader.StreamFactory(context.getAssets()))
        .append(
            Uri.class,
            ParcelFileDescriptor.class,
            new AssetUriLoader.FileDescriptorFactory(context.getAssets()))
        .append(Uri.class, InputStream.class, new MediaStoreImageThumbLoader.Factory(context))
        .append(Uri.class, InputStream.class, new MediaStoreVideoThumbLoader.Factory(context))
        .append(
            Uri.class,
            InputStream.class,
            new UriLoader.StreamFactory(contentResolver))
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
        .append(Drawable.class, Drawable.class, new UnitDrawableDecoder());
  }

  private void registerTranscoders() {
    registry
        .register(
            Bitmap.class,
            BitmapDrawable.class,
            new BitmapDrawableTranscoder(resources))
        .register(Bitmap.class, byte[].class, bitmapBytesTranscoder)
        .register(
            Drawable.class,
            byte[].class,
            new DrawableBytesTranscoder(
                bitmapPool, bitmapBytesTranscoder, gifDrawableBytesTranscoder))
        .register(GifDrawable.class, byte[].class, gifDrawableBytesTranscoder);
  }
}
