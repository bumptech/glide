package com.bumptech.glide;

import static com.bumptech.glide.test.GlideOptions.skipMemoryCacheOf;
import static com.bumptech.glide.test.Matchers.anyDrawable;
import static com.bumptech.glide.test.Matchers.anyDrawableTarget;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.AbsListView.LayoutParams;
import android.widget.ImageView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.MockGlideExecutor;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.test.BitmapSubject;
import com.bumptech.glide.test.ConcurrencyHelper;
import com.bumptech.glide.test.GlideApp;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.test.TearDownGlide;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class LoadBytesTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();

  @Mock private RequestListener<Drawable> requestListener;

  private Context context;
  private ImageView imageView;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    context = ApplicationProvider.getApplicationContext();

    imageView = new ImageView(context);
    int[] dimensions = getCanonicalDimensions();
    imageView.setLayoutParams(new LayoutParams(/*w=*/ dimensions[0], /*h=*/ dimensions[1]));

    // Writes to the resource disk cache run in a non-blocking manner after the Target is notified.
    // Unless we enforce a single threaded executor, the encode task races with our second decode
    // task, causing the test to sometimes fail (when the second resource is started after the
    // encode and loaded from the disk cache) and sometimes succeed (when the second resource is
    // started before the encode and loads from source).
    ExecutorService executor = Executors.newSingleThreadExecutor();
    GlideExecutor glideExecutor = MockGlideExecutor.newTestExecutor(executor);
    Glide.init(
        context,
        new GlideBuilder()
            .setAnimationExecutor(glideExecutor)
            .setDiskCacheExecutor(glideExecutor)
            .setSourceExecutor(glideExecutor));
  }

  @Test
  public void loadFromRequestManager_intoImageView_withDifferentByteArrays_loadsDifferentImages()
      throws IOException, ExecutionException, InterruptedException {
    final byte[] canonicalBytes = getCanonicalBytes();
    final byte[] modifiedBytes = getModifiedBytes();

    concurrency.loadOnMainThread(Glide.with(context).load(canonicalBytes), imageView);
    Bitmap firstBitmap = copyFromImageViewDrawable(imageView);

    concurrency.loadOnMainThread(Glide.with(context).load(modifiedBytes), imageView);
    Bitmap secondBitmap = copyFromImageViewDrawable(imageView);

    // This assertion alone doesn't catch the case where the second Bitmap is loaded from the result
    // cache of the data from the first Bitmap.
    BitmapSubject.assertThat(firstBitmap).isNotSameInstanceAs(secondBitmap);

    Bitmap expectedCanonicalBitmap =
        BitmapFactory.decodeByteArray(canonicalBytes, /*offset=*/ 0, canonicalBytes.length);
    BitmapSubject.assertThat(firstBitmap).sameAs(expectedCanonicalBitmap);

    Bitmap expectedModifiedBitmap =
        BitmapFactory.decodeByteArray(modifiedBytes, /*offset=*/ 0, modifiedBytes.length);
    BitmapSubject.assertThat(secondBitmap).sameAs(expectedModifiedBitmap);
  }

  @Test
  public void loadFromRequestBuilder_intoImageView_withDifferentByteArrays_loadsDifferentImages()
      throws IOException, ExecutionException, InterruptedException {
    final byte[] canonicalBytes = getCanonicalBytes();
    final byte[] modifiedBytes = getModifiedBytes();

    concurrency.loadOnMainThread(
        GlideApp.with(context).asDrawable().load(canonicalBytes), imageView);
    Bitmap firstBitmap = copyFromImageViewDrawable(imageView);

    concurrency.loadOnMainThread(
        GlideApp.with(context).asDrawable().load(modifiedBytes), imageView);
    Bitmap secondBitmap = copyFromImageViewDrawable(imageView);

    // This assertion alone doesn't catch the case where the second Bitmap is loaded from the result
    // cache of the data from the first Bitmap.
    BitmapSubject.assertThat(firstBitmap).isNotSameInstanceAs(secondBitmap);

    Bitmap expectedCanonicalBitmap =
        BitmapFactory.decodeByteArray(canonicalBytes, /*offset=*/ 0, canonicalBytes.length);
    BitmapSubject.assertThat(firstBitmap).sameAs(expectedCanonicalBitmap);

    Bitmap expectedModifiedBitmap =
        BitmapFactory.decodeByteArray(modifiedBytes, /*offset=*/ 0, modifiedBytes.length);
    BitmapSubject.assertThat(secondBitmap).sameAs(expectedModifiedBitmap);
  }

  @Test
  public void requestManager_intoImageView_withSameByteArrayAndMemoryCacheEnabled_loadsFromMemory()
      throws IOException {
    final byte[] canonicalBytes = getCanonicalBytes();
    concurrency.loadOnMainThread(
        Glide.with(context).load(canonicalBytes).apply(skipMemoryCacheOf(false)), imageView);

    Glide.with(context).clear(imageView);

    concurrency.loadOnMainThread(
        Glide.with(context)
            .load(canonicalBytes)
            .listener(requestListener)
            .apply(skipMemoryCacheOf(false)),
        imageView);

    verify(requestListener)
        .onResourceReady(
            anyDrawable(), any(), anyDrawableTarget(), eq(DataSource.MEMORY_CACHE), anyBoolean());
  }

  @Test
  public void requestBuilder_intoImageView_withSameByteArrayAndMemoryCacheEnabled_loadsFromMemory()
      throws IOException {
    final byte[] canonicalBytes = getCanonicalBytes();
    concurrency.loadOnMainThread(
        Glide.with(context).asDrawable().load(canonicalBytes).apply(skipMemoryCacheOf(false)),
        imageView);

    Glide.with(context).clear(imageView);

    concurrency.loadOnMainThread(
        Glide.with(context)
            .asDrawable()
            .load(canonicalBytes)
            .listener(requestListener)
            .apply(skipMemoryCacheOf(false)),
        imageView);

    verify(requestListener)
        .onResourceReady(
            anyDrawable(), any(), anyDrawableTarget(), eq(DataSource.MEMORY_CACHE), anyBoolean());
  }

  @Test
  public void loadFromRequestManager_withSameByteArray_validDiskCacheStrategy_returnsFromDiskCache()
      throws IOException {
    byte[] data = getCanonicalBytes();
    Target<Drawable> target =
        concurrency.wait(
            GlideApp.with(context)
                .load(data)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .submit());
    GlideApp.with(context).clear(target);

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            GlideApp.get(context).clearMemory();
          }
        });

    concurrency.wait(
        GlideApp.with(context)
            .load(data)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .listener(requestListener)
            .submit());

    verify(requestListener)
        .onResourceReady(
            anyDrawable(),
            any(),
            anyDrawableTarget(),
            eq(DataSource.RESOURCE_DISK_CACHE),
            anyBoolean());
  }

  @Test
  public void loadFromRequestBuilder_withSameByteArray_validDiskCacheStrategy_returnsFromDiskCache()
      throws IOException {
    byte[] data = getCanonicalBytes();
    Target<Drawable> target =
        concurrency.wait(
            GlideApp.with(context)
                .asDrawable()
                .load(data)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .submit());
    GlideApp.with(context).clear(target);

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            GlideApp.get(context).clearMemory();
          }
        });

    concurrency.wait(
        GlideApp.with(context)
            .asDrawable()
            .load(data)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .listener(requestListener)
            .submit());

    verify(requestListener)
        .onResourceReady(
            anyDrawable(),
            any(),
            anyDrawableTarget(),
            eq(DataSource.RESOURCE_DISK_CACHE),
            anyBoolean());
  }

  @Test
  public void loadFromRequestManager_withSameByteArray_memoryCacheEnabled_returnsFromCache()
      throws IOException {
    byte[] data = getCanonicalBytes();
    Target<Drawable> target =
        concurrency.wait(GlideApp.with(context).load(data).skipMemoryCache(false).submit());
    GlideApp.with(context).clear(target);

    concurrency.wait(
        GlideApp.with(context)
            .load(data)
            .skipMemoryCache(false)
            .listener(requestListener)
            .submit());

    verify(requestListener)
        .onResourceReady(
            anyDrawable(), any(), anyDrawableTarget(), eq(DataSource.MEMORY_CACHE), anyBoolean());
  }

  @Test
  public void loadFromRequestBuilder_withSameByteArray_memoryCacheEnabled_returnsFromCache()
      throws IOException {
    byte[] data = getCanonicalBytes();
    Target<Drawable> target =
        concurrency.wait(
            GlideApp.with(context).asDrawable().load(data).skipMemoryCache(false).submit());
    GlideApp.with(context).clear(target);

    concurrency.wait(
        GlideApp.with(context)
            .asDrawable()
            .load(data)
            .skipMemoryCache(false)
            .listener(requestListener)
            .submit());

    verify(requestListener)
        .onResourceReady(
            anyDrawable(), any(), anyDrawableTarget(), eq(DataSource.MEMORY_CACHE), anyBoolean());
  }

  @Test
  public void loadFromRequestManager_withSameByteArray_returnsFromLocal() throws IOException {
    byte[] data = getCanonicalBytes();
    Target<Drawable> target = concurrency.wait(GlideApp.with(context).load(data).submit());
    GlideApp.with(context).clear(target);

    concurrency.wait(GlideApp.with(context).load(data).listener(requestListener).submit());

    verify(requestListener)
        .onResourceReady(
            anyDrawable(), any(), anyDrawableTarget(), eq(DataSource.LOCAL), anyBoolean());
  }

  @Test
  public void loadFromRequestBuilder_withSameByteArray_returnsFromLocal() throws IOException {
    byte[] data = getCanonicalBytes();
    Target<Drawable> target =
        concurrency.wait(GlideApp.with(context).asDrawable().load(data).submit());
    GlideApp.with(context).clear(target);

    concurrency.wait(
        GlideApp.with(context).asDrawable().load(data).listener(requestListener).submit());

    verify(requestListener)
        .onResourceReady(
            anyDrawable(), any(), anyDrawableTarget(), eq(DataSource.LOCAL), anyBoolean());
  }

  @Test
  public void loadFromRequestManager_withSameByteArrayAndMissingFromMemory_returnsFromLocal()
      throws IOException {
    byte[] data = getCanonicalBytes();
    Target<Drawable> target = concurrency.wait(GlideApp.with(context).load(data).submit());
    GlideApp.with(context).clear(target);

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            GlideApp.get(context).clearMemory();
          }
        });

    concurrency.wait(GlideApp.with(context).load(data).listener(requestListener).submit());

    verify(requestListener)
        .onResourceReady(
            anyDrawable(), any(), anyDrawableTarget(), eq(DataSource.LOCAL), anyBoolean());
  }

  @Test
  public void loadFromRequestBuilder_withSameByteArrayAndMissingFromMemory_returnsFromLocal()
      throws IOException {
    byte[] data = getCanonicalBytes();
    Target<Drawable> target =
        concurrency.wait(GlideApp.with(context).asDrawable().load(data).submit());
    GlideApp.with(context).clear(target);

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            GlideApp.get(context).clearMemory();
          }
        });

    concurrency.wait(
        GlideApp.with(context).asDrawable().load(data).listener(requestListener).submit());

    verify(requestListener)
        .onResourceReady(
            anyDrawable(), any(), anyDrawableTarget(), eq(DataSource.LOCAL), anyBoolean());
  }

  @Test
  public void loadFromBuilder_withDiskCacheStrategySetBeforeLoad_doesNotOverrideDiskCacheStrategy()
      throws IOException {
    byte[] data = getCanonicalBytes();
    concurrency.wait(
        GlideApp.with(context)
            .asDrawable()
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .load(data)
            .submit());

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            GlideApp.get(context).clearMemory();
          }
        });

    concurrency.wait(
        GlideApp.with(context)
            .asDrawable()
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .listener(requestListener)
            .load(data)
            .submit());

    verify(requestListener)
        .onResourceReady(
            anyDrawable(),
            any(),
            anyDrawableTarget(),
            eq(DataSource.RESOURCE_DISK_CACHE),
            anyBoolean());
  }

  @Test
  public void loadFromBuilder_withSkipMemoryCacheSetBeforeLoad_doesNotOverrideSkipMemoryCache()
      throws IOException {
    byte[] data = getCanonicalBytes();
    concurrency.wait(
        GlideApp.with(context).asDrawable().skipMemoryCache(false).load(data).submit());

    concurrency.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            GlideApp.get(context).clearMemory();
          }
        });

    concurrency.wait(
        GlideApp.with(context)
            .asDrawable()
            .skipMemoryCache(false)
            .listener(requestListener)
            .load(data)
            .submit());

    verify(requestListener)
        .onResourceReady(
            anyDrawable(), any(), anyDrawableTarget(), eq(DataSource.MEMORY_CACHE), anyBoolean());
  }

  @Test
  public void loadFromBuilder_withDataDiskCacheStrategy_returnsFromSource() throws IOException {
    byte[] data = getCanonicalBytes();

    concurrency.wait(
        GlideApp.with(context)
            .asDrawable()
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .load(data)
            .submit());

    concurrency.wait(
        GlideApp.with(context)
            .asDrawable()
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .skipMemoryCache(true)
            .load(data)
            .listener(requestListener)
            .submit());

    verify(requestListener)
        .onResourceReady(
            anyDrawable(),
            any(),
            anyDrawableTarget(),
            eq(DataSource.DATA_DISK_CACHE),
            anyBoolean());
  }

  private Bitmap copyFromImageViewDrawable(ImageView imageView) {
    if (imageView.getDrawable() == null) {
      fail("Drawable unexpectedly null");
    }

    // Glide mutates Bitmaps, so it's possible that a Bitmap loaded into a View in one place may
    // be re-used to load a different image later. Create a defensive copy just in case.
    return Bitmap.createBitmap(((BitmapDrawable) imageView.getDrawable()).getBitmap());
  }

  private int[] getCanonicalDimensions() throws IOException {
    byte[] canonicalBytes = getCanonicalBytes();
    Bitmap bitmap =
        BitmapFactory.decodeByteArray(canonicalBytes, /*offset=*/ 0, canonicalBytes.length);
    return new int[] {bitmap.getWidth(), bitmap.getHeight()};
  }

  private byte[] getModifiedBytes() throws IOException {
    int[] dimensions = getCanonicalDimensions();
    Bitmap bitmap = Bitmap.createBitmap(dimensions[0], dimensions[1], Config.ARGB_8888);
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    bitmap.compress(CompressFormat.PNG, /*quality=*/ 100, os);
    return os.toByteArray();
  }

  private byte[] getCanonicalBytes() throws IOException {
    int resourceId = ResourceIds.raw.canonical;
    Resources resources = context.getResources();
    InputStream is = resources.openRawResource(resourceId);
    return ByteStreams.toByteArray(is);
  }
}
