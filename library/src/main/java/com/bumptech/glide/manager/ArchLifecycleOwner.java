package com.bumptech.glide.manager;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.support.annotation.NonNull;

/**
 * The simple wrapper around Context and Lifecycle.
 *
 * @see Lifecycle
 */
public class ArchLifecycleOwner implements LifecycleOwner {

  private final Context context;

  private final Lifecycle lifecycle;

  public ArchLifecycleOwner(@NonNull Context context, @NonNull Lifecycle lifecycle) {
    this.context = context;
    this.lifecycle = lifecycle;
  }

  @NonNull
  public Context getContext() {
    return context;
  }

  @NonNull
  @Override
  public Lifecycle getLifecycle() {
    return lifecycle;
  }

}
