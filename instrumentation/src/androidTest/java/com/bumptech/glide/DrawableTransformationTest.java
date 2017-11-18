package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
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
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.test.BitmapSubject;
import com.bumptech.glide.test.GlideApp;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DrawableTransformationTest {
  @Rule public final ExpectedException expectedException = ExpectedException.none();

  private Context context;

  @Before
  public void setUp() {
    context = InstrumentationRegistry.getTargetContext();
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
    Drawable result = Glide.with(context)
        .load(colorDrawable)
        .apply(new RequestOptions()
            .optionalCenterCrop())
        .submit()
        .get();

    assertThat(result).isInstanceOf(ColorDrawable.class);
    assertThat(((ColorDrawable) result).getColor()).isEqualTo(Color.RED);
  }

  /**
   * Transformations that do nothing can simply return the original Bitmap.
   */
  @Test
  public void load_withColorDrawable_fixedSize_requiredUnitTransform_returnsOriginalDrawable()
      throws ExecutionException, InterruptedException {
    Drawable colorDrawable = new ColorDrawable(Color.RED);

    Drawable result = Glide.with(context)
        .load(colorDrawable)
        .apply(new RequestOptions()
            .centerCrop())
        .submit(100, 100)
        .get();

    assertThat(result).isInstanceOf(ColorDrawable.class);
    assertThat(((ColorDrawable) result).getColor()).isEqualTo(Color.RED);
  }

  /**
   * Transformations that produce a different output color/shape/image etc will end up returning
   * a {@link Bitmap} based on the original {@link Drawable} but with the transformation applied.
   */
  @Test
  public void load_withColorDrawable_fixedSize_nonUnitRequiredTransform_returnsBitmapDrawable()
      throws ExecutionException, InterruptedException {
    Drawable colorDrawable = new ColorDrawable(Color.RED);

    Drawable result = Glide.with(context)
        .load(colorDrawable)
        .apply(new RequestOptions()
            .circleCrop())
        .submit(100, 100)
        .get();

    Bitmap redSquare = Bitmap.createBitmap(100, 100, Config.ARGB_8888);
    Canvas canvas = new Canvas(redSquare);
    canvas.drawColor(Color.RED);

    BitmapPool bitmapPool = mock(BitmapPool.class);
    when(bitmapPool.get(100, 100, Bitmap.Config.ARGB_8888))
        .thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
    Bitmap expected = TransformationUtils.circleCrop(bitmapPool, redSquare, 100, 100);

    assertThat(result).isInstanceOf(BitmapDrawable.class);
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
    Drawable colorDrawable = new ColorDrawable(Color.RED);

    expectedException.expect(ExecutionException.class);
    Glide.with(context)
        .load(colorDrawable)
        .apply(new RequestOptions()
            .centerCrop())
        .submit()
        .get();
  }

  @Test
  public void load_withBitmapDrawable_andDoNothingTransformation_doesNotRecycleBitmap()
      throws ExecutionException, InterruptedException {
    Bitmap bitmap = Bitmap.createBitmap(100, 200, Config.ARGB_8888);
    BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);

    Drawable result = GlideApp.with(context)
        .load(drawable)
        .fitCenter()
        .override(bitmap.getWidth(), bitmap.getHeight())
        .submit()
        .get();

    BitmapSubject.assertThat(result).isNotRecycled();
  }

  @Test
  public void load_withBitmapDrawable_andFunctionalTransformation_doesNotRecycleBitmap()
      throws ExecutionException, InterruptedException {
      Bitmap bitmap = Bitmap.createBitmap(100, 200, Config.ARGB_8888);
    BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);

    Drawable result = GlideApp.with(context)
        .load(drawable)
        .fitCenter()
        .override(bitmap.getWidth() / 2, bitmap.getHeight() / 2)
        .submit()
        .get();

    BitmapSubject.assertThat(result).isNotRecycled();
  }

  @Test
  public void load_withColorDrawable_fixedSize_unitBitmapTransform_recyclesIntermediates()
      throws ExecutionException, InterruptedException {
    Drawable colorDrawable = new ColorDrawable(Color.RED);

    int width = 100;
    int height = 200;

    GlideApp.with(context)
        .load(colorDrawable)
        .fitCenter()
        .override(width, height)
        .submit()
        .get();

    BitmapPool bitmapPool = Glide.get(context).getBitmapPool();
    // Make sure we didn't put the same Bitmap twice.
    Bitmap first = bitmapPool.get(width, height, Config.ARGB_8888);
    Bitmap second = bitmapPool.get(width, height, Config.ARGB_8888);

    assertThat(first).isNotSameAs(second);
  }
   @Test
  public void load_withColorDrawable_fixedSize_functionalBitmapTransform_doesNotRecycleOutput()
      throws ExecutionException, InterruptedException {
    Drawable colorDrawable = new ColorDrawable(Color.RED);

    int width = 100;
    int height = 200;

    Drawable result = GlideApp.with(context)
        .load(colorDrawable)
        .circleCrop()
        .override(width, height)
        .submit()
        .get();

     BitmapSubject.assertThat(result).isNotRecycled();

    BitmapPool bitmapPool = Glide.get(context).getBitmapPool();
    // Make sure we didn't put the same Bitmap twice.
    Bitmap first = bitmapPool.get(width, height, Config.ARGB_8888);
    Bitmap second = bitmapPool.get(width, height, Config.ARGB_8888);

    assertThat(first).isNotSameAs(second);
  }
}
