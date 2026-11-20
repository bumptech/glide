package com.bumptech.glide.util;

import static com.google.common.truth.Truth.assertThat;

import com.bumptech.glide.util.EqualityKeyProxy.StrongProxy;
import com.bumptech.glide.util.EqualityKeyProxy.WeakProxy;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.GcFinalization;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import java.lang.ref.WeakReference;
import java.util.Objects;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public class EqualityKeyProxyTest {

  @Test
  public void of_distinguishesIdentityAndValueTypes(@TestParameter boolean allowWeakProxy) {
    IdentityObject identityObject = new IdentityObject();
    ValueObject valueObject = new ValueObject("id");
    InheritedValueObject inheritedValueObject = new InheritedValueObject("id");

    EqualityKeyProxy<IdentityObject> wrappedIdentity =
        EqualityKeyProxy.of(identityObject, allowWeakProxy);
    EqualityKeyProxy<ValueObject> wrappedValue = EqualityKeyProxy.of(valueObject, allowWeakProxy);
    EqualityKeyProxy<InheritedValueObject> wrappedInherited =
        EqualityKeyProxy.of(inheritedValueObject, allowWeakProxy);

    assertThat(wrappedIdentity).isInstanceOf(allowWeakProxy ? WeakProxy.class : StrongProxy.class);
    assertThat(wrappedValue).isInstanceOf(StrongProxy.class);
    assertThat(wrappedInherited).isInstanceOf(StrongProxy.class);
  }

  @Test
  public void ofNullable_handlesNullAndNonNull(@TestParameter boolean allowWeakProxy) {
    Object identity = new Object();
    EqualityKeyProxy<Object> wrapped = EqualityKeyProxy.ofNullable(identity, allowWeakProxy);

    assertThat(wrapped).isInstanceOf(allowWeakProxy ? WeakProxy.class : StrongProxy.class);
    assertThat(wrapped).isEqualTo(EqualityKeyProxy.of(identity, allowWeakProxy));
    assertThat(EqualityKeyProxy.ofNullable(null, allowWeakProxy)).isNull();
  }

  @Test
  public void equalsAndHashCode_followsContractForWeakAndStrongProxies() {
    Object first = new Object();
    Object second = new Object();

    new EqualsTester()
        .addEqualityGroup(EqualityKeyProxy.of(first), EqualityKeyProxy.of(first))
        .addEqualityGroup(EqualityKeyProxy.of(second))
        .addEqualityGroup(
            EqualityKeyProxy.of(new ValueObject("a")), EqualityKeyProxy.of(new ValueObject("a")))
        .addEqualityGroup(EqualityKeyProxy.of(new ValueObject("b")))
        .testEquals();
  }

  @Test
  public void weakProxy_afterGc_preservesHashCodeAndSelfEqualityOnly() {
    Object shared = new Object();
    int expectedHashCode = System.identityHashCode(shared);
    EqualityKeyProxy<Object> first = EqualityKeyProxy.of(shared);
    EqualityKeyProxy<Object> second = EqualityKeyProxy.of(shared);
    WeakReference<Object> sharedRef = new WeakReference<>(shared);

    assertThat(first.equals(first)).isTrue();
    assertThat(first.equals(second)).isTrue();
    assertThat(second.equals(first)).isTrue();

    shared = null;

    GcFinalization.awaitClear(sharedRef);

    assertThat(first.hashCode()).isEqualTo(expectedHashCode);
    assertThat(first.equals(first)).isTrue();
    assertThat(first.equals(second)).isFalse();
    assertThat(second.equals(first)).isFalse();
  }

  @Test
  public void gc_withIdentityEqualReferent_collectsReferentWhileProxyIsRetained() {
    Object referent = new Object();
    EqualityKeyProxy<Object> unusedRetainedProxy = EqualityKeyProxy.of(referent);
    WeakReference<Object> referentRef = new WeakReference<>(referent);
    referent = null;

    GcFinalization.awaitClear(referentRef);

    assertThat(referentRef.get()).isNull();
  }

  @Test
  public void gc_withValueEqualReferent_retainsReferentStrongly() {
    ValueObject referent = new ValueObject("retained");
    EqualityKeyProxy<ValueObject> unusedRetainedProxy = EqualityKeyProxy.of(referent);
    WeakReference<ValueObject> referentRef = new WeakReference<>(referent);
    referent = null;

    GcFinalization.awaitFullGc();

    assertThat(referentRef.get()).isNotNull();
  }

  private static final class IdentityObject {}

  private static class ValueObject {
    private final String id;

    ValueObject(String id) {
      this.id = id;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof ValueObject) {
        ValueObject other = (ValueObject) o;
        return Objects.equals(id, other.id);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(id);
    }

    @Override
    public String toString() {
      return "ValueObject{" + id + '}';
    }
  }

  private static final class InheritedValueObject extends ValueObject {
    InheritedValueObject(String id) {
      super(id);
    }
  }
}
