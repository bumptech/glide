package com.bumptech.glide.tests;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;

import android.content.Context;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.Resource;

public final class Util {
  private Util() { }

  @SuppressWarnings("unchecked")
  public static <T> Resource<T> mockResource() {
    return mock(Resource.class);
  }

  public static Context anyContext() {
    return any(Context.class);
  }

  /**
   * Creates a Mockito argument matcher to be used in verify.
   * It returns a generic typed {@link Resource} to prevent unchecked warnings.
   */
  @SuppressWarnings("unchecked")
  public static <T> Resource<T> anyResource() {
    return any(Resource.class);
  }

  public static DataSource isADataSource() {
    return isA(DataSource.class);
  }
}
