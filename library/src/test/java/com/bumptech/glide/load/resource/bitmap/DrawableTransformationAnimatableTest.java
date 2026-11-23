package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.GlideBuilder.UseResourceClassForDecoding;
import com.bumptech.glide.GlideContext;
import com.bumptech.glide.GlideExperiments;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.load.engine.cache.DiskCacheAdapter;
import com.bumptech.glide.load.engine.cache.MemoryCacheAdapter;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.MockGlideExecutor;
import com.bumptech.glide.load.resource.SimpleResource;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.ImageViewTargetFactory;
import java.security.MessageDigest;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Tests {@link DrawableTransformation} handling of {@link Animatable} resources, including
 * containers, custom {@link Drawable#getCurrent()} overrides, and {@link BitmapDrawable} subtypes.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public final class DrawableTransformationAnimatableTest {

  private static final int INPUT_SIZE = 100;
  private static final int TRANSFORMED_SIZE = 50;

  private final Context context = ApplicationProvider.getApplicationContext();
  private Bitmap transformedBitmap;
  private StubBitmapTransformation bitmapTransformation;

  @Before
  public void setUp() {
    transformedBitmap =
        Bitmap.createBitmap(TRANSFORMED_SIZE, TRANSFORMED_SIZE, Bitmap.Config.ARGB_8888);
    bitmapTransformation = new StubBitmapTransformation(transformedBitmap);
  }

  @After
  public void tearDown() {
    Glide.tearDown();
  }

  @Test
  public void transform_optional_withNonContainerAnimatable_whenExperimentEnabled_skipsGet() {
    // Set global Glide opposite the GlideContext override to confirm GlideContext wins.
    initGlide(/* useResourceClass= */ false);
    GlideContext glideContext = newGlideContextWithExperiment(/* enabled= */ true);
    DrawableTransformation optional =
        new DrawableTransformation(bitmapTransformation, /* isRequired= */ false);
    CountingAnimatedResource firstInput =
        new CountingAnimatedResource(new FakeAnimatedDrawable(), FakeAnimatedDrawable.class);
    CountingAnimatedResource secondInput =
        new CountingAnimatedResource(new FakeAnimatedDrawable(), FakeAnimatedDrawable.class);

    // Call twice to verify repeated lookups return the same result.
    Resource<Drawable> firstResult =
        optional.transform(glideContext, firstInput, INPUT_SIZE, INPUT_SIZE);
    Resource<Drawable> secondResult =
        optional.transform(glideContext, secondInput, INPUT_SIZE, INPUT_SIZE);

    assertThat(firstResult).isSameInstanceAs(firstInput);
    assertThat(firstInput.getCount).isEqualTo(0);
    assertThat(secondResult).isSameInstanceAs(secondInput);
    assertThat(secondInput.getCount).isEqualTo(0);
    assertThat(bitmapTransformation.transformCount).isEqualTo(0);
  }

  @Test
  public void transform_optional_withNonContainerAnimatable_whenExperimentDisabled_callsGet() {
    // Set global Glide opposite the GlideContext override to confirm GlideContext wins.
    initGlide(/* useResourceClass= */ true);
    GlideContext glideContext = newGlideContextWithExperiment(/* enabled= */ false);
    DrawableTransformation optional =
        new DrawableTransformation(bitmapTransformation, /* isRequired= */ false);
    CountingAnimatedResource input =
        new CountingAnimatedResource(new FakeAnimatedDrawable(), FakeAnimatedDrawable.class);

    Resource<Drawable> result = optional.transform(glideContext, input, INPUT_SIZE, INPUT_SIZE);

    assertThat(result).isSameInstanceAs(input);
    assertThat(input.getCount).isEqualTo(1);
    assertThat(bitmapTransformation.transformCount).isEqualTo(0);
  }

  @Test
  public void transform_optional_withPlainApplicationContext_fallsBackToGet() {
    initGlide(/* useResourceClass= */ true);
    DrawableTransformation optional =
        new DrawableTransformation(bitmapTransformation, /* isRequired= */ false);
    CountingAnimatedResource input =
        new CountingAnimatedResource(new FakeAnimatedDrawable(), FakeAnimatedDrawable.class);

    Resource<Drawable> result = optional.transform(context, input, INPUT_SIZE, INPUT_SIZE);

    assertThat(result).isSameInstanceAs(input);
    assertThat(input.getCount).isEqualTo(1);
    assertThat(bitmapTransformation.transformCount).isEqualTo(0);
  }

  @Test
  public void transform_optional_withNullGlideExperiments_fallsBackToGet() {
    initGlide(/* useResourceClass= */ true);
    GlideContext glideContext = newGlideContext(/* experiments= */ null);
    DrawableTransformation optional =
        new DrawableTransformation(bitmapTransformation, /* isRequired= */ false);
    CountingAnimatedResource input =
        new CountingAnimatedResource(new FakeAnimatedDrawable(), FakeAnimatedDrawable.class);

    Resource<Drawable> result = optional.transform(glideContext, input, INPUT_SIZE, INPUT_SIZE);

    assertThat(result).isSameInstanceAs(input);
    assertThat(input.getCount).isEqualTo(1);
    assertThat(bitmapTransformation.transformCount).isEqualTo(0);
  }

  @Test
  public void transform_optional_withColorDrawable_transformsBitmap() {
    initGlide(/* useResourceClass= */ true);
    GlideContext glideContext = newGlideContextWithExperiment(/* enabled= */ true);
    DrawableTransformation optional =
        new DrawableTransformation(bitmapTransformation, /* isRequired= */ false);
    Resource<Drawable> input = new SimpleResource<>(new ColorDrawable(Color.RED));

    Resource<Drawable> result = optional.transform(glideContext, input, INPUT_SIZE, INPUT_SIZE);

    assertThat(bitmapTransformation.transformCount).isEqualTo(1);
    assertThat(((BitmapDrawable) result.get()).getBitmap()).isSameInstanceAs(transformedBitmap);
  }

  @Test
  public void transform_optional_withGenericDrawableResourceClass_transformsBitmap() {
    initGlide(/* useResourceClass= */ true);
    GlideContext glideContext = newGlideContextWithExperiment(/* enabled= */ true);
    DrawableTransformation optional =
        new DrawableTransformation(bitmapTransformation, /* isRequired= */ false);
    CountingAnimatedResource input =
        new CountingAnimatedResource(new ColorDrawable(Color.RED), Drawable.class);

    Resource<Drawable> result = optional.transform(glideContext, input, INPUT_SIZE, INPUT_SIZE);

    assertThat(input.getCount).isEqualTo(1);
    assertThat(bitmapTransformation.transformCount).isEqualTo(1);
    assertThat(((BitmapDrawable) result.get()).getBitmap()).isSameInstanceAs(transformedBitmap);
  }

  @Test
  public void transform_optional_withAnimationDrawable_transformsCurrentFrame() {
    initGlide(/* useResourceClass= */ true);
    GlideContext glideContext = newGlideContextWithExperiment(/* enabled= */ true);
    DrawableTransformation optional =
        new DrawableTransformation(bitmapTransformation, /* isRequired= */ false);
    Bitmap frameBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);
    AnimationDrawable animation = new AnimationDrawable();
    animation.addFrame(
        new BitmapDrawable(context.getResources(), frameBitmap), /* duration= */ 100);

    Resource<Drawable> result =
        optional.transform(glideContext, new SimpleResource<>(animation), INPUT_SIZE, INPUT_SIZE);

    assertThat(bitmapTransformation.transformCount).isEqualTo(1);
    assertThat(bitmapTransformation.lastInputBitmap).isSameInstanceAs(frameBitmap);
    assertThat(((BitmapDrawable) result.get()).getBitmap()).isSameInstanceAs(transformedBitmap);
  }

  @Test
  public void transform_optional_withInheritedGetCurrentOverride_transformsCurrentFrame() {
    initGlide(/* useResourceClass= */ true);
    GlideContext glideContext = newGlideContextWithExperiment(/* enabled= */ true);
    DrawableTransformation optional =
        new DrawableTransformation(bitmapTransformation, /* isRequired= */ false);
    Bitmap frameBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);
    BitmapDrawable frame = new BitmapDrawable(context.getResources(), frameBitmap);
    Resource<Drawable> firstInput = new SimpleResource<>(new LeafDelegatingAnimatedDrawable(frame));
    Resource<Drawable> secondInput =
        new SimpleResource<>(new LeafDelegatingAnimatedDrawable(frame));

    // Call twice to verify repeated lookups return the same result.
    Resource<Drawable> firstResult =
        optional.transform(glideContext, firstInput, INPUT_SIZE, INPUT_SIZE);
    Resource<Drawable> secondResult =
        optional.transform(glideContext, secondInput, INPUT_SIZE, INPUT_SIZE);

    assertThat(bitmapTransformation.transformCount).isEqualTo(2);
    assertThat(bitmapTransformation.lastInputBitmap).isSameInstanceAs(frameBitmap);
    assertThat(((BitmapDrawable) firstResult.get()).getBitmap())
        .isSameInstanceAs(transformedBitmap);
    assertThat(((BitmapDrawable) secondResult.get()).getBitmap())
        .isSameInstanceAs(transformedBitmap);
  }

  @Test
  public void transform_optional_withAnimatableBitmapDrawable_transformsBitmap() {
    initGlide(/* useResourceClass= */ true);
    GlideContext glideContext = newGlideContextWithExperiment(/* enabled= */ true);
    DrawableTransformation optional =
        new DrawableTransformation(bitmapTransformation, /* isRequired= */ false);
    Bitmap frameBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);
    Resource<Drawable> input =
        new SimpleResource<>(new AnimatableBitmapDrawable(context.getResources(), frameBitmap));

    Resource<Drawable> result = optional.transform(glideContext, input, INPUT_SIZE, INPUT_SIZE);

    assertThat(bitmapTransformation.transformCount).isEqualTo(1);
    assertThat(bitmapTransformation.lastInputBitmap).isSameInstanceAs(frameBitmap);
    assertThat(((BitmapDrawable) result.get()).getBitmap()).isSameInstanceAs(transformedBitmap);
  }

  @Test
  public void transform_optional_withNullResourceClass_fallsBackToGet() {
    initGlide(/* useResourceClass= */ true);
    GlideContext glideContext = newGlideContextWithExperiment(/* enabled= */ true);
    DrawableTransformation optional =
        new DrawableTransformation(bitmapTransformation, /* isRequired= */ false);
    CountingAnimatedResource input =
        new CountingAnimatedResource(new FakeAnimatedDrawable(), /* declaredClass= */ null);

    Resource<Drawable> result = optional.transform(glideContext, input, INPUT_SIZE, INPUT_SIZE);

    assertThat(input.getCount).isEqualTo(1);
    assertThat(result).isSameInstanceAs(input);
    assertThat(bitmapTransformation.transformCount).isEqualTo(0);
  }

  @Test
  public void transform_optional_withRawAnimatableInterfaceClass_fallsBackToGet() {
    initGlide(/* useResourceClass= */ true);
    GlideContext glideContext = newGlideContextWithExperiment(/* enabled= */ true);
    DrawableTransformation optional =
        new DrawableTransformation(bitmapTransformation, /* isRequired= */ false);
    CountingAnimatedResource input =
        new CountingAnimatedResource(new FakeAnimatedDrawable(), Animatable.class);

    Resource<Drawable> result = optional.transform(glideContext, input, INPUT_SIZE, INPUT_SIZE);

    assertThat(input.getCount).isEqualTo(1);
    assertThat(result).isSameInstanceAs(input);
    assertThat(bitmapTransformation.transformCount).isEqualTo(0);
  }

  @Test
  public void transform_required_withNonContainerAnimatable_callsGetAndThrows() {
    initGlide(/* useResourceClass= */ true);
    GlideContext glideContext = newGlideContextWithExperiment(/* enabled= */ true);
    DrawableTransformation required =
        new DrawableTransformation(bitmapTransformation, /* isRequired= */ true);
    CountingAnimatedResource input =
        new CountingAnimatedResource(new FakeAnimatedDrawable(), FakeAnimatedDrawable.class);

    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> required.transform(glideContext, input, INPUT_SIZE, INPUT_SIZE));
    assertThat(input.getCount).isEqualTo(1);
    assertThat(thrown).hasMessageThat().contains("Unable to convert");
  }

  private void initGlide(boolean useResourceClass) {
    Glide.tearDown();
    GlideExecutor directExecutor = MockGlideExecutor.newMainThreadExecutor();
    Glide.init(
        context,
        new GlideBuilder()
            .setBitmapPool(new BitmapPoolAdapter())
            .setSourceExecutor(directExecutor)
            .setDiskCacheExecutor(directExecutor)
            .setAnimationExecutor(directExecutor)
            .experimentalSetUseResourceClassForDecoding(useResourceClass));
  }

  private GlideContext newGlideContextWithExperiment(boolean enabled) {
    GlideExperiments experiments = mock(GlideExperiments.class);
    when(experiments.isEnabled(UseResourceClassForDecoding.class)).thenReturn(enabled);
    return newGlideContext(experiments);
  }

  private GlideContext newGlideContext(@Nullable GlideExperiments experiments) {
    GlideExecutor directExecutor = MockGlideExecutor.newMainThreadExecutor();
    Engine engine =
        new Engine(
            new MemoryCacheAdapter(),
            DiskCacheAdapter::new,
            directExecutor,
            directExecutor,
            directExecutor,
            directExecutor,
            /* isActiveResourceRetentionAllowed= */ false);
    return new GlideContext(
        context,
        Glide.get(context).getArrayPool(),
        () -> Glide.get(context).getRegistry(),
        new ImageViewTargetFactory(),
        RequestOptions::new,
        Collections.emptyMap(),
        Collections.emptyList(),
        engine,
        experiments,
        Log.INFO);
  }

  private static final class StubBitmapTransformation implements Transformation<Bitmap> {
    private static final String ID = "stub-bitmap-transformation";

    private final Bitmap transformedBitmap;
    int transformCount;
    @Nullable Bitmap lastInputBitmap;

    StubBitmapTransformation(Bitmap transformedBitmap) {
      this.transformedBitmap = transformedBitmap;
    }

    @NonNull
    @Override
    public Resource<Bitmap> transform(
        @NonNull Context context, @NonNull Resource<Bitmap> resource, int outWidth, int outHeight) {
      transformCount++;
      lastInputBitmap = resource.get();
      return BitmapResource.obtain(transformedBitmap, Glide.get(context).getBitmapPool());
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
      messageDigest.update(ID.getBytes(CHARSET));
    }
  }

  private static final class FakeAnimatedDrawable extends ColorDrawable implements Animatable {
    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Override
    public boolean isRunning() {
      return false;
    }
  }

  /**
   * Simulates non-{@code DrawableContainer} animated drawables (such as {@code
   * DrawableWrapperCompat} or {@code AnimatedVectorDrawableCompat}) that override {@link
   * #getCurrent()}.
   */
  private static class CustomDelegatingAnimatedDrawable extends ColorDrawable
      implements Animatable {
    private final Drawable currentFrame;

    CustomDelegatingAnimatedDrawable(Drawable currentFrame) {
      this.currentFrame = currentFrame;
    }

    @NonNull
    @Override
    public Drawable getCurrent() {
      return currentFrame;
    }

    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Override
    public boolean isRunning() {
      return false;
    }
  }

  /** Subclass inheriting {@link #getCurrent()} without redeclaring it. */
  private static final class LeafDelegatingAnimatedDrawable
      extends CustomDelegatingAnimatedDrawable {
    LeafDelegatingAnimatedDrawable(Drawable currentFrame) {
      super(currentFrame);
    }
  }

  private static final class AnimatableBitmapDrawable extends BitmapDrawable implements Animatable {
    AnimatableBitmapDrawable(Resources resources, Bitmap bitmap) {
      super(resources, bitmap);
    }

    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Override
    public boolean isRunning() {
      return false;
    }
  }

  private static final class CountingAnimatedResource implements Resource<Drawable> {
    private final Drawable drawable;
    @Nullable private final Class<?> declaredClass;
    int getCount;

    CountingAnimatedResource(Drawable drawable, @Nullable Class<?> declaredClass) {
      this.drawable = drawable;
      this.declaredClass = declaredClass;
    }

    // Intentionally omits @NonNull to test defensive null handling.
    // Unchecked cast lets tests pass both Drawable subtypes and raw interfaces like Animatable.
    @Override
    @SuppressWarnings("unchecked")
    public Class<Drawable> getResourceClass() {
      return (Class<Drawable>) declaredClass;
    }

    @NonNull
    @Override
    public Drawable get() {
      getCount++;
      return drawable;
    }

    @Override
    public int getSize() {
      return 1;
    }

    @Override
    public void recycle() {}
  }
}
