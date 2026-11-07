package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.MockGlideExecutor;
import com.bumptech.glide.request.FutureTarget;
import com.google.testing.junit.testparameterinjector.TestParameter;
import java.io.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestParameterInjector;

/**
 * Tests caching behavior when loading in-memory {@link Bitmap}, {@link Drawable}, and {@code
 * byte[]} models.
 */
@RunWith(RobolectricTestParameterInjector.class)
public final class CachingTest {

  private Context context;
  private DiskCache.Factory diskCacheFactory;
  private DiskCache diskCache;
  private MemoryCache memoryCache;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    initGlide(/* useTypedLoadForInMemoryObjects= */ true);
  }

  private void initGlide(boolean useTypedLoadForInMemoryObjects) {
    diskCacheFactory = mock(DiskCache.Factory.class);
    diskCache = mock(DiskCache.class);
    memoryCache = mock(MemoryCache.class);
    when(diskCacheFactory.build()).thenReturn(diskCache);
    GlideExecutor executor = MockGlideExecutor.newMainThreadExecutor();
    Glide.init(
        context,
        new GlideBuilder()
            .setDiskCache(diskCacheFactory)
            .setMemoryCache(memoryCache)
            .setSourceExecutor(executor)
            .setDiskCacheExecutor(executor)
            .setAnimationExecutor(executor)
            .experimentalSetUseTypedLoadForInMemoryObjects(useTypedLoadForInMemoryObjects));
  }

  @After
  public void tearDown() {
    Glide.tearDown();
  }

  @Test
  public void loadBitmap_asDrawable_doesNotInvokeDiskCache(@TestParameter boolean castToObject)
      throws Exception {
    Bitmap bitmap = Bitmap.createBitmap(7, 7, Bitmap.Config.ARGB_8888);

    FutureTarget<Drawable> future;
    if (castToObject) {
      future = Glide.with(context).load((Object) bitmap).submit();
    } else {
      future = Glide.with(context).load(bitmap).submit();
    }
    Drawable loaded = future.get();
    Glide.with(context).clear(future);

    assertThat(loaded).isInstanceOf(BitmapDrawable.class);
    assertThat(((BitmapDrawable) loaded).getBitmap()).isSameInstanceAs(bitmap);
    verifyNoInteractions(diskCacheFactory, diskCache);
  }

  @Test
  public void loadBitmap_asBitmap_doesNotInvokeDiskCache(@TestParameter boolean castToObject)
      throws Exception {
    Bitmap bitmap = Bitmap.createBitmap(7, 7, Bitmap.Config.ARGB_8888);

    FutureTarget<Bitmap> future;
    if (castToObject) {
      future = Glide.with(context).asBitmap().load((Object) bitmap).submit();
    } else {
      future = Glide.with(context).asBitmap().load(bitmap).submit();
    }
    Bitmap loaded = future.get();
    Glide.with(context).clear(future);

    assertThat(loaded).isSameInstanceAs(bitmap);
    verifyNoInteractions(diskCacheFactory, diskCache);
  }

  @Test
  public void loadDrawable_doesNotInvokeDiskCache(@TestParameter boolean castToObject)
      throws Exception {
    Drawable drawable = context.getDrawable(android.R.drawable.star_on);

    FutureTarget<Drawable> future;
    if (castToObject) {
      future = Glide.with(context).load((Object) drawable).submit();
    } else {
      future = Glide.with(context).load(drawable).submit();
    }
    Drawable loaded = future.get();
    Glide.with(context).clear(future);

    assertThat(loaded.getConstantState()).isSameInstanceAs(drawable.getConstantState());
    verifyNoInteractions(diskCacheFactory, diskCache);
  }

  @Test
  public void loadByteArray_doesNotInvokeCaches(@TestParameter boolean castToObject)
      throws Exception {
    byte[] bytes = createPngBytes();

    FutureTarget<Drawable> future;
    if (castToObject) {
      future = Glide.with(context).load((Object) bytes).submit();
    } else {
      future = Glide.with(context).load(bytes).submit();
    }
    Drawable loaded = future.get();
    Glide.with(context).clear(future);

    assertThat(loaded).isInstanceOf(BitmapDrawable.class);
    verifyNoInteractions(diskCacheFactory, diskCache);
    verify(memoryCache, never()).remove(any());
    verify(memoryCache, never()).put(any(), any());
  }

  @Test
  public void loadBitmapAsObject_experimentDisabled_invokesDiskCache() throws Exception {
    Glide.tearDown();
    initGlide(/* useTypedLoadForInMemoryObjects= */ false);
    Bitmap bitmap = Bitmap.createBitmap(7, 7, Bitmap.Config.ARGB_8888);

    FutureTarget<Drawable> future = Glide.with(context).load((Object) bitmap).submit();
    Drawable loaded = future.get();
    Glide.with(context).clear(future);

    assertThat(loaded).isInstanceOf(BitmapDrawable.class);
    assertThat(((BitmapDrawable) loaded).getBitmap()).isSameInstanceAs(bitmap);
    verify(diskCache, atLeastOnce()).get(any());
  }

  @Test
  public void loadDrawableAsObject_experimentDisabled_invokesDiskCache() throws Exception {
    Glide.tearDown();
    initGlide(/* useTypedLoadForInMemoryObjects= */ false);
    Drawable drawable = context.getDrawable(android.R.drawable.star_on);

    FutureTarget<Drawable> future = Glide.with(context).load((Object) drawable).submit();
    Drawable loaded = future.get();
    Glide.with(context).clear(future);

    assertThat(loaded.getConstantState()).isSameInstanceAs(drawable.getConstantState());
    verify(diskCache, atLeastOnce()).get(any());
  }

  @Test
  public void loadByteArrayAsObject_experimentDisabled_invokesCaches() throws Exception {
    Glide.tearDown();
    initGlide(/* useTypedLoadForInMemoryObjects= */ false);
    byte[] bytes = createPngBytes();

    FutureTarget<Drawable> future = Glide.with(context).load((Object) bytes).submit();
    Drawable loaded = future.get();
    Glide.with(context).clear(future);

    assertThat(loaded).isInstanceOf(BitmapDrawable.class);
    verify(diskCache, atLeastOnce()).get(any());
    verify(memoryCache).remove(any());
    verify(memoryCache).put(any(), any());
  }

  private static byte[] createPngBytes() {
    Bitmap bitmap = Bitmap.createBitmap(7, 7, Bitmap.Config.ARGB_8888);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, /* quality= */ 100, out);
    return out.toByteArray();
  }
}
