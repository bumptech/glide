package com.bumptech.glide.load.resource.bitmap;

import static com.bumptech.glide.tests.Util.anyContext;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.tests.KeyTester;
import com.bumptech.glide.tests.Util;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
@SuppressWarnings("deprecation")
public class BitmapDrawableTransformationTest {
  @Rule public final KeyTester keyTester = new KeyTester();

  @Mock private BitmapPool bitmapPool;
  @Mock private Transformation<Bitmap> wrapped;
  @Mock private Resource<BitmapDrawable> drawableResourceToTransform;

  private BitmapDrawableTransformation transformation;
  private Bitmap bitmapToTransform;
  private Application context;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    bitmapToTransform = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    BitmapDrawable drawableToTransform = new BitmapDrawable(bitmapToTransform);

    context = ApplicationProvider.getApplicationContext();
    Glide.init(context, new GlideBuilder().setBitmapPool(bitmapPool));
    when(drawableResourceToTransform.get()).thenReturn(drawableToTransform);
    transformation = new BitmapDrawableTransformation(wrapped);
  }

  @After
  public void tearDown() {
    Glide.tearDown();
  }

  @Test
  public void testReturnsOriginalResourceIfTransformationDoesNotTransform() {
    int outWidth = 123;
    int outHeight = 456;
    when(wrapped.transform(anyContext(), Util.<Bitmap>anyResource(), eq(outWidth), eq(outHeight)))
        .thenAnswer(
            new Answer<Resource<Bitmap>>() {
              @SuppressWarnings("unchecked")
              @Override
              public Resource<Bitmap> answer(InvocationOnMock invocation) throws Throwable {
                return (Resource<Bitmap>) invocation.getArguments()[1];
              }
            });

    Resource<BitmapDrawable> transformed =
        transformation.transform(context, drawableResourceToTransform, outWidth, outHeight);

    assertThat(transformed).isEqualTo(drawableResourceToTransform);
  }

  @Test
  public void testReturnsNewResourceIfTransformationDoesTransform() {
    int outWidth = 999;
    int outHeight = 555;

    Bitmap transformedBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.RGB_565);
    Resource<Bitmap> transformedBitmapResource = Util.mockResource();
    when(transformedBitmapResource.get()).thenReturn(transformedBitmap);
    when(wrapped.transform(anyContext(), Util.<Bitmap>anyResource(), eq(outWidth), eq(outHeight)))
        .thenReturn(transformedBitmapResource);

    Resource<BitmapDrawable> transformed =
        transformation.transform(context, drawableResourceToTransform, outWidth, outHeight);

    assertThat(transformed.get().getBitmap()).isEqualTo(transformedBitmap);
  }

  @Test
  public void testProvidesBitmapFromGivenResourceToWrappedTransformation() {
    int outWidth = 332;
    int outHeight = 111;
    Resource<Bitmap> transformed = Util.mockResource();
    when(transformed.get())
        .thenReturn(Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888));
    when(wrapped.transform(anyContext(), Util.<Bitmap>anyResource(), anyInt(), anyInt()))
        .thenReturn(transformed);

    transformation.transform(context, drawableResourceToTransform, outWidth, outHeight);
    ArgumentCaptor<Resource<Bitmap>> captor = Util.cast(ArgumentCaptor.forClass(Resource.class));

    verify(wrapped).transform(anyContext(), captor.capture(), eq(outWidth), eq(outHeight));

    assertThat(captor.getValue().get()).isEqualTo(bitmapToTransform);
  }

  @Test
  public void testEquals() throws NoSuchAlgorithmException {
    doAnswer(new Util.WriteDigest("wrapped"))
        .when(wrapped)
        .updateDiskCacheKey(any(MessageDigest.class));
    @SuppressWarnings("unchecked")
    Transformation<Bitmap> other = mock(Transformation.class);
    doAnswer(new Util.WriteDigest("other"))
        .when(other)
        .updateDiskCacheKey(any(MessageDigest.class));

    keyTester
        .addEquivalenceGroup(transformation, new BitmapDrawableTransformation(wrapped))
        .addEquivalenceGroup(new BitmapDrawableTransformation(other))
        .addEquivalenceGroup(wrapped)
        .addRegressionTest(
            transformation, "adbf45b08ad6468aa147e5b2a23758ef56ab631a2b70ad52501ca358441a34f3")
        .test();
  }
}
