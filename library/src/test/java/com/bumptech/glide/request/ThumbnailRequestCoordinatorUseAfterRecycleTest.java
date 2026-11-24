package com.bumptech.glide.request;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.load.engine.cache.DiskCacheAdapter;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.MockGlideExecutor;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.testing.junit.testparameterinjector.TestParameter;
import java.io.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestParameterInjector;
import org.robolectric.android.util.concurrent.PausedExecutorService;
import org.robolectric.annotation.LooperMode;

/**
 * Verifies {@link ThumbnailRequestCoordinator} clear and pause-then-clear behavior with {@link
 * GlideBuilder#experimentalSetEnableThumbnailClearAfterPauseFix(boolean)} disabled and enabled.
 */
@RunWith(RobolectricTestParameterInjector.class)
@LooperMode(LooperMode.Mode.PAUSED)
public final class ThumbnailRequestCoordinatorUseAfterRecycleTest {

  private static final int BITMAP_SIZE_PX = 16;
  private static final long MEMORY_CACHE_BYTES = 1024L * 1024L;

  @After
  public void tearDown() {
    Glide.tearDown();
  }

  /**
   * With the fix disabled, {@link ThumbnailRequestCoordinator#clear()} clears {@code thumb} before
   * {@code full}, recycling the thumbnail bitmap before {@code target.onLoadCleared()} runs. With
   * the fix enabled, {@code target.onLoadCleared()} runs before the bitmap is recycled.
   */
  @Test
  public void clearWithoutPause(@TestParameter boolean enableFix) {
    Context context = initGlide(enableFix);
    RequestManager requestManager = Glide.with(context);
    RecordingBitmapTarget target = new RecordingBitmapTarget();

    startChainedLoadWithCompletedThumb(requestManager, target);
    Bitmap thumbBitmap = target.displayedBitmap;
    assertThat(thumbBitmap).isNotNull();

    requestManager.clear(target);

    assertThat(target.onLoadClearedCalls).isEqualTo(1);
    assertThat(target.bitmapAlreadyRecycledAtOnLoadCleared).isEqualTo(!enableFix);
    assertThat(target.displayedBitmap).isNull();
    assertThat(thumbBitmap.isRecycled()).isTrue();
  }

  /**
   * Pausing while the full request is running preserves the thumbnail. A subsequent {@code clear()}
   * skips {@code target.onLoadCleared()} when the fix is disabled (leaving the target holding a
   * recycled bitmap), and delivers the deferred {@code onLoadCleared()} before recycling the bitmap
   * when the fix is enabled.
   */
  @Test
  public void pauseThenClear(@TestParameter boolean enableFix) {
    Context context = initGlide(enableFix);
    RequestManager requestManager = Glide.with(context);
    RecordingBitmapTarget target = new RecordingBitmapTarget();

    startChainedLoadWithCompletedThumb(requestManager, target);
    Bitmap thumbBitmap = target.displayedBitmap;
    assertThat(thumbBitmap).isNotNull();
    assertThat(thumbBitmap.isRecycled()).isFalse();

    requestManager.pauseRequests();
    assertThat(target.onLoadClearedCalls).isEqualTo(0);
    assertThat(target.displayedBitmap).isSameInstanceAs(thumbBitmap);
    assertThat(thumbBitmap.isRecycled()).isFalse();

    requestManager.clear(target);

    if (enableFix) {
      assertThat(target.onLoadClearedCalls).isEqualTo(1);
      assertThat(target.bitmapAlreadyRecycledAtOnLoadCleared).isFalse();
      assertThat(target.displayedBitmap).isNull();
    } else {
      assertThat(target.onLoadClearedCalls).isEqualTo(0);
      assertThat(target.displayedBitmap).isSameInstanceAs(thumbBitmap);
    }
    assertThat(thumbBitmap.isRecycled()).isTrue();
  }

  /** Clearing twice after pause must notify {@code target.onLoadCleared()} at most once. */
  @Test
  public void pauseThenClearTwice(@TestParameter boolean enableFix) {
    Context context = initGlide(enableFix);
    RequestManager requestManager = Glide.with(context);
    RecordingBitmapTarget target = new RecordingBitmapTarget();

    startChainedLoadWithCompletedThumb(requestManager, target);
    Bitmap thumbBitmap = target.displayedBitmap;
    assertThat(thumbBitmap).isNotNull();
    Request request = target.getRequest();
    assertThat(request).isNotNull();

    requestManager.pauseRequests();
    request.clear();
    request.clear();

    if (enableFix) {
      assertThat(target.onLoadClearedCalls).isEqualTo(1);
      assertThat(target.bitmapAlreadyRecycledAtOnLoadCleared).isFalse();
      assertThat(target.displayedBitmap).isNull();
    } else {
      assertThat(target.onLoadClearedCalls).isEqualTo(0);
      assertThat(target.displayedBitmap).isSameInstanceAs(thumbBitmap);
    }
    assertThat(thumbBitmap.isRecycled()).isTrue();
  }

