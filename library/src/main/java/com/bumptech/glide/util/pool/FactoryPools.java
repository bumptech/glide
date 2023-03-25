package com.bumptech.glide.util.pool;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.util.Pools.Pool;
import androidx.core.util.Pools.SimplePool;
import androidx.core.util.Pools.SynchronizedPool;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides implementations of {@link Pool} never return {@code null}, log when new instances are
 * created, and that can use the {@link com.bumptech.glide.util.pool.FactoryPools.Poolable}
 * interface to ensure objects aren't used while inside the pool.
 */
public final class FactoryPools {
  private static final String TAG = "FactoryPools";
  private static final int DEFAULT_POOL_SIZE = 20;
  private static final Resetter<Object> EMPTY_RESETTER =
      new Resetter<Object>() {
        @Override
        public void reset(@NonNull Object object) {
          // Do nothing.
        }
      };

  private FactoryPools() {}

  /**
   * Returns a non-thread safe {@link Pool} that never returns {@code null} from {@link
   * Pool#acquire()} and that contains objects of the type created by the given {@link Factory} with
   * the given maximum size.
   *
   * <p>If the pool is empty when {@link Pool#acquire()} is called, the given {@link Factory} will
   * be used to create a new instance.
   *
   * @param <T> The type of object the pool will contains.
   */
  @NonNull
  public static <T extends Poolable> Pool<T> simple(int size, @NonNull Factory<T> factory) {
    return build(new SimplePool<T>(size), factory);
  }

  /**
   * Identical to {@link #threadSafe(int, Factory, Resetter)} except no action is taken when an
   * instance is returned to the pool.
   */
  @NonNull
  public static <T extends Poolable> Pool<T> threadSafe(int size, @NonNull Factory<T> factory) {
    return build(new SynchronizedPool<T>(size), factory);
  }

  /**
   * Returns a new thread safe {@link Pool} that never returns {@code null} from {@link
   * Pool#acquire()} and that contains objects of the type created by the given {@link Factory} with
   * the given maximum size.
   *
   * <p>If the pool is empty when {@link Pool#acquire()} is called, the given {@link Factory} will
   * be used to create a new instance.
   *
   * <p>Each time an instance is returned to the pool {@code resetter} will be called with the given
   * instance.
   *
   * @param <T> The type of object the pool will contains.
   */
  @NonNull
  public static <T extends Poolable> Pool<T> threadSafe(
      int size, @NonNull Factory<T> factory, @NonNull Resetter<T> resetter) {
    return build(new SynchronizedPool<T>(size), factory, resetter);
  }

  /**
   * Returns a new {@link Pool} that never returns {@code null} and that contains {@link List Lists}
   * of a specific generic type with a standard maximum size of 20.
   *
   * <p>If the pool is empty when {@link Pool#acquire()} is called, a new {@link List} will be
   * created.
   *
   * @param <T> The type of object that the {@link List Lists} will contain.
   */
  @NonNull
  public static <T> Pool<List<T>> threadSafeList() {
    return threadSafeList(DEFAULT_POOL_SIZE);
  }

  /**
   * Returns a new thread safe {@link Pool} that never returns {@code null} and that contains {@link
   * List Lists} of a specific generic type with the given maximum size.
   *
   * <p>If the pool is empty when {@link Pool#acquire()} is called, a new {@link List} will be
   * created.
   *
   * @param <T> The type of object that the {@link List Lists} will contain.
   */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  @NonNull
  public static <T> Pool<List<T>> threadSafeList(int size) {
    return build(
        new SynchronizedPool<List<T>>(size),
        new Factory<List<T>>() {
          @NonNull
          @Override
          public List<T> create() {
            return new ArrayList<>();
          }
        },
        new Resetter<List<T>>() {
          @Override
          public void reset(@NonNull List<T> object) {
            object.clear();
          }
        });
  }

  @NonNull
  private static <T extends Poolable> Pool<T> build(
      @NonNull Pool<T> pool, @NonNull Factory<T> factory) {
    return build(pool, factory, FactoryPools.<T>emptyResetter());
  }

  @NonNull
  private static <T> Pool<T> build(
      @NonNull Pool<T> pool, @NonNull Factory<T> factory, @NonNull Resetter<T> resetter) {
    return new FactoryPool<>(pool, factory, resetter);
  }

  @NonNull
  @SuppressWarnings("unchecked")
  private static <T> Resetter<T> emptyResetter() {
    return (Resetter<T>) EMPTY_RESETTER;
  }

  /**
   * Creates new instances of the given type.
   *
   * @param <T> The type of Object that will be created.
   */
  public interface Factory<T> {
    T create();
  }

  /**
   * Resets state when objects are returned to the pool.
   *
   * @param <T> The type of Object that will be reset.
   */
  public interface Resetter<T> {
    void reset(@NonNull T object);
  }

  /**
   * Allows additional verification to catch errors caused by using objects while they are in an
   * object pool.
   */
  public interface Poolable {
    @NonNull
    StateVerifier getVerifier();
  }

  private static final class FactoryPool<T> implements Pool<T> {
    private final Factory<T> factory;
    private final Resetter<T> resetter;
    private final Pool<T> pool;

    FactoryPool(@NonNull Pool<T> pool, @NonNull Factory<T> factory, @NonNull Resetter<T> resetter) {
      this.pool = pool;
      this.factory = factory;
      this.resetter = resetter;
    }

    @Override
    public T acquire() {
      T result = pool.acquire();
      if (result == null) {
        result = factory.create();
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
          Log.v(TAG, "Created new " + result.getClass());
        }
      }
      if (result instanceof Poolable) {
        ((Poolable) result).getVerifier().setRecycled(false /*isRecycled*/);
      }
      return result;
    }

    @Override
    public boolean release(@NonNull T instance) {
      if (instance instanceof Poolable) {
        ((Poolable) instance).getVerifier().setRecycled(true /*isRecycled*/);
      }
      resetter.reset(instance);
      return pool.release(instance);
    }
  }
}
