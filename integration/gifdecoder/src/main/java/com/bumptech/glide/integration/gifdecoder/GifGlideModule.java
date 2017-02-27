package com.bumptech.glide.integration.gifdecoder;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.model.UnitModelLoader;
import com.bumptech.glide.module.LibraryGlideModule;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Registers Gif related options and components.
 */
@GlideModule
public class GifGlideModule extends LibraryGlideModule {
  @Override
  public void registerComponents(Context context, Registry registry) {
    BitmapPool bitmapPool = Glide.get(context).getBitmapPool();
    ArrayPool arrayPool = Glide.get(context).getArrayPool();
    ByteBufferGifDecoder byteBufferGifDecoder =
        new ByteBufferGifDecoder(context, registry.getImageHeaderParsers(), bitmapPool, arrayPool);
    registry
        .prepend(InputStream.class, GifDrawable.class,
            new StreamGifDecoder(registry.getImageHeaderParsers(), byteBufferGifDecoder, arrayPool))
        .prepend(ByteBuffer.class, GifDrawable.class, byteBufferGifDecoder)
        .register(GifDrawable.class, new GifDrawableEncoder())
        /* GIF Frames */
        .append(GifDecoder.class, GifDecoder.class, new UnitModelLoader.Factory<GifDecoder>())
        .append(GifDecoder.class, Bitmap.class, new GifFrameResourceDecoder(bitmapPool))
        .register(GifDrawable.class, byte[].class, new GifDrawableBytesTranscoder());
  }
}