  /**
   * Restarting a cleared request via {@code begin()} resets the notification guard so a subsequent
   * {@code pause()} followed by {@code clear()} notifies {@code target.onLoadCleared()} again when
   * the fix is enabled.
   */
  @Test
  public void clearThenRestartThenClear(@TestParameter boolean enableFix) {
    Context context = initGlide(enableFix);
    RequestManager requestManager = Glide.with(context);
    RecordingBitmapTarget target = new RecordingBitmapTarget();

    startChainedLoadWithCompletedThumb(requestManager, target);
    Request request = target.getRequest();
    assertThat(request).isNotNull();

    requestManager.pauseRequests();
    request.clear();
    assertThat(target.onLoadClearedCalls).isEqualTo(enableFix ? 1 : 0);

    request.begin();
    shadowOf(Looper.getMainLooper()).idle();
    Bitmap restartedBitmap = target.displayedBitmap;
    assertThat(restartedBitmap).isNotNull();
    assertThat(restartedBitmap.isRecycled()).isFalse();

    request.pause();
    request.clear();
    if (enableFix) {
      assertThat(target.onLoadClearedCalls).isEqualTo(2);
      assertThat(target.bitmapAlreadyRecycledAtOnLoadCleared).isFalse();
      assertThat(target.displayedBitmap).isNull();
    } else {
      assertThat(target.onLoadClearedCalls).isEqualTo(0);
      assertThat(target.displayedBitmap).isSameInstanceAs(restartedBitmap);
    }
    assertThat(restartedBitmap.isRecycled()).isTrue();
  }

  private void startChainedLoadWithCompletedThumb(
      RequestManager requestManager, RecordingBitmapTarget target) {
    // Use separate byte[] instances so full and thumb produce distinct EngineKeys and do not
    // coalesce onto the same EngineJob. Encoding as PNG (rather than passing a Bitmap model, which
    // UnitBitmapDecoder wraps in a non-recycled resource) produces a real BitmapResource owned by
    // BitmapPoolAdapter so releasing it recycles the bitmap.
    //
    // Setting useUnlimitedSourceGeneratorsPool(true) routes the full request to the paused
    // unlimited executor (see initGlide), keeping full in-flight while the thumbnail decodes
    // immediately on the direct source executor (including after request.begin() restarts).
    RecordingBitmapTarget unused =
        requestManager
            .asBitmap()
            .load(createPngBytes())
            .useUnlimitedSourceGeneratorsPool(true)
            .thumbnail(requestManager.asBitmap().load(createPngBytes()))
            .into(target);
    shadowOf(Looper.getMainLooper()).idle();
  }

  private static byte[] createPngBytes() {
    Bitmap bitmap = Bitmap.createBitmap(BITMAP_SIZE_PX, BITMAP_SIZE_PX, Bitmap.Config.ARGB_8888);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, /* quality= */ 100, out);
    return out.toByteArray();
  }

  private Context initGlide(boolean enableFix) {
    Context context = ApplicationProvider.getApplicationContext();
    GlideExecutor executor = MockGlideExecutor.newMainThreadExecutor();
    // Intentionally never drained so full requests submitted via
    // useUnlimitedSourceGeneratorsPool(true) stay queued and in-flight.
    GlideExecutor pausedExecutor = MockGlideExecutor.newTestExecutor(new PausedExecutorService());
    GlideBuilder builder =
        new GlideBuilder()
            .setBitmapPool(new BitmapPoolAdapter())
            .setMemoryCache(new LruResourceCache(MEMORY_CACHE_BYTES))
            .setDiskCache(new DiskCacheAdapter.Factory())
            .setSourceExecutor(executor)
            .setSourceUnlimitedExecutor(pausedExecutor)
            .setDiskCacheExecutor(executor)
            .setAnimationExecutor(executor);
    if (enableFix) {
      builder.experimentalSetEnableThumbnailClearAfterPauseFix(true);
    }
    Glide.init(context, builder);
    return context;
  }

  /** Simulates a View/Drawable target that drops its Bitmap on clear. */
  private static final class RecordingBitmapTarget extends CustomTarget<Bitmap> {
    Bitmap displayedBitmap;
    int onLoadClearedCalls;
    boolean bitmapAlreadyRecycledAtOnLoadCleared;

    RecordingBitmapTarget() {
      super(BITMAP_SIZE_PX, BITMAP_SIZE_PX);
    }

    @Override
    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
      displayedBitmap = resource;
    }

    @Override
    public void onLoadCleared(Drawable placeholder) {
      onLoadClearedCalls++;
      if (displayedBitmap != null && displayedBitmap.isRecycled()) {
        // TODO: Throw an exception once the fix is rolled out and the flag is removed.
        bitmapAlreadyRecycledAtOnLoadCleared = true;
      }
      displayedBitmap = null;
    }
  }
}
