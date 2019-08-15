package com.bumptech.glide.signature;

import android.content.Context;
import android.content.res.Configuration;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.util.Util;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

/** Includes information about the package as well as whether or not the device is in night mode. */
public final class AndroidResourceSignature implements Key {

  private final int nightMode;
  private final Key applicationVersion;

  @NonNull
  public static Key obtain(@NonNull Context context) {
    Key signature = ApplicationVersionSignature.obtain(context);
    int nightMode =
        context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    return new AndroidResourceSignature(nightMode, signature);
  }

  private AndroidResourceSignature(int nightMode, Key applicationVersion) {
    this.nightMode = nightMode;
    this.applicationVersion = applicationVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof AndroidResourceSignature) {
      AndroidResourceSignature that = (AndroidResourceSignature) o;
      return nightMode == that.nightMode && applicationVersion.equals(that.applicationVersion);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Util.hashCode(applicationVersion, nightMode);
  }

  @Override
  public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
    applicationVersion.updateDiskCacheKey(messageDigest);
    byte[] nightModeData = ByteBuffer.allocate(4).putInt(nightMode).array();
    messageDigest.update(nightModeData);
  }
}
