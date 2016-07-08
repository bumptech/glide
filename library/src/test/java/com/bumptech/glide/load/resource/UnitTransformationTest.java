package com.bumptech.glide.load.resource;

import static com.bumptech.glide.tests.Util.mockResource;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.tests.KeyAssertions;
import com.bumptech.glide.tests.Util;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class UnitTransformationTest {

  @Test
  public void testReturnsGivenResource() {
    Resource<Object> resource = mockResource();
    UnitTransformation<Object> transformation = UnitTransformation.get();
    assertEquals(resource, transformation.transform(resource, 10, 10));
  }

  @Test
  public void testEquals() throws NoSuchAlgorithmException {
    KeyAssertions.assertSame(UnitTransformation.get(), UnitTransformation.get());

    @SuppressWarnings("unchecked") Transformation<Object> other = mock(Transformation.class);
    doAnswer(new Util.WriteDigest("other")).when(other)
        .updateDiskCacheKey(any(MessageDigest.class));
    KeyAssertions.assertDifferent(UnitTransformation.get(), other);
  }
}
