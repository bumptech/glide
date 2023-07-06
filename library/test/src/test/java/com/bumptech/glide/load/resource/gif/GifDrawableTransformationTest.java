package com.bumptech.glide.load.resource.gif;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.bumptech.glide.tests.Util.mockResource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.tests.KeyTester;
import com.bumptech.glide.tests.Util;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class GifDrawableTransformationTest {
  @Rule public final KeyTester keyTester = new KeyTester();
  @Mock private Transformation<Bitmap> wrapped;
  @Mock private BitmapPool bitmapPool;

  private GifDrawableTransformation transformation;
  private Context context;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    context = ApplicationProvider.getApplicationContext();

    Glide.init(context, new GlideBuilder().setBitmapPool(bitmapPool));
    transformation = new GifDrawableTransformation(wrapped);
  }

  @After
  public void tearDown() {
    Glide.tearDown();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSetsTransformationAsFrameTransformation() {
    Resource<GifDrawable> resource = mockResource();
    GifDrawable gifDrawable = mock(GifDrawable.class);
    Transformation<Bitmap> unitTransformation = UnitTransformation.get();
    when(gifDrawable.getFrameTransformation()).thenReturn(unitTransformation);
    when(gifDrawable.getIntrinsicWidth()).thenReturn(500);
    when(gifDrawable.getIntrinsicHeight()).thenReturn(500);
    when(resource.get()).thenReturn(gifDrawable);

    Bitmap firstFrame = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    when(gifDrawable.getFirstFrame()).thenReturn(firstFrame);

    final int width = 123;
    final int height = 456;
    Bitmap expectedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Resource<Bitmap> expectedResource = mockResource();
    when(expectedResource.get()).thenReturn(expectedBitmap);
    when(wrapped.transform(any(Context.class), Util.<Bitmap>anyResource(), anyInt(), anyInt()))
        .thenReturn(expectedResource);

    transformation.transform(context, resource, width, height);

    verify(gifDrawable).setFrameTransformation(isA(Transformation.class), eq(expectedBitmap));
  }

  @Test
  public void testEquals() throws NoSuchAlgorithmException {
    doAnswer(new Util.WriteDigest("first"))
        .when(wrapped)
        .updateDiskCacheKey(isA(MessageDigest.class));
    @SuppressWarnings("unchecked")
    Transformation<Bitmap> other = mock(Transformation.class);
    doAnswer(new Util.WriteDigest("other"))
        .when(other)
        .updateDiskCacheKey(isA(MessageDigest.class));
    keyTester
        .addEquivalenceGroup(
            transformation,
            new GifDrawableTransformation(wrapped),
            new GifDrawableTransformation(wrapped))
        .addEquivalenceGroup(wrapped)
        .addEquivalenceGroup(new GifDrawableTransformation(other))
        .addRegressionTest(
            new GifDrawableTransformation(wrapped),
            "a7937b64b8caa58f03721bb6bacf5c78cb235febe0e70b1b84cd99541461a08e")
        .test();
  }
}
