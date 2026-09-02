package com.bumptech.glide;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentCallbacks2;
import android.content.Context;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.tests.TearDownGlide;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Integration tests for Glide Experiments. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public final class GlideExperimentsTest {

  @Rule public TearDownGlide tearDownGlide = new TearDownGlide();

  private Context context;
  private static final int INITIAL_CACHE_SIZE = 1000;
  private static final int INITIAL_POOL_SIZE = 1000;
  private MemoryCache memoryCache;
  private BitmapPool bitmapPool;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    memoryCache = new LruResourceCache(INITIAL_CACHE_SIZE);
    bitmapPool = new LruBitmapPool(INITIAL_POOL_SIZE);
  }

  private Glide createGlide(
      boolean enableTrimMemoryOnUiHidden, @Nullable MemoryCategory memoryCategoryInBackground) {
    return new GlideBuilder()
        .setBitmapPool(bitmapPool)
        .setMemoryCache(memoryCache)
        .setMemoryCategoryInBackground(memoryCategoryInBackground)
        .experimentalSetEnableTrimMemoryOnUiHidden(enableTrimMemoryOnUiHidden)
        .build(context, Collections.emptyList(), /* annotationGeneratedGlideModule= */ null);
  }

  @Test
  public void testTrimMemoryOnUiHidden_experimentEnabled_setsMemoryCategory() {
    Glide glide = createGlide(/* enableTrimMemoryOnUiHidden= */ true, MemoryCategory.ZERO);

    glide.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);

    assertThat(memoryCache.getMaxSize()).isEqualTo(0);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(0);
  }

  @Test
  public void testTrimMemoryOnUiHidden_experimentDisabled_doesNotSetMemoryCategory() {
    Glide glide = createGlide(/* enableTrimMemoryOnUiHidden= */ false, MemoryCategory.ZERO);

    glide.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);

    assertThat(memoryCache.getMaxSize()).isEqualTo(INITIAL_CACHE_SIZE);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(INITIAL_POOL_SIZE);
  }

  @Test
  public void testTrimMemoryBackground_experimentDisabled_setsMemoryCategory() {
    Glide glide = createGlide(/* enableTrimMemoryOnUiHidden= */ false, MemoryCategory.ZERO);

    glide.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);

    assertThat(memoryCache.getMaxSize()).isEqualTo(0);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(0);
  }

  @Test
  public void testTrimMemoryBackground_experimentEnabled_setsMemoryCategory() {
    Glide glide = createGlide(/* enableTrimMemoryOnUiHidden= */ true, MemoryCategory.ZERO);

    glide.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);

    assertThat(memoryCache.getMaxSize()).isEqualTo(0);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(0);
  }

  @Test
  public void testTrimMemoryRunningCritical_experimentEnabled_doesNotChangeMultiplier() {
    Glide glide = createGlide(/* enableTrimMemoryOnUiHidden= */ true, MemoryCategory.ZERO);

    glide.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL);

    assertThat(memoryCache.getMaxSize()).isEqualTo(INITIAL_CACHE_SIZE);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(INITIAL_POOL_SIZE);
  }

  @Test
  public void testTrimMemoryOnUiHidden_withoutMemoryCategory_doesNotChangeMultiplier() {
    Glide glide =
        createGlide(/* enableTrimMemoryOnUiHidden= */ true, /* memoryCategoryInBackground= */ null);

    glide.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);

    assertThat(memoryCache.getMaxSize()).isEqualTo(INITIAL_CACHE_SIZE);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(INITIAL_POOL_SIZE);
  }

  @Test
  public void testTrimMemoryOnUiHidden_foregroundRestoresMemoryCategory() {
    Glide glide = createGlide(/* enableTrimMemoryOnUiHidden= */ true, MemoryCategory.LOW);

    glide.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);
    assertThat(memoryCache.getMaxSize())
        .isEqualTo(Math.round(INITIAL_CACHE_SIZE * MemoryCategory.LOW.getMultiplier()));
    assertThat(bitmapPool.getMaxSize())
        .isEqualTo(Math.round(INITIAL_POOL_SIZE * MemoryCategory.LOW.getMultiplier()));

    glide.setMemoryCategoryWhenInForeground();
    assertThat(memoryCache.getMaxSize()).isEqualTo(INITIAL_CACHE_SIZE);
    assertThat(bitmapPool.getMaxSize()).isEqualTo(INITIAL_POOL_SIZE);
  }
}
