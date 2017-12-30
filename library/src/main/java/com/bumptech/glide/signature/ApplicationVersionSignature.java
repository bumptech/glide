package com.bumptech.glide.signature;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import com.bumptech.glide.load.Key;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A utility class for obtaining a {@link com.bumptech.glide.load.Key} signature containing the
 * application version name using {@link android.content.pm.PackageInfo#versionCode}.
 */
public final class ApplicationVersionSignature {
  private static final ConcurrentHashMap<String, Key> PACKAGE_NAME_TO_KEY =
      new ConcurrentHashMap<>();

  /**
   * Returns the signature {@link com.bumptech.glide.load.Key} for version code of the Application
   * of the given Context.
   */
  @NonNull
  public static Key obtain(@NonNull Context context) {
    String packageName = context.getPackageName();
    Key result = PACKAGE_NAME_TO_KEY.get(packageName);
    if (result == null) {
      Key toAdd = obtainVersionSignature(context);
      result = PACKAGE_NAME_TO_KEY.putIfAbsent(packageName, toAdd);
      // There wasn't a previous mapping, so toAdd is now the Key.
      if (result == null) {
        result = toAdd;
      }
    }

    return result;
  }

  @VisibleForTesting
  static void reset() {
    PACKAGE_NAME_TO_KEY.clear();
  }

  @NonNull
  private static Key obtainVersionSignature(@NonNull Context context) {
    PackageInfo pInfo;
    try {
      pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
    } catch (PackageManager.NameNotFoundException e) {
      throw new RuntimeException(e);
    }
    final String versionCode;
    if (pInfo != null) {
      versionCode = String.valueOf(pInfo.versionCode);
    } else {
      versionCode = UUID.randomUUID().toString();
    }
    return new ObjectKey(versionCode);
  }

  private ApplicationVersionSignature() {
    // Empty for visibility.
  }
}
