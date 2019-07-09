package com.bumptech.glide;

import static com.bumptech.glide.test.Matchers.anyBitmap;
import static com.bumptech.glide.test.Matchers.anyBitmapTarget;
import static com.bumptech.glide.test.Matchers.anyDrawable;
import static com.bumptech.glide.test.Matchers.anyDrawableTarget;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemoryCacheAdapter;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.MockGlideExecutor;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.test.ConcurrencyHelper;
import com.bumptech.glide.test.GlideApp;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.test.TearDownGlide;
import com.bumptech.glide.util.Util;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class LoadBitmapTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  @Mock private RequestListener<Bitmap> bitmapListener;
  @Mock private RequestListener<Drawable> drawableListener;

  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private Context context;
  private GlideBuilder glideBuilder;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    context = ApplicationProvider.getApplicationContext();

    // Clearing the future here can race with clearing the EngineResource held on to by EngineJob
    // while it's notifying callbacks. Forcing all executors to use the same thread avoids the race
    // by making our clear and EngineJob's clear run on the same thread.
    GlideExecutor mainThreadExecutor = MockGlideExecutor.newMainThreadExecutor();
    glideBuilder =
        new GlideBuilder()
            .setSourceExecutor(mainThreadExecutor)
            .setDiskCacheExecutor(mainThreadExecutor)
            .setAnimationExecutor(mainThreadExecutor);
  }

  @Test
  public void clearFromRequestBuilder_asDrawable_withLoadedBitmap_doesNotRecycleBitmap() {
    Glide.init(
        context,
        new GlideBuilder()
            .setMemoryCache(new MemoryCacheAdapter())
            .setBitmapPool(new BitmapPoolAdapter()));
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    Target<Drawable> target =
        concurrency.wait(GlideApp.with(context).asDrawable().load(bitmap).submit(100, 100));
    Glide.with(context).clear(target);

    // Allow Glide's resource recycler to run on the main thread.
    concurrency.pokeMainThread();

    assertThat(bitmap.isRecycled()).isFalse();
  }

  @Test
  public void transformFromRequestBuilder_asDrawable_withLoadedBitmap_doesNotRecycleBitmap() {
    Glide.init(
        context,
        new GlideBuilder()
            .setMemoryCache(new MemoryCacheAdapter())
            .setBitmapPool(new BitmapPoolAdapter()));
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    concurrency.wait(
        GlideApp.with(context).asDrawable().load(bitmap).centerCrop().submit(100, 100));

    assertThat(bitmap.isRecycled()).isFalse();
  }

  @Test
  public void clearFromRequestManager_withLoadedBitmap_doesNotRecycleBitmap() {
    Glide.init(
        context,
        new GlideBuilder()
            .setMemoryCache(new MemoryCacheAdapter())
            .setBitmapPool(new BitmapPoolAdapter()));
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    Target<Drawable> target =
        concurrency.wait(GlideApp.with(context).load(bitmap).submit(100, 100));
    Glide.with(context).clear(target);

    // Allow Glide's resource recycler to run on the main thread.
    concurrency.pokeMainThread();

    assertThat(bitmap.isRecycled()).isFalse();
  }

  @Test
  public void transformFromRequestManager_withLoadedBitmap_doesNotRecycleBitmap() {
    Glide.init(
        context,
        new GlideBuilder()
            .setMemoryCache(new MemoryCacheAdapter())
            .setBitmapPool(new BitmapPoolAdapter()));
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    concurrency.wait(GlideApp.with(context).load(bitmap).centerCrop().submit(100, 100));

    assertThat(bitmap.isRecycled()).isFalse();
  }

  @Test
  public void clearFromRequestBuilder_withLoadedBitmap_asBitmap_doesNotRecycleBitmap() {
    Glide.init(
        context,
        new GlideBuilder()
            .setMemoryCache(new MemoryCacheAdapter())
            .setBitmapPool(new BitmapPoolAdapter()));
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    Target<Bitmap> target =
        concurrency.wait(GlideApp.with(context).asBitmap().load(bitmap).submit(100, 100));
    Glide.with(context).clear(target);

    // Allow Glide's resource recycler to run on the main thread.
    concurrency.pokeMainThread();

    assertThat(bitmap.isRecycled()).isFalse();
  }

  @Test
  public void transformFromRequestBuilder_withLoadedBitmap_asBitmap_doesNotRecycleBitmap() {
    Glide.init(
        context,
        new GlideBuilder()
            .setMemoryCache(new MemoryCacheAdapter())
            .setBitmapPool(new BitmapPoolAdapter()));
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    concurrency.wait(GlideApp.with(context).asBitmap().load(bitmap).centerCrop().submit(100, 100));

    assertThat(bitmap.isRecycled()).isFalse();
  }

  @Test
  public void loadFromRequestManager_withBitmap_doesNotLoadFromDiskCache() {
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    Glide.init(
        context,
        glideBuilder
            .setMemoryCache(new LruResourceCache(Util.getBitmapByteSize(bitmap) * 10))
            .setBitmapPool(new LruBitmapPool(Util.getBitmapByteSize(bitmap) * 10)));
    Target<Drawable> target =
        concurrency.wait(GlideApp.with(context).load(bitmap).centerCrop().submit(100, 100));
    Glide.with(context).clear(target);

    assertThat(bitmap.isRecycled()).isFalse();

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            Glide.get(context).clearMemory();
          }
        });

    concurrency.wait(
        GlideApp.with(context)
            .load(bitmap)
            .centerCrop()
            .listener(drawableListener)
            .submit(100, 100));

    verify(drawableListener)
        .onResourceReady(
            anyDrawable(), any(), anyDrawableTarget(), eq(DataSource.LOCAL), anyBoolean());
  }

  @Test
  public void loadFromRequestBuilder_asDrawable_withBitmap_doesNotLoadFromDiskCache() {
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    Glide.init(
        context,
        glideBuilder
            .setMemoryCache(new LruResourceCache(Util.getBitmapByteSize(bitmap) * 10))
            .setBitmapPool(new LruBitmapPool(Util.getBitmapByteSize(bitmap) * 10)));
    Target<Drawable> target =
        concurrency.wait(
            GlideApp.with(context).asDrawable().load(bitmap).centerCrop().submit(100, 100));
    Glide.with(context).clear(target);

    assertThat(bitmap.isRecycled()).isFalse();

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            Glide.get(context).clearMemory();
          }
        });

    concurrency.wait(
        GlideApp.with(context)
            .load(bitmap)
            .centerCrop()
            .listener(drawableListener)
            .submit(100, 100));

    verify(drawableListener)
        .onResourceReady(
            anyDrawable(), any(), anyDrawableTarget(), eq(DataSource.LOCAL), anyBoolean());
  }

  @Test
  public void loadFromRequestBuilder_asDrawable_withBitmapAndStrategyBeforeLoad_notFromCache() {
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    Glide.init(
        context,
        glideBuilder
            .setMemoryCache(new LruResourceCache(Util.getBitmapByteSize(bitmap) * 10))
            .setBitmapPool(new LruBitmapPool(Util.getBitmapByteSize(bitmap) * 10)));
    Target<Drawable> target =
        concurrency.wait(
            GlideApp.with(context)
                .asDrawable()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .load(bitmap)
                .centerCrop()
                .submit(100, 100));
    Glide.with(context).clear(target);

    assertThat(bitmap.isRecycled()).isFalse();

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            Glide.get(context).clearMemory();
          }
        });

    concurrency.wait(
        GlideApp.with(context)
            .load(bitmap)
            .centerCrop()
            .listener(drawableListener)
            .submit(100, 100));

    verify(drawableListener)
        .onResourceReady(
            anyDrawable(), any(), anyDrawableTarget(), eq(DataSource.LOCAL), anyBoolean());
  }

  @Test
  public void loadFromRequestBuilder_asBitmap_withBitmap_doesNotLoadFromDiskCache() {
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    Glide.init(
        context,
        glideBuilder
            .setMemoryCache(new LruResourceCache(Util.getBitmapByteSize(bitmap) * 10))
            .setBitmapPool(new LruBitmapPool(Util.getBitmapByteSize(bitmap) * 10)));
    Target<Bitmap> target =
        concurrency.wait(
            GlideApp.with(context).asBitmap().load(bitmap).centerCrop().submit(100, 100));
    Glide.with(context).clear(target);

    assertThat(bitmap.isRecycled()).isFalse();

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            Glide.get(context).clearMemory();
          }
        });

    concurrency.wait(
        GlideApp.with(context)
            .asBitmap()
            .load(bitmap)
            .centerCrop()
            .listener(bitmapListener)
            .submit(100, 100));

    verify(bitmapListener)
        .onResourceReady(anyBitmap(), any(), anyBitmapTarget(), eq(DataSource.LOCAL), anyBoolean());
  }

  @Test
  public void loadFromRequestBuilder_asBitmap_withBitmapAndStrategyBeforeLoad_notFromCache() {
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    Glide.init(
        context,
        glideBuilder
            .setMemoryCache(new LruResourceCache(Util.getBitmapByteSize(bitmap) * 10))
            .setBitmapPool(new LruBitmapPool(Util.getBitmapByteSize(bitmap) * 10)));

    Target<Bitmap> target =
        concurrency.wait(
            GlideApp.with(context)
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .load(bitmap)
                .centerCrop()
                .submit(100, 100));
    Glide.with(context).clear(target);

    assertThat(bitmap.isRecycled()).isFalse();

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            Glide.get(context).clearMemory();
          }
        });

    concurrency.wait(
        GlideApp.with(context)
            .asBitmap()
            .load(bitmap)
            .centerCrop()
            .listener(bitmapListener)
            .submit(100, 100));

    verify(bitmapListener)
        .onResourceReady(anyBitmap(), any(), anyBitmapTarget(), eq(DataSource.LOCAL), anyBoolean());
  }
}
