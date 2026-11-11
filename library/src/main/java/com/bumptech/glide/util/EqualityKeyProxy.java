package com.bumptech.glide.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A proxy for a cache key component that delegates {@link #equals(Object)}, {@link #hashCode()},
 * and {@link #toString()} to the underlying object while retaining value-equal objects strongly and
 * identity-equal objects weakly.
 *
 * <p>This helps maintain Glide's support for arbitrary model types (literally {@code
 * java.lang.Object}) in expressions such as {@code load(uri ?: bitmap)} while allowing Glide to
 * avoid holding strong references where it doesn't need them.
 *
 * @param <T> The type of the proxied object.
 */
//
// Having a time-dependent definition of equality is weird, so here's a rough proof of correctness.
// (For brevity, "strongly-reachable" below means "strongly- or softly- reachable".)
//
// We assume:
// * A type system where `equals` forms a proper equivalence relation (i.e. reflexive, symmetric,
//   and transitive).
// * Hash codes of the proxied objects/references do not change.
//
// We guarantee:
// * `EKP(o)` is always equal to itself.
// * For `o1` which uses value-equality, if `o1.equals(o2)` then `EKP(o1).equals(EKP(o2))`.
//   This follows because symmetry dictates that `o2` also uses value-equality.
// * For `o` which uses identity-equality, `EKP(o).equals(EKP(o))` when the object pointed to
//   by `o` is strongly-reachable.  This is enforced by comparing the referents.
// * For `o1` which uses identity-equality, if `o1 != o2` then `!EKP(o1).equals(EKP(o2))`, always.
// * Hash codes of the proxies do not change.
//
// So what changed about the pointer semantics?
// * For `o` which uses identity-equality and whose object isn't strongly-reachable, the value of
//   `EKP(o).equals(EKP(o))` is undefined.
//
// What works?
// * Querying hashmaps with strongly-reachable objects.  In particular, `Engine.load` queries caches
//   synchronously while holding strong references.
// * Removing items from hashmaps with the same `EKP(o)` instance as was used to insert it.
//   (`ActiveResources` needs this for its cleanup work.)
// * Inserting different keys into hashmaps.  If an image request finishes after its key became
//   weak, inserting the key into a map will maintain structural invariants.
//
// What won't work?  What can't we do?
// * Inserting two separate copies of `EKP(o)` into a map when the object is weakly-reachable.
//
public abstract class EqualityKeyProxy<T> {
  private static final Map<Class<?>, Boolean> HAS_IDENTITY_EQUALS =
      Collections.synchronizedMap(new WeakHashMap<>());

  /**
   * Wraps {@code value} in a weak {@link EqualityKeyProxy} if its equality is identity-based
   * ({@link Object#equals(Object)} or arrays), or in a strong {@link EqualityKeyProxy} if it
   * overrides {@link Object#equals(Object)}.
   */
  @NonNull
  public static <T> EqualityKeyProxy<T> of(@NonNull T value) {
    Preconditions.checkNotNull(value);
    return isIdentityEqual(value) ? new WeakProxy<>(value) : new StrongProxy<>(value);
  }

  /** Nullable variant of {@link #of(Object)}. */
  @Nullable
  public static <T> EqualityKeyProxy<T> ofNullable(@Nullable T value) {
    return value == null ? null : of(value);
  }

  @Override
  public abstract boolean equals(Object o);

  @Override
  public abstract int hashCode();

  @Override
  public abstract String toString();

  private static boolean isIdentityEqual(@NonNull Object value) {
    Class<?> clazz = value.getClass();
    if (clazz.isArray()) {
      // Fast-path check.
      return true;
    }
    Boolean hasIdentityEquals = HAS_IDENTITY_EQUALS.get(clazz);
    if (hasIdentityEquals == null) {
      hasIdentityEquals = hasIdentityEquals(clazz);
      HAS_IDENTITY_EQUALS.put(clazz, hasIdentityEquals);
    }
    return hasIdentityEquals;
  }

  private static boolean hasIdentityEquals(Class<?> clazz) {
    try {
      return clazz.getMethod("equals", Object.class).getDeclaringClass() == Object.class;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  @VisibleForTesting
  static final class WeakProxy<T> extends EqualityKeyProxy<T> {
    private final WeakReference<T> reference;
    private final Class<?> clazz;
    private final int hashCode;

    WeakProxy(@NonNull T referent) {
      this.reference = new WeakReference<>(referent);
      this.clazz = referent.getClass();
      this.hashCode = System.identityHashCode(referent);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof WeakProxy)) {
        return false;
      }
      WeakProxy<?> other = (WeakProxy<?>) o;
      T referent = reference.get();
      return referent != null && referent == other.reference.get();
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public String toString() {
      return "WeakProxy{class="
          + clazz
          + ", hashCode="
          + hashCode
          + ", null?="
          + (reference.get() == null)
          + '}';
    }
  }

  @VisibleForTesting
  static final class StrongProxy<T> extends EqualityKeyProxy<T> {
    private final T referent;

    StrongProxy(@NonNull T referent) {
      this.referent = referent;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof StrongProxy)) {
        return false;
      }
      StrongProxy<?> other = (StrongProxy<?>) o;
      return referent.equals(other.referent);
    }

    @Override
    public int hashCode() {
      return referent.hashCode();
    }

    @Override
    public String toString() {
      // Delegates directly to avoid changing SHA hashes from ObjectKey.
      return referent.toString();
    }
  }

  // Package-private to prevent external subclassing while avoiding a synthetic accessor for nested
  // subclasses.
  EqualityKeyProxy() {}
}
