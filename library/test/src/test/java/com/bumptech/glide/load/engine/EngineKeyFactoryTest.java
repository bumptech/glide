package com.bumptech.glide.load.engine;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.signature.ObjectKey;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.GcFinalization;
import com.google.testing.junit.testparameterinjector.TestParameter;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Objects;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestParameterInjector;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestParameterInjector.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class EngineKeyFactoryTest {
  private final EngineKeyFactory factory = new EngineKeyFactory();

  @Test
  public void buildKey_equalsAndHashCode(@TestParameter boolean allowWeakReferences) {
    Object identityModel = new Object();
    Transformation<Object> identityTransformation = new IdentityTransformation();

    new EqualsTester()
        .addEqualityGroup(
            buildKey(identityModel, allowWeakReferences),
            buildKey(identityModel, allowWeakReferences))
        .addEqualityGroup(buildKey(new Object(), allowWeakReferences))
        .addEqualityGroup(
            buildKey(new ValueModel("value"), allowWeakReferences),
            buildKey(new ValueModel("value"), allowWeakReferences))
        .addEqualityGroup(
            buildKeyWithTransformation("id", identityTransformation, allowWeakReferences),
            buildKeyWithTransformation("id", identityTransformation, allowWeakReferences))
        .addEqualityGroup(
            buildKeyWithTransformation("id", new IdentityTransformation(), allowWeakReferences))
        .addEqualityGroup(
            buildKeyWithTransformation(
                "id", new ValueTransformation("transform"), allowWeakReferences),
            buildKeyWithTransformation(
                "id", new ValueTransformation("transform"), allowWeakReferences))
        .testEquals();
  }

  @Test
  public void gc_withIdentityEqualModel_collectsModelOnlyWhenWeakReferencesAllowed(
      @TestParameter boolean allowWeakReferences) {
    Object model = new Object();
    EngineKey unusedRetainedKey = buildKey(model, allowWeakReferences);
    WeakReference<Object> modelRef = new WeakReference<>(model);
    model = null;

    if (allowWeakReferences) {
      GcFinalization.awaitClear(modelRef);
      assertThat(modelRef.get()).isNull();
    } else {
      GcFinalization.awaitFullGc();
      assertThat(modelRef.get()).isNotNull();
    }
  }

  @Test
  public void
      gc_withIdentityEqualTransformation_collectsTransformationOnlyWhenWeakReferencesAllowed(
          @TestParameter boolean allowWeakReferences) {
    Transformation<Object> transformation = new IdentityTransformation();
    EngineKey unusedRetainedKey =
        buildKeyWithTransformation("id", transformation, allowWeakReferences);
    WeakReference<Transformation<Object>> transformationRef = new WeakReference<>(transformation);
    transformation = null;

    if (allowWeakReferences) {
      GcFinalization.awaitClear(transformationRef);
      assertThat(transformationRef.get()).isNull();
    } else {
      GcFinalization.awaitFullGc();
      assertThat(transformationRef.get()).isNotNull();
    }
  }

  @Test
  public void gc_withValueEqualModel_retainsModel(@TestParameter boolean allowWeakReferences) {
    ValueModel model = new ValueModel("retained");
    EngineKey unusedRetainedKey = buildKey(model, allowWeakReferences);
    WeakReference<ValueModel> modelRef = new WeakReference<>(model);
    model = null;

    GcFinalization.awaitFullGc();

    assertThat(modelRef.get()).isNotNull();
  }

  @Test
  public void gc_withValueEqualTransformation_retainsTransformation(
      @TestParameter boolean allowWeakReferences) {
    ValueTransformation transformation = new ValueTransformation("retained");
    EngineKey unusedRetainedKey =
        buildKeyWithTransformation("id", transformation, allowWeakReferences);
    WeakReference<ValueTransformation> transformationRef = new WeakReference<>(transformation);
    transformation = null;

    GcFinalization.awaitFullGc();

    assertThat(transformationRef.get()).isNotNull();
  }

  private EngineKey buildKey(Object model, boolean allowWeakReferences) {
    return factory.buildKey(
        model,
        new ObjectKey("signature"),
        100,
        100,
        Object.class,
        Object.class,
        Collections.<Class<?>, Transformation<?>>emptyMap(),
        new Options(),
        allowWeakReferences);
  }

  private EngineKey buildKeyWithTransformation(
      Object model, Transformation<?> transformation, boolean allowWeakReferences) {
    return factory.buildKey(
        model,
        new ObjectKey("signature"),
        100,
        100,
        Object.class,
        Object.class,
        Collections.<Class<?>, Transformation<?>>singletonMap(Object.class, transformation),
        new Options(),
        allowWeakReferences);
  }

  private static final class IdentityTransformation implements Transformation<Object> {
    @NonNull
    @Override
    public Resource<Object> transform(
        @NonNull Context context, @NonNull Resource<Object> resource, int outWidth, int outHeight) {
      return resource;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {}
  }

  private static final class ValueTransformation implements Transformation<Object> {
    private final String id;

    ValueTransformation(String id) {
      this.id = id;
    }

    @NonNull
    @Override
    public Resource<Object> transform(
        @NonNull Context context, @NonNull Resource<Object> resource, int outWidth, int outHeight) {
      return resource;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof ValueTransformation) {
        ValueTransformation other = (ValueTransformation) o;
        return Objects.equals(id, other.id);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(id);
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {}
  }

  private static final class ValueModel {
    private final String id;

    ValueModel(String id) {
      this.id = id;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof ValueModel) {
        ValueModel other = (ValueModel) o;
        return Objects.equals(id, other.id);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(id);
    }
  }
}
