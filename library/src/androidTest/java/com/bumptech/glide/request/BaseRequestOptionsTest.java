package com.bumptech.glide.request;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import com.bumptech.glide.load.Transformation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BaseRequestOptionsTest {

  private TestOptions options;

  @Before
  public void setUp() {
    options = new TestOptions();
  }

  @Test
  public void testTransformationIsSetAfterApplyingOtherOptionsWithTransformation() {
    TestOptions other = new TestOptions();
    other.transform(Object.class, mock(Transformation.class));
    options.apply(other);
    assertThat(options.isTransformationSet()).isTrue();
  }

  private static class TestOptions extends BaseRequestOptions<TestOptions> {
    // Empty.
  }
}