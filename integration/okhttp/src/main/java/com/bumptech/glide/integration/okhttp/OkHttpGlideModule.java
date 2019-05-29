package com.bumptech.glide.integration.okhttp;

import android.content.Context;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.model.GlideUrl;
import java.io.InputStream;

/**
 * A {@link com.bumptech.glide.module.GlideModule} implementation to replace Glide's default {@link
 * java.net.HttpURLConnection} based {@link com.bumptech.glide.load.model.ModelLoader} with an
 * OkHttp based {@link com.bumptech.glide.load.model.ModelLoader}.
 *
 * <p>If you're using gradle, you can include this module simply by depending on the aar, the module
 * will be merged in by manifest merger. For other build systems or for more more information, see
 * {@link com.bumptech.glide.module.GlideModule}.
 *
 * @deprecated replaced with com.bumptech.glide.integration.okhttp3.OkHttpGlideModule.
 */
@Deprecated
public class OkHttpGlideModule implements com.bumptech.glide.module.GlideModule {
  @Override
  public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
    // Do nothing.
  }

  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory());
  }
}
