package com.bumptech.glide.load.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.tests.KeyAssertions;
import com.bumptech.glide.tests.Util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RunWith(JUnit4.class)
public class UnitTransformationTest {

  @Test
  public void testReturnsGivenResource() {
    Resource resource = mock(Resource.class);
    UnitTransformation transformation = UnitTransformation.get();
    assertEquals(resource, transformation.transform(resource, 10, 10));
  }

  @Test
  public void testEquals() throws NoSuchAlgorithmException {
    KeyAssertions.assertSame(UnitTransformation.get(), UnitTransformation.get());

    Transformation<Object> other = mock(Transformation.class);
    doAnswer(new Util.WriteDigest("other")).when(other)
        .updateDiskCacheKey(any(MessageDigest.class));
    KeyAssertions.assertDifferent(UnitTransformation.get(), other);
  }
}
