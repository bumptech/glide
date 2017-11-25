package com.bumptech.glide;


import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.load.engine.cache.MemoryCacheAdapter;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.test.ConcurrencyHelper;
import com.bumptech.glide.test.GlideApp;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.test.TearDownGlide;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LoadBitmapTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private Context context;

  @Before
  public void setUp() {
    context = InstrumentationRegistry.getTargetContext();
  }

  @Test
  public void clearFromRequestBuilder_asDrawable_withLoadedBitmap_doesNotRecycleBitmap() {
    Glide.init(context, new GlideBuilder()
        .setMemoryCache(new MemoryCacheAdapter())
        .setBitmapPool(new BitmapPoolAdapter()));
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    FutureTarget<Drawable> target =
        concurrency.wait(
            GlideApp.with(context)
                .asDrawable()
                .load(bitmap)
                .submit(100, 100));
    Glide.with(context).clear(target);

    // Allow Glide's resource recycler to run on the main thread.
    concurrency.pokeMainThread();

    assertThat(bitmap.isRecycled()).isFalse();
  }

  @Test
  public void transformFromRequestBuilder_asDrawable_withLoadedBitmap_doesNotRecycleBitmap() {
    Glide.init(context, new GlideBuilder()
        .setMemoryCache(new MemoryCacheAdapter())
        .setBitmapPool(new BitmapPoolAdapter()));
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    concurrency.wait(
        GlideApp.with(context)
            .asDrawable()
            .load(bitmap)
            .centerCrop()
            .submit(100, 100));

    assertThat(bitmap.isRecycled()).isFalse();
  }

  @Test
  public void clearFromRequestManager_withLoadedBitmap_doesNotRecycleBitmap() {
    Glide.init(context, new GlideBuilder()
        .setMemoryCache(new MemoryCacheAdapter())
        .setBitmapPool(new BitmapPoolAdapter()));
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    FutureTarget<Drawable> target =
        concurrency.wait(
            GlideApp.with(context)
                .load(bitmap)
                .submit(100, 100));
    Glide.with(context).clear(target);

    // Allow Glide's resource recycler to run on the main thread.
    concurrency.pokeMainThread();

    assertThat(bitmap.isRecycled()).isFalse();
  }

  @Test
  public void transformFromRequestManager_withLoadedBitmap_doesNotRecycleBitmap() {
    Glide.init(context, new GlideBuilder()
        .setMemoryCache(new MemoryCacheAdapter())
        .setBitmapPool(new BitmapPoolAdapter()));
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    concurrency.wait(
        GlideApp.with(context)
            .load(bitmap)
            .centerCrop()
            .submit(100, 100));

    assertThat(bitmap.isRecycled()).isFalse();
  }

  @Test
  public void clearFromRequestBuilder_withLoadedBitmap_asBitmap_doesNotRecycleBitmap() {
    Glide.init(context, new GlideBuilder()
        .setMemoryCache(new MemoryCacheAdapter())
        .setBitmapPool(new BitmapPoolAdapter()));
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    FutureTarget<Bitmap> target =
        concurrency.wait(
            GlideApp.with(context)
                .asBitmap()
                .load(bitmap)
                .submit(100, 100));
    Glide.with(context).clear(target);

    // Allow Glide's resource recycler to run on the main thread.
    concurrency.pokeMainThread();

    assertThat(bitmap.isRecycled()).isFalse();
  }

  @Test
  public void transformFromRequestBuilder_withLoadedBitmap_asBitmap_doesNotRecycleBitmap() {
    Glide.init(context, new GlideBuilder()
        .setMemoryCache(new MemoryCacheAdapter())
        .setBitmapPool(new BitmapPoolAdapter()));
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    concurrency.wait(
        GlideApp.with(context)
            .asBitmap()
            .load(bitmap)
            .centerCrop()
            .submit(100, 100));

    assertThat(bitmap.isRecycled()).isFalse();
  }

}
