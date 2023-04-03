package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.load.resource.SimpleResource;
import com.bumptech.glide.tests.KeyTester;
import com.bumptech.glide.tests.Util;
import java.security.MessageDigest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class DrawableTransformationTest {
  @Rule public final KeyTester keyTester = new KeyTester();
  @Mock private Transformation<Bitmap> bitmapTransformation;
  private BitmapPool bitmapPool;
  private DrawableTransformation transformation;
  private Context context;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    transformation = new DrawableTransformation(bitmapTransformation, /* isRequired= */ true);
    context = ApplicationProvider.getApplicationContext();
    bitmapPool = new BitmapPoolAdapter();
    Glide.init(context, new GlideBuilder().setBitmapPool(bitmapPool));
  }

  @After
  public void tearDown() {
    Glide.tearDown();
  }

  @Test
  public void transform_withBitmapDrawable_andUnitBitmapTransformation_doesNotRecycle() {
    when(bitmapTransformation.transform(
            any(Context.class), anyBitmapResource(), anyInt(), anyInt()))
        .thenAnswer(new ReturnGivenResource());

    Bitmap bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
    BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
    @SuppressWarnings("unchecked")
    Resource<Drawable> input =
        (Resource<Drawable>) (Resource<?>) new BitmapDrawableResource(drawable, bitmapPool);
    transformation.transform(context, input, /* outWidth= */ 100, /* outHeight= */ 200);

    assertThat(bitmap.isRecycled()).isFalse();
  }

  @Test
  public void transform_withBitmapDrawable_andFunctionalBitmapTransformation_doesNotRecycle() {
    when(bitmapTransformation.transform(
            any(Context.class), anyBitmapResource(), anyInt(), anyInt()))
        .thenAnswer(
            new Answer<Resource<Bitmap>>() {
              @Override
              public Resource<Bitmap> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return BitmapResource.obtain(
                    Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888), bitmapPool);
              }
            });
    Bitmap bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
    BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
    @SuppressWarnings("unchecked")
    Resource<Drawable> input =
        (Resource<Drawable>) (Resource<?>) new BitmapDrawableResource(drawable, bitmapPool);
    transformation.transform(context, input, /* outWidth= */ 100, /* outHeight= */ 200);

    assertThat(bitmap.isRecycled()).isFalse();
  }

  @Test
  public void transform_withColorDrawable_andUnitBitmapTransformation_recycles() {
    bitmapPool = mock(BitmapPool.class);
    Glide.tearDown();
    Glide.init(context, new GlideBuilder().setBitmapPool(bitmapPool));
    when(bitmapTransformation.transform(
            any(Context.class), anyBitmapResource(), anyInt(), anyInt()))
        .thenAnswer(new ReturnGivenResource());

    ColorDrawable colorDrawable = new ColorDrawable(Color.RED);
    final Resource<Drawable> input = new SimpleResource<Drawable>(colorDrawable);

    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                Bitmap bitmap = (Bitmap) invocationOnMock.getArguments()[0];
                assertThat(bitmap.getWidth()).isEqualTo(100);
                assertThat(bitmap.getHeight()).isEqualTo(200);
                return null;
              }
            })
        .when(bitmapPool)
        .put(any(Bitmap.class));
    when(bitmapPool.get(anyInt(), anyInt(), any(Bitmap.Config.class)))
        .thenAnswer(
            new Answer<Bitmap>() {
              @Override
              public Bitmap answer(InvocationOnMock invocationOnMock) throws Throwable {
                int width = (Integer) invocationOnMock.getArguments()[0];
                int height = (Integer) invocationOnMock.getArguments()[1];
                Bitmap.Config config = (Bitmap.Config) invocationOnMock.getArguments()[2];
                return Bitmap.createBitmap(width, height, config);
              }
            });

    transformation.transform(context, input, /* outWidth= */ 100, /* outHeight= */ 200);

    verify(bitmapPool).put(isA(Bitmap.class));
  }

  @Test
  public void testEquals() {
    BitmapTransformation otherBitmapTransformation = mock(BitmapTransformation.class);
    doAnswer(new Util.WriteDigest("bitmapTransformation"))
        .when(bitmapTransformation)
        .updateDiskCacheKey(any(MessageDigest.class));
    doAnswer(new Util.WriteDigest("otherBitmapTransformation"))
        .when(otherBitmapTransformation)
        .updateDiskCacheKey(any(MessageDigest.class));

    keyTester
        .addEquivalenceGroup(
            transformation,
            new DrawableTransformation(bitmapTransformation, /* isRequired= */ true),
            new DrawableTransformation(bitmapTransformation, /* isRequired= */ false))
        .addEquivalenceGroup(bitmapTransformation)
        .addEquivalenceGroup(otherBitmapTransformation)
        .addEquivalenceGroup(
            new DrawableTransformation(otherBitmapTransformation, /* isRequired= */ true),
            new DrawableTransformation(otherBitmapTransformation, /* isRequired= */ false))
        .addRegressionTest(
            new DrawableTransformation(bitmapTransformation, /* isRequired= */ true),
            "eddf60c557a6315a489b8a3a19b12439a90381256289fbe9a503afa726230bd9")
        .addRegressionTest(
            new DrawableTransformation(otherBitmapTransformation, /* isRequired= */ false),
            "40931536ed0ec97c39d4be10c44f5b69a86030ec575317f5a0f17e15a0ea9be8")
        .test();
  }

  @SuppressWarnings("unchecked")
  private static Resource<Bitmap> anyBitmapResource() {
    return any(Resource.class);
  }

  private static final class ReturnGivenResource implements Answer<Resource<Bitmap>> {

    @Override
    public Resource<Bitmap> answer(InvocationOnMock invocationOnMock) throws Throwable {
      @SuppressWarnings("unchecked")
      Resource<Bitmap> input = (Resource<Bitmap>) invocationOnMock.getArguments()[1];
      return input;
    }
  }
}
