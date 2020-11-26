package com.bumptech.glide;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;
import com.bumptech.glide.test.AppModuleWithExcludes;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Generated;

@SuppressWarnings("deprecation")
@Generated("com.bumptech.glide.annotation.compiler.AppModuleGenerator")
final class GeneratedAppGlideModuleImpl extends GeneratedAppGlideModule {
  private final AppModuleWithExcludes appGlideModule;

  public GeneratedAppGlideModuleImpl(Context context) {
    appGlideModule = new AppModuleWithExcludes();
    if (Log.isLoggable("Glide", Log.DEBUG)) {
      Log.d("Glide", "Discovered AppGlideModule from annotation: com.bumptech.glide.test.AppModuleWithExcludes");
      Log.d("Glide", "AppGlideModule excludes LibraryGlideModule from annotation: com.bumptech.glide.test.EmptyLibraryModule");
    }
  }

  @Override
  public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
    appGlideModule.applyOptions(context, builder);
  }

  @Override
  public void registerComponents(@NonNull Context context, @NonNull Glide glide,
      @NonNull Registry registry) {
    appGlideModule.registerComponents(context, glide, registry);
  }

  @Override
  public boolean isManifestParsingEnabled() {
    return appGlideModule.isManifestParsingEnabled();
  }

  @Override
  @NonNull
  public Set<Class<?>> getExcludedModuleClasses() {
    Set<Class<?>> excludedClasses = new HashSet<Class<?>>();
    excludedClasses.add(com.bumptech.glide.test.EmptyLibraryModule.class);
    return excludedClasses;
  }

  @Override
  @NonNull
  GeneratedRequestManagerFactory getRequestManagerFactory() {
    return new GeneratedRequestManagerFactory();
  }
}
