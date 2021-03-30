package com.bumptech.glide;

import static com.bumptech.glide.testutil.BitmapSubject.assertThat;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.test.GlideApp;
import com.google.common.truth.Truth;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DrawableTransformationTest {
  private Context context;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
  }

  @After
  public void tearDown() {
    Glide.get(context).clearDiskCache();
    Glide.tearDown();
  }

  @Test
  public void load_withColorDrawable_sizeOriginal_optionalTransform_returnsColorDrawable()
      throws ExecutionException, InterruptedException {
    Drawable colorDrawable = new ColorDrawable(Color.RED);
    Drawable result =
        Glide.with(context)
            .load(colorDrawable)
            .apply(new RequestOptions().optionalCenterCrop())
            .submit()
            .get();

    Truth.assertThat(result).isInstanceOf(ColorDrawable.class);
    assertThat(((ColorDrawable) result).getColor()).isEqualTo(Color.RED);
  }

  /** Transformations that do nothing can simply return the original Bitmap. */
  @Test
  public void load_withColorDrawable_fixedSize_requiredUnitTransform_returnsOriginalDrawable()
      throws ExecutionException, InterruptedException {
    Drawable colorDrawable = new ColorDrawable(Color.RED);

    Drawable result =
        Glide.with(context)
            .load(colorDrawable)
            .apply(new RequestOptions().centerCrop())
            .submit(100, 100)
            .get();

    Truth.assertThat(result).isInstanceOf(ColorDrawable.class);
    assertThat(((ColorDrawable) result).getColor()).isEqualTo(Color.RED);
  }

  /**
   * Transformations that produce a different output color/shape/image etc will end up returning a
   * {@link Bitmap} based on the original {@link Drawable} but with the transformation applied.
   */
  @Test
  public void load_withColorDrawable_fixedSize_nonUnitRequiredTransform_returnsBitmapDrawable()
      throws ExecutionException, InterruptedException {
    Drawable colorDrawable = new ColorDrawable(Color.RED);

    Drawable result =
        Glide.with(context)
            .load(colorDrawable)
            .apply(new RequestOptions().circleCrop())
            .submit(100, 100)
            .get();

    Bitmap redSquare = Bitmap.createBitmap(100, 100, Config.ARGB_8888);
    Canvas canvas = new Canvas(redSquare);
    canvas.drawColor(Color.RED);

    BitmapPool bitmapPool = mock(BitmapPool.class);
    when(bitmapPool.get(100, 100, Bitmap.Config.ARGB_8888))
        .thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
    Bitmap expected = TransformationUtils.circleCrop(bitmapPool, redSquare, 100, 100);

    Truth.assertThat(result).isInstanceOf(BitmapDrawable.class);
    Bitmap bitmap = ((BitmapDrawable) result).getBitmap();
    assertThat(bitmap.getWidth()).isEqualTo(100);
    assertThat(bitmap.getHeight()).isEqualTo(100);
    for (int x = 0; x < bitmap.getWidth(); x++) {
      for (int y = 0; y < bitmap.getHeight(); y++) {
        assertThat(bitmap.getPixel(x, y)).isEqualTo(expected.getPixel(x, y));
      }
    }
  }

  @Test
  public void load_withColorDrawable_sizeOriginal_requiredTransform_fails()
      throws ExecutionException, InterruptedException {
    final Drawable colorDrawable = new ColorDrawable(Color.RED);

    // The following section is a hack to workaround a weird behavior where a post in RequestManager
    // can cause a failed request to be started twice in a row if the first attempt happens before.
    // the post. This seems rather unlikely to happen in real applications and it only occurs when
    // the request fails unexpectedly, so we're working around this weird behavior in this test.
    // See #3551.

    // Trigger the Glide application RequestManager to be created.
    Glide.get(context).getRequestManagerRetriever().get(context);
    // Wait until it's added as a lifecycle observer.
    final CountDownLatch latch = new CountDownLatch(1);
    new Handler(Looper.getMainLooper())
        .post(
            new Runnable() {
              @Override
              public void run() {
                latch.countDown();
              }
            });
    latch.await(5, TimeUnit.SECONDS);

    // End hacks.

    assertThrows(
        ExecutionException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            Glide.with(context)
                .load(colorDrawable)
                .apply(new RequestOptions().centerCrop())
                .submit()
                .get();
          }
        });
  }

  @Test
  public void load_withBitmapDrawable_andDoNothingTransformation_doesNotRecycleBitmap()
      throws ExecutionException, InterruptedException {
    Bitmap bitmap = Bitmap.createBitmap(100, 200, Config.ARGB_8888);
    BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);

    Drawable result =
        GlideApp.with(context)
            .load(drawable)
            .fitCenter()
            .override(bitmap.getWidth(), bitmap.getHeight())
            .submit()
            .get();

    assertThat(result).isNotRecycled();
  }

  @Test
  public void load_withBitmapDrawable_andFunctionalTransformation_doesNotRecycleBitmap()
      throws ExecutionException, InterruptedException {
    Bitmap bitmap = Bitmap.createBitmap(100, 200, Config.ARGB_8888);
    BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);

    Drawable result =
        GlideApp.with(context)
            .load(drawable)
            .fitCenter()
            .override(bitmap.getWidth() / 2, bitmap.getHeight() / 2)
            .submit()
            .get();

    assertThat(result).isNotRecycled();
  }

  @Test
  public void load_withColorDrawable_fixedSize_unitBitmapTransform_recyclesIntermediates()
      throws ExecutionException, InterruptedException {
    Drawable colorDrawable = new ColorDrawable(Color.RED);

    int width = 100;
    int height = 200;

    GlideApp.with(context).load(colorDrawable).fitCenter().override(width, height).submit().get();

    BitmapPool bitmapPool = Glide.get(context).getBitmapPool();
    // Make sure we didn't put the same Bitmap twice.
    Bitmap first = bitmapPool.get(width, height, Config.ARGB_8888);
    Bitmap second = bitmapPool.get(width, height, Config.ARGB_8888);

    assertThat(first).isNotSameInstanceAs(second);
  }

  @Test
  public void load_withColorDrawable_fixedSize_functionalBitmapTransform_doesNotRecycleOutput()
      throws ExecutionException, InterruptedException {
    Drawable colorDrawable = new ColorDrawable(Color.RED);

    int width = 100;
    int height = 200;

    Drawable result =
        GlideApp.with(context)
            .load(colorDrawable)
            .circleCrop()
            .override(width, height)
            .submit()
            .get();

    assertThat(result).isNotRecycled();

    BitmapPool bitmapPool = Glide.get(context).getBitmapPool();
    // Make sure we didn't put the same Bitmap twice.
    Bitmap first = bitmapPool.get(width, height, Config.ARGB_8888);
    Bitmap second = bitmapPool.get(width, height, Config.ARGB_8888);

    assertThat(first).isNotSameInstanceAs(second);
  }
}
