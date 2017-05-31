package com.bumptech.glide.module;

import android.content.Context;
import com.bumptech.glide.Registry;

/**
 * An internal interface, to be removed when {@link GlideModule}s are removed.
 */
interface RegistersComponents {

  /**
   * Lazily register components immediately after the Glide singleton is created but before any
   * requests can be started.
   *
   * <p> This method will be called once and only once per implementation. </p>
   *
   * @param context  An Application {@link android.content.Context}.
   * @param registry An {@link com.bumptech.glide.Registry} to use to register components.
   */
  void registerComponents(Context context, Registry registry);
}
