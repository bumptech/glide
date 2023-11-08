package com.bumptech.glide.integration.avif;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.resource.bitmap.BitmapDrawableDecoder;
import com.bumptech.glide.module.LibraryGlideModule;
import java.io.InputStream;
import java.nio.ByteBuffer;

/** Glide support for AVIF Images. */
@GlideModule
public final class AvifGlideModule extends LibraryGlideModule {

  @Override
  public void registerComponents(
      @NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
    // Add the Avif ResourceDecoders before any of the available system decoders. This ensures that
    // the integration will be preferred for Avif images.
    AvifByteBufferBitmapDecoder byteBufferBitmapDecoder =
        new AvifByteBufferBitmapDecoder(glide.getBitmapPool());
    registry.prepend(
        Registry.BUCKET_BITMAP, ByteBuffer.class, Bitmap.class, byteBufferBitmapDecoder);
    registry.prepend(
        Registry.BUCKET_BITMAP_DRAWABLE,
        ByteBuffer.class,
        BitmapDrawable.class,
        new BitmapDrawableDecoder<>(context.getResources(), byteBufferBitmapDecoder));
    AvifStreamBitmapDecoder streamBitmapDecoder =
        new AvifStreamBitmapDecoder(
            registry.getImageHeaderParsers(), byteBufferBitmapDecoder, glide.getArrayPool());
    registry.prepend(Registry.BUCKET_BITMAP, InputStream.class, Bitmap.class, streamBitmapDecoder);
    registry.prepend(
        Registry.BUCKET_BITMAP_DRAWABLE,
        InputStream.class,
        BitmapDrawable.class,
        new BitmapDrawableDecoder<>(context.getResources(), streamBitmapDecoder));
  }
}
