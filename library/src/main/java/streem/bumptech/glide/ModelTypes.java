package com.bumptech.glide;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.CheckResult;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import java.io.File;
import java.net.URL;

/**
 * Ensures that the set of explicitly supported model types remains consistent across Glide's API
 * surface.
 */
interface ModelTypes<T> {
  @NonNull
  @CheckResult
  T load(@Nullable Bitmap bitmap);

  @NonNull
  @CheckResult
  T load(@Nullable Drawable drawable);

  @NonNull
  @CheckResult
  T load(@Nullable String string);

  @NonNull
  @CheckResult
  T load(@Nullable Uri uri);

  @NonNull
  @CheckResult
  T load(@Nullable File file);

  @NonNull
  @CheckResult
  T load(@RawRes @DrawableRes @Nullable Integer resourceId);

  @Deprecated
  @CheckResult
  T load(@Nullable URL url);

  @NonNull
  @CheckResult
  T load(@Nullable byte[] model);

  @NonNull
  @CheckResult
  @SuppressWarnings("unchecked")
  T load(@Nullable Object model);
}
