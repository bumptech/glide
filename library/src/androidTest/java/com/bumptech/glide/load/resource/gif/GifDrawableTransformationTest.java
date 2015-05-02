package com.bumptech.glide.load.resource.gif;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.tests.KeyAssertions;
import com.bumptech.glide.tests.Util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class GifDrawableTransformationTest {
  @Mock Transformation<Bitmap> wrapped;
  @Mock BitmapPool bitmapPool;

  GifDrawableTransformation transformation;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    transformation = new GifDrawableTransformation(wrapped, bitmapPool);
  }

  @Test
  public void testSetsTransformationAsFrameTransformation() {
    Resource<GifDrawable> resource = mock(Resource.class);
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
    Resource<Bitmap> expectedResource = mock(Resource.class);
    when(expectedResource.get()).thenReturn(expectedBitmap);
    when(wrapped.transform(any(Resource.class), anyInt(), anyInt())).thenReturn(expectedResource);

    transformation.transform(resource, width, height);

    verify(gifDrawable).setFrameTransformation(any(Transformation.class), eq(expectedBitmap));
  }

  @Test
  public void testEquals() throws NoSuchAlgorithmException {
    doAnswer(new Util.WriteDigest("first")).when(wrapped)
        .updateDiskCacheKey(any(MessageDigest.class));
    KeyAssertions.assertSame(transformation, new GifDrawableTransformation(wrapped, bitmapPool));

    Transformation<Bitmap> other = mock(Transformation.class);
    doAnswer(new Util.WriteDigest("other")).when(other)
        .updateDiskCacheKey(any(MessageDigest.class));
    KeyAssertions.assertDifferent(transformation, new GifDrawableTransformation(other, bitmapPool));
  }
}
