package com.bumptech.glide.module;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses {@link com.bumptech.glide.module.GlideModule} references out of the AndroidManifest file.
 */
public final class ManifestParser {
  private static final String TAG = "ManifestParser";
  private static final String GLIDE_MODULE_VALUE = "GlideModule";

  private final Context context;

  public ManifestParser(Context context) {
    this.context = context;
  }

  @SuppressWarnings("deprecation")
  public List<GlideModule> parse() {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "Loading Glide modules");
    }
    List<GlideModule> modules = new ArrayList<>();
    try {
      ApplicationInfo appInfo = context.getPackageManager()
          .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
      if (appInfo.metaData == null) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "Got null app info metadata");
        }
        return modules;
      }
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Got app info metadata: " + appInfo.metaData);
      }
      for (String key : appInfo.metaData.keySet()) {
        if (GLIDE_MODULE_VALUE.equals(appInfo.metaData.get(key))) {
          modules.add(parseModule(key));
          if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Loaded Glide module: " + key);
          }
        }
      }
    } catch (PackageManager.NameNotFoundException e) {
      throw new RuntimeException("Unable to find metadata to parse GlideModules", e);
    }
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "Finished loading Glide modules");
    }

    return modules;
  }

  @SuppressWarnings("deprecation")
  private static GlideModule parseModule(String className) {
    Class<?> clazz;
    try {
      clazz = Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Unable to find GlideModule implementation", e);
    }

    Object module;
    try {
      module = clazz.newInstance();
    } catch (InstantiationException e) {
      throw new RuntimeException("Unable to instantiate GlideModule implementation for " + clazz,
              e);
      // These can't be combined until API minimum is 19.
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Unable to instantiate GlideModule implementation for " + clazz,
              e);
    }

    if (!(module instanceof GlideModule)) {
      throw new RuntimeException("Expected instanceof GlideModule, but found: " + module);
    }
    return (GlideModule) module;
  }
}
