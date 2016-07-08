package com.bumptech.glide.request;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Bitmap;
import com.bumptech.glide.load.Transformation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BaseRequestOptionsTest {

  private TestOptions options;
  @Mock private Transformation<Bitmap> transformation;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    options = new TestOptions();
  }

  @Test
  public void testIsTransformationRequired_byDefault_isFalse() {
    assertThat(options.isTransformationRequired()).isFalse();
  }

  @Test
  public void testIsTransformationSet_byDefault_isFalse() {
    assertThat(options.isTransformationSet()).isFalse();
  }

  @Test
  public void testIsTransformationAllowed_byDefault_isTrue() {
    assertThat(options.isTransformationAllowed()).isTrue();
  }

  @Test
  public void testIsTransformationSet_afterApplyingOptionsWithTransform_isTrue() {
    TestOptions other = new TestOptions();
    other.transform(Bitmap.class, transformation);
    options.apply(other);
    assertThat(options.isTransformationSet()).isTrue();
  }

  @Test
  public void testIsTransformationSet_afterDontTransform_isFalse() {
    options.dontTransform();
    assertThat(options.isTransformationSet()).isFalse();
  }

  @Test
  public void testIsTransformationAllowed_afterDontTransform_isFalse() {
    options.dontTransform();
    assertThat(options.isTransformationAllowed()).isFalse();
  }

  @Test
  public void testIsTransformationRequired_afterDontTransform_isFalse() {
    options.dontTransform();
    assertThat(options.isTransformationRequired()).isFalse();
  }

  @Test
  public void testApplyingDontTransform_overridesTransformations() {
    options.transform(RuntimeEnvironment.application, transformation);
    options.dontTransform();
    assertThat(options.isTransformationSet()).isFalse();
    assertThat(options.isTransformationRequired()).isFalse();
    assertThat(options.getTransformations()).isEmpty();
  }

  @Test
  public void testApplyingTransformation_overridesDontTransform() {
    options.dontTransform();
    options.transform(RuntimeEnvironment.application, transformation);

    assertThat(options.isTransformationAllowed()).isTrue();
    assertThat(options.isTransformationRequired()).isTrue();
    assertThat(options.getTransformations()).containsEntry(Bitmap.class, transformation);
  }

  @Test
  public void testApplyingOptions_withDontTransform_overridesTransformations() {
    options.transform(RuntimeEnvironment.application, transformation);
    TestOptions other = new TestOptions();
    other.dontTransform();

    options.apply(other);

    assertThat(options.isTransformationAllowed()).isFalse();
    assertThat(options.isTransformationSet()).isFalse();
    assertThat(options.isTransformationRequired()).isFalse();
    assertThat(options.getTransformations()).isEmpty();
  }

  @Test
  public void testApplyingOptions_withTransformation_overridesDontTransform() {
    options.dontTransform();
    TestOptions other = new TestOptions();
    other.transform(RuntimeEnvironment.application, transformation);

    options.apply(other);

    assertThat(options.isTransformationAllowed()).isTrue();
    assertThat(options.isTransformationSet()).isTrue();
    assertThat(options.isTransformationRequired()).isTrue();
    assertThat(options.getTransformations()).containsEntry(Bitmap.class, transformation);
  }

  @Test
  public void testApplyingDefaultOptions_withDontTransform_retainsDontTransform() {
    options.dontTransform();
    options.apply(new TestOptions());

    assertThat(options.isTransformationAllowed()).isFalse();
    assertThat(options.isTransformationRequired()).isFalse();
    assertThat(options.getTransformations()).isEmpty();
  }

  @Test
  public void testApplyingDefaultOptions_withTransform_retrainsTransform() {
    options.transform(RuntimeEnvironment.application, transformation);
    options.apply(new TestOptions());

    assertThat(options.isTransformationAllowed()).isTrue();
    assertThat(options.isTransformationRequired()).isTrue();
    assertThat(options.getTransformations()).containsEntry(Bitmap.class, transformation);
  }

  private static class TestOptions extends BaseRequestOptions<TestOptions> {
    // Empty.
  }
}
