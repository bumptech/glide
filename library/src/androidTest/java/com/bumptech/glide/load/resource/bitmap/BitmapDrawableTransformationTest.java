package com.bumptech.glide.load.resource.bitmap;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import android.graphics.Bitmap;

import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.tests.KeyAssertions;
import com.bumptech.glide.tests.Util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class BitmapDrawableTransformationTest {

  @Mock BitmapPool bitmapPool;
  @Mock Transformation<Bitmap> wrapped;
  private BitmapDrawableTransformation transformation;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    transformation = new BitmapDrawableTransformation(RuntimeEnvironment.application, bitmapPool,
        wrapped);
  }

  @Test
  public void testEquals() throws NoSuchAlgorithmException {
    doAnswer(new Util.WriteDigest("wrapped")).when(wrapped)
        .updateDiskCacheKey(any(MessageDigest.class));
    KeyAssertions.assertSame(transformation,
        new BitmapDrawableTransformation(RuntimeEnvironment.application, bitmapPool, wrapped));

    Transformation<Bitmap> other = mock(Transformation.class);
    doAnswer(new Util.WriteDigest("other")).when(other)
        .updateDiskCacheKey(any(MessageDigest.class));
    KeyAssertions.assertDifferent(transformation,
        new BitmapDrawableTransformation(RuntimeEnvironment.application, bitmapPool, other));
  }
}