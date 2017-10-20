package com.bumptech.glide.test;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.bumptech.glide.Glide;
import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestNonBitmapResources {
  private static final ImmutableList<String> PACKAGES = ImmutableList.of(
      "com.android.gallery3d",
      "com.android.contacts");

  private Context context;

  @Before
  public void setUp() {
    context = InstrumentationRegistry.getTargetContext();
  }

  @After
  public void tearDown() {
    Glide.tearDown();
  }

  @Test
  public void load_withBitmapResourceId_asDrawable_producesNonNullDrawable()
      throws ExecutionException, InterruptedException {
    Drawable drawable = Glide.with(context)
        .load(android.R.drawable.star_big_off)
        .submit()
        .get();
    assertThat(drawable).isNotNull();
  }

  @Test
  public void load_withBitmapResourceId_asBitmap_producesNonNullBitmap()
      throws ExecutionException, InterruptedException {
    Bitmap bitmap = Glide.with(context)
        .asBitmap()
        .load(android.R.drawable.star_big_off)
        .submit()
        .get();
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void load_withBitmapAliasResourceId_asDrawable_producesNonNullDrawable()
      throws ExecutionException, InterruptedException {
    Drawable drawable = Glide.with(context)
        .load(R.drawable.bitmap_alias)
        .submit()
        .get();
    assertThat(drawable).isNotNull();
  }

  @Test
  public void load_withShapeDrawableResourceId_asDrawable_producesNonNullDrawable()
      throws ExecutionException, InterruptedException {
    Drawable drawable = Glide.with(context)
        .load(R.drawable.shape_drawable)
        .submit()
        .get();
    assertThat(drawable).isNotNull();
  }

  @Test
  public void load_withStateListDrawableResourceId_asDrawable_producesNonNullDrawable()
      throws ExecutionException, InterruptedException {
    Drawable drawable = Glide.with(context)
        .load(R.drawable.state_list_drawable)
        .submit()
        .get();
    assertThat(drawable).isNotNull();
  }

  @Test
  public void load_withVectorDrawableResourceId_asDrawable_producesNonNullDrawable()
      throws ExecutionException, InterruptedException {
    Drawable drawable = Glide.with(context)
        .load(R.drawable.vector_drawable)
        .submit()
        .get();
    assertThat(drawable).isNotNull();
  }

  @Test
  public void load_withNinePatchResourceId_asDrawable_producesNonNullDrawable()
      throws ExecutionException, InterruptedException {
    Drawable drawable = Glide.with(context)
        .load(R.drawable.googlelogo_color_120x44dp)
        .submit()
        .get();

    assertThat(drawable).isNotNull();
  }

  @Test
  public void load_withApplicationIconResourceIdUri_asDrawable_producesNonNullDrawable()
      throws NameNotFoundException, ExecutionException, InterruptedException {
    for (String packageName : getInstalledPackages()) {
      int iconResourceId = getResourceId(packageName);

      Uri uri = new Uri.Builder()
          .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
          .authority(packageName)
          .path(String.valueOf(iconResourceId))
          .build();

      Drawable drawable = Glide.with(context)
          .load(uri)
          .submit()
          .get();
      assertThat(drawable).isNotNull();
    }
  }

  @Test
  public void load_withApplicationIconResourceNameUri_asDrawable_producesNonNullDrawable()
      throws ExecutionException, InterruptedException, NameNotFoundException {
    for (String packageName : getInstalledPackages()) {
      int iconResourceId = getResourceId(packageName);

      Context toUse = context.createPackageContext(packageName, /*flags=*/ 0);
      Resources resources = toUse.getResources();
      Uri uri = new Uri.Builder()
          .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
          .authority(packageName)
          .path(resources.getResourceTypeName(iconResourceId))
          .path(resources.getResourceEntryName(iconResourceId))
          .path(String.valueOf(iconResourceId))
          .build();

      Drawable drawable = Glide.with(context)
          .load(uri)
          .submit()
          .get();
      assertThat(drawable).isNotNull();
    }
  }

  private Set<String> getInstalledPackages() {
    Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
    PackageManager packageManager = context.getPackageManager();
    List<ResolveInfo> pkgAppsList =
        packageManager.queryIntentActivities(mainIntent, /*flags=*/ 0);
    Set<String> result = new HashSet<>();
    for (ResolveInfo info : pkgAppsList) {
      int iconResourceId = getResourceId(info.activityInfo.packageName);
      if (iconResourceId != 0) {
        result.add(info.activityInfo.packageName);
      }
    }
    return result;
  }

  private int getResourceId(String packageName) {
    PackageInfo packageInfo;
    try {
      packageInfo = context.getPackageManager().getPackageInfo(packageName, /*flags=*/ 0);
    } catch (NameNotFoundException e) {
      return 0;
    }
    return packageInfo.applicationInfo.icon;
  }
}
