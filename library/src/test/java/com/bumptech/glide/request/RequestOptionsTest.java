package com.bumptech.glide.request;

import static com.google.common.truth.Truth.assertThat;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.bumptech.glide.util.Util;
import com.google.common.testing.EqualsTester;
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
public class RequestOptionsTest {

  private RequestOptions options;
  @Mock private Transformation<Bitmap> transformation;
  private Application app;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    options = new RequestOptions();

    app = RuntimeEnvironment.application;
  }

  @Test
  public void isScaleOnlyOrNoTransform_byDefault_isTrue() {
    assertThat(options.isScaleOnlyOrNoTransform()).isTrue();
  }

  @Test
  public void isScaleOnlyOrNoTransform_withFitCenter_isTrue() {
    options.fitCenter();
    assertThat(options.isScaleOnlyOrNoTransform()).isTrue();
    options.optionalFitCenter();
    assertThat(options.isScaleOnlyOrNoTransform()).isTrue();
  }

  @Test
  public void isScaleOnlyOrNoTransform_withCenterInside_isTrue() {
    options.centerInside();
    assertThat(options.isScaleOnlyOrNoTransform()).isTrue();
    options.optionalCenterInside();
    assertThat(options.isScaleOnlyOrNoTransform()).isTrue();
  }

  @Test
  public void isScaleOnlyOrNoTransform_withCenterCrop_isFalse() {
    options.centerCrop();
    assertThat(options.isScaleOnlyOrNoTransform()).isFalse();
    options.optionalCenterCrop();
    assertThat(options.isScaleOnlyOrNoTransform()).isFalse();
  }

  @Test
  public void isScaleOnlyOrNoTransform_withCircleCrop_isFalse() {
    options.circleCrop();
    assertThat(options.isScaleOnlyOrNoTransform()).isFalse();
    options.circleCrop();
    assertThat(options.isScaleOnlyOrNoTransform()).isFalse();
  }

  @Test
  public void isScaleOnlyOrNoTransform_withBitmapTransformation_isFalse() {
    options.transform(transformation);
    assertThat(options.isScaleOnlyOrNoTransform()).isFalse();
    options.optionalTransform(transformation);
    assertThat(options.isScaleOnlyOrNoTransform()).isFalse();
  }

  @Test
  public void isScaleOnlyOrNoTransform_withCustomTransformation_isFalse() {
    options.transform(Bitmap.class, transformation);
    assertThat(options.isScaleOnlyOrNoTransform()).isFalse();
    options.optionalTransform(Bitmap.class, transformation);
    assertThat(options.isScaleOnlyOrNoTransform()).isFalse();
  }

  @Test
  public void isScaleOnlyOrNoTransform_withDownsampleStrategy_isTrue() {
    options.downsample(DownsampleStrategy.CENTER_OUTSIDE);
    assertThat(options.isScaleOnlyOrNoTransform()).isTrue();
  }

  @Test
  public void isScaleOnlyOrNoTransform_withNonScaleAndThenDontTransform_isTrue() {
    options.circleCrop().dontTransform();
    assertThat(options.isScaleOnlyOrNoTransform()).isTrue();
  }

  @Test
  public void isScaleOnlyOrNoTransform_withNonScaleAndAppliedDontTransform_isTrue() {
    options.circleCrop();
    options.apply(new RequestOptions().dontTransform());
    assertThat(options.isScaleOnlyOrNoTransform()).isTrue();
  }

  @Test
  public void isScaleOnlyOrNoTransform_withDontTransformAndAppliedNonScaleTransform_isFalse() {
    options.fitCenter();
    options.apply(new RequestOptions().circleCrop());
    assertThat(options.isScaleOnlyOrNoTransform()).isFalse();
  }

  @Test
  public void isScaleOnlyOrNoTransform_withNonScaleOnly_andAppliedWithScaleOnly_isTrue() {
    options.circleCrop();
    options.apply(new RequestOptions().fitCenter());
    assertThat(options.isScaleOnlyOrNoTransform()).isTrue();
  }

  @Test
  public void isScaleOnlyOrNoTransform_withScaleOnlyAndAppliedWithoutTransform_isTrue() {
    options.fitCenter();
    options.apply(new RequestOptions().dontAnimate());
    assertThat(options.isScaleOnlyOrNoTransform()).isTrue();
  }

  @Test
  public void isScaleOnlyOrNoTransform_withNonScaleOnlyAndAppliedWithoutTransform_isFalse() {
    options.circleCrop();
    options.apply(new RequestOptions().dontAnimate());
    assertThat(options.isScaleOnlyOrNoTransform()).isFalse();
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
    RequestOptions other = new RequestOptions();
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
    options.transform(transformation);
    options.dontTransform();
    assertThat(options.isTransformationSet()).isFalse();
    assertThat(options.isTransformationRequired()).isFalse();
    assertThat(options.getTransformations()).isEmpty();
  }

  @Test
  public void testApplyingTransformation_overridesDontTransform() {
    options.dontTransform();
    options.transform(transformation);

    assertThat(options.isTransformationAllowed()).isTrue();
    assertThat(options.isTransformationRequired()).isTrue();
    assertThat(options.getTransformations()).containsEntry(Bitmap.class, transformation);
  }

  @Test
  public void testApplyingOptions_withDontTransform_overridesTransformations() {
    options.transform(transformation);
    RequestOptions other = new RequestOptions();
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
    RequestOptions other = new RequestOptions();
    other.transform(transformation);

    options.apply(other);

    assertThat(options.isTransformationAllowed()).isTrue();
    assertThat(options.isTransformationSet()).isTrue();
    assertThat(options.isTransformationRequired()).isTrue();
    assertThat(options.getTransformations()).containsEntry(Bitmap.class, transformation);
  }

  @Test
  public void testApplyingDefaultOptions_withDontTransform_retainsDontTransform() {
    options.dontTransform();
    options.apply(new RequestOptions());

    assertThat(options.isTransformationAllowed()).isFalse();
    assertThat(options.isTransformationRequired()).isFalse();
    assertThat(options.getTransformations()).isEmpty();
  }

  @Test
  public void testApplyingDefaultOptions_withTransform_retrainsTransform() {
    options.transform(transformation);
    options.apply(new RequestOptions());

    assertThat(options.isTransformationAllowed()).isTrue();
    assertThat(options.isTransformationRequired()).isTrue();
    assertThat(options.getTransformations()).containsEntry(Bitmap.class, transformation);
  }

  @Test
  @SuppressWarnings({"unchecked", "varargs"})
  public void testApplyMultiTransform() {
    options.transforms(new CircleCrop(), new CenterCrop());
    assertThat(options.isTransformationRequired()).isTrue();
    assertThat(options.getTransformations()).containsKey(Bitmap.class);
    assertThat(options.getTransformations().get(Bitmap.class))
      .isInstanceOf(MultiTransformation.class);
  }

  @Test
  public void testEqualsHashCode() {
    Drawable first = new ColorDrawable(Color.RED);
    Drawable second = new GradientDrawable();
    assertThat(first).isNotEqualTo(second);
    assertThat(Util.bothNullOrEqual(first, second)).isFalse();
    new EqualsTester()
        .addEqualityGroup(
            new RequestOptions().sizeMultiplier(.7f),
            new RequestOptions().sizeMultiplier(.7f))
        .addEqualityGroup(new RequestOptions().sizeMultiplier(0.8f))
        .addEqualityGroup(new RequestOptions().error(1), new RequestOptions().error(1))
        .addEqualityGroup(new RequestOptions().error(2))
        .addEqualityGroup(new RequestOptions().error(first), new RequestOptions().error(first))
        .addEqualityGroup(new RequestOptions().error(second))
        .addEqualityGroup(new RequestOptions().placeholder(1), new RequestOptions().placeholder(1))
        .addEqualityGroup(new RequestOptions().placeholder(2))
        .addEqualityGroup(
            new RequestOptions().placeholder(first),
            new RequestOptions().placeholder(first))
        .addEqualityGroup(new RequestOptions().placeholder(second))
        .addEqualityGroup(new RequestOptions().fallback(1), new RequestOptions().fallback(1))
        .addEqualityGroup(new RequestOptions().fallback(2))
        .addEqualityGroup(
            new RequestOptions().fallback(first),
            new RequestOptions().fallback(first))
        .addEqualityGroup(new RequestOptions().fallback(second))
        .addEqualityGroup(
            new RequestOptions().skipMemoryCache(true),
            new RequestOptions().skipMemoryCache(true))
        .addEqualityGroup(
            new RequestOptions(),
            new RequestOptions().skipMemoryCache(false),
            new RequestOptions().theme(null),
            new RequestOptions().onlyRetrieveFromCache(false),
            new RequestOptions().useUnlimitedSourceGeneratorsPool(false))
        .addEqualityGroup(
            new RequestOptions().override(100),
            new RequestOptions().override(100, 100))
        .addEqualityGroup(
            new RequestOptions().override(200),
            new RequestOptions().override(200, 200))
        .addEqualityGroup(
            new RequestOptions().override(100, 200),
            new RequestOptions().override(100, 200))
        .addEqualityGroup(
            new RequestOptions().override(200, 100),
            new RequestOptions().override(200, 100))
        .addEqualityGroup(
            new RequestOptions().centerCrop(),
            new RequestOptions().centerCrop())
        .addEqualityGroup(
            new RequestOptions().optionalCenterCrop(),
            new RequestOptions().optionalCenterCrop())
        .addEqualityGroup(new RequestOptions().fitCenter())
        .addEqualityGroup(new RequestOptions().circleCrop())
        .addEqualityGroup(new RequestOptions().centerInside())
        .addEqualityGroup(
            new RequestOptions().useUnlimitedSourceGeneratorsPool(true),
            new RequestOptions().useUnlimitedSourceGeneratorsPool(true))
        .addEqualityGroup(
            new RequestOptions().onlyRetrieveFromCache(true),
            new RequestOptions().onlyRetrieveFromCache(true))
        .addEqualityGroup(
            new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL),
            new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
        .addEqualityGroup(
            new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
        .addEqualityGroup(
            new RequestOptions().priority(Priority.HIGH),
            new RequestOptions().priority(Priority.HIGH))
        .addEqualityGroup(
            new RequestOptions().priority(Priority.LOW))
        .addEqualityGroup(
            new RequestOptions().set(Option.<Boolean>memory("test"), true),
            new RequestOptions().set(Option.<Boolean>memory("test"), true))
        .addEqualityGroup(
            new RequestOptions().set(Option.<Boolean>memory("test"), false))
        .addEqualityGroup(
            new RequestOptions().set(Option.<Boolean>memory("test2"), true))
        .addEqualityGroup(
            new RequestOptions().decode(Integer.class),
            new RequestOptions().decode(Integer.class))
        .addEqualityGroup(
            new RequestOptions().decode(Float.class))
        .addEqualityGroup(
            new RequestOptions().signature(new ObjectKey("test")),
            new RequestOptions().signature(new ObjectKey("test")))
        .addEqualityGroup(
            new RequestOptions().signature(new ObjectKey("test2")))
        .addEqualityGroup(
            new RequestOptions().theme(app.getTheme()),
            new RequestOptions().theme(app.getTheme()))
        .testEquals();
  }

}
