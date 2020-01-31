package com.bumptech.glide.load;

import static com.bumptech.glide.tests.Util.anyContext;
import static com.bumptech.glide.tests.Util.anyResource;
import static com.bumptech.glide.tests.Util.mockResource;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.tests.KeyTester;
import com.bumptech.glide.tests.Util;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(JUnit4.class)
@SuppressWarnings("unchecked")
public class MultiTransformationTest {
  @Rule public final KeyTester keyTester = new KeyTester();

  @Mock private Transformation<Object> first;
  @Mock private Transformation<Object> second;
  @Mock private Resource<Object> initial;
  @Mock private Resource<Object> firstTransformed;
  @Mock private Resource<Object> secondTransformed;
  private Application context;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    context = RuntimeEnvironment.application;

    doAnswer(new Util.WriteDigest("first"))
        .when(first)
        .updateDiskCacheKey(any(MessageDigest.class));
    doAnswer(new Util.WriteDigest("second"))
        .when(second)
        .updateDiskCacheKey(any(MessageDigest.class));
  }

  @Test
  public void testAppliesTransformationsInOrder() {
    final int width = 584;
    final int height = 768;

    MultiTransformation<Object> transformation = new MultiTransformation<>(first, second);
    when(first.transform(anyContext(), eq(initial), eq(width), eq(height)))
        .thenReturn(firstTransformed);

    when(second.transform(anyContext(), eq(firstTransformed), eq(width), eq(height)))
        .thenReturn(secondTransformed);

    assertEquals(secondTransformed, transformation.transform(context, initial, width, height));
  }

  @Test
  public void testInitialResourceIsNotRecycled() {
    when(first.transform(anyContext(), anyResource(), anyInt(), anyInt()))
        .thenReturn(firstTransformed);

    MultiTransformation<Object> transformation = new MultiTransformation<>(first);

    transformation.transform(context, initial, 123, 456);

    verify(initial, never()).recycle();
  }

  @Test
  public void testInitialResourceIsNotRecycledEvenIfReturnedByMultipleTransformations() {
    when(first.transform(anyContext(), anyResource(), anyInt(), anyInt())).thenReturn(initial);
    when(second.transform(anyContext(), anyResource(), anyInt(), anyInt())).thenReturn(initial);

    MultiTransformation<Object> transformation = new MultiTransformation<>(first, second);
    transformation.transform(context, initial, 1111, 2222);

    verify(initial, never()).recycle();
  }

  @Test
  public void
      testInitialResourceIsNotRecycledIfReturnedByOneTransformationButNotByALaterTransformation() {
    when(first.transform(anyContext(), anyResource(), anyInt(), anyInt())).thenReturn(initial);
    when(second.transform(anyContext(), anyResource(), anyInt(), anyInt()))
        .thenReturn(mockResource());

    MultiTransformation<Object> transformation = new MultiTransformation<>(first, second);
    transformation.transform(context, initial, 1, 2);

    verify(initial, never()).recycle();
  }

  @Test
  public void testFinalResourceIsNotRecycled() {
    when(first.transform(anyContext(), anyResource(), anyInt(), anyInt()))
        .thenReturn(firstTransformed);

    MultiTransformation<Object> transformation = new MultiTransformation<>(first);

    transformation.transform(context, mockResource(), 111, 222);

    verify(firstTransformed, never()).recycle();
  }

  @Test
  public void testIntermediateResourcesAreRecycled() {
    when(first.transform(anyContext(), anyResource(), anyInt(), anyInt()))
        .thenReturn(firstTransformed);
    when(second.transform(anyContext(), anyResource(), anyInt(), anyInt()))
        .thenReturn(secondTransformed);

    MultiTransformation<Object> transformation = new MultiTransformation<>(first, second);

    transformation.transform(context, mockResource(), 233, 454);

    verify(firstTransformed).recycle();
  }

  @Test
  public void testEquals() throws NoSuchAlgorithmException {
    keyTester
        .addEquivalenceGroup(new MultiTransformation<>(first), new MultiTransformation<>(first))
        .addEquivalenceGroup(new MultiTransformation<>(second))
        .addEquivalenceGroup(new MultiTransformation<>(first, second))
        .addEquivalenceGroup(new MultiTransformation<>(second, first))
        .addRegressionTest(
            new MultiTransformation<>(first),
            "a7937b64b8caa58f03721bb6bacf5c78cb235febe0e70b1b84cd99541461a08e")
        .addRegressionTest(
            new MultiTransformation<>(first, second),
            "da83f63e1a473003712c18f5afc5a79044221943d1083c7c5a7ac7236d85e8d2")
        .test();
  }
}
