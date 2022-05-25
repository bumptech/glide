package com.bumptech.glide;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.bumptech.glide.test.AppModuleWithLibraryInPackage;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("deprecation")
final class GeneratedAppGlideModuleImpl extends GeneratedAppGlideModule {
  private final AppModuleWithLibraryInPackage appGlideModule;

  public GeneratedAppGlideModuleImpl(Context context) {
    appGlideModule = new AppModuleWithLibraryInPackage();
    if (Log.isLoggable("Glide", Log.DEBUG)) {
      Log.d("Glide", "Discovered AppGlideModule from annotation: com.bumptech.glide.test.AppModuleWithLibraryInPackage");
      Log.d("Glide", "AppGlideModule excludes LibraryGlideModule from annotation: com.bumptech.glide.test._package.LibraryModuleInPackage");
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
    excludedClasses.add(com.bumptech.glide.test._package.LibraryModuleInPackage.class);
    return excludedClasses;
  }

  @Override
  @NonNull
  GeneratedRequestManagerFactory getRequestManagerFactory() {
    return new GeneratedRequestManagerFactory();
  }
}
