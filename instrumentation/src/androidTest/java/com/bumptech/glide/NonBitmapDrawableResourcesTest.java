package com.bumptech.glide;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;
import static com.bumptech.glide.request.RequestOptions.centerCropTransform;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

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
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.testutil.TearDownGlide;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NonBitmapDrawableResourcesTest {
  @Rule public final TestName testName = new TestName();
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final Context context = ApplicationProvider.getApplicationContext();

  @Test
  public void load_withBitmapResourceId_asDrawable_producesNonNullDrawable()
      throws ExecutionException, InterruptedException {
    Drawable drawable = Glide.with(context).load(android.R.drawable.star_big_off).submit().get();
    assertThat(drawable).isNotNull();
  }

  @Test
  public void load_withBitmapResourceId_asDrawable_withTransformation_producesNonNullBitmap()
      throws ExecutionException, InterruptedException {
    Drawable drawable =
        Glide.with(context)
            .load(android.R.drawable.star_big_off)
            .apply(centerCropTransform())
            .submit()
            .get();
    assertThat(drawable).isNotNull();
  }

  @Test
  public void load_withBitmapResourceId_asBitmap_producesNonNullBitmap()
      throws ExecutionException, InterruptedException {
    Bitmap bitmap =
        Glide.with(context).asBitmap().load(android.R.drawable.star_big_off).submit().get();
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void load_withBitmapAliasResourceId_asDrawable_producesNonNullDrawable()
      throws ExecutionException, InterruptedException {
    Drawable drawable = Glide.with(context).load(ResourceIds.drawable.bitmap_alias).submit().get();
    assertThat(drawable).isNotNull();
  }

  @Test
  public void load_withBitmapAliasResourceId_asDrawable_withTransformation_producesNonNullDrawable()
      throws ExecutionException, InterruptedException {
    Drawable drawable =
        Glide.with(context)
            .load(ResourceIds.drawable.bitmap_alias)
            .apply(centerCropTransform())
            .submit()
            .get();
    assertThat(drawable).isNotNull();
  }

  @Test
  public void load_withBitmapAliasResourceId_asBitmap_producesNonNullBitmap()
      throws ExecutionException, InterruptedException {
    Bitmap bitmap =
        Glide.with(context).asBitmap().load(ResourceIds.drawable.bitmap_alias).submit().get();
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void load_withShapeDrawableResourceId_asDrawable_producesNonNullDrawable()
      throws ExecutionException, InterruptedException {
    Drawable drawable =
        Glide.with(context).load(ResourceIds.drawable.shape_drawable).submit().get();
    assertThat(drawable).isNotNull();
  }

  @Test
  public void load_withShapeDrawableResourceId_asDrawable_withTransformation_sizeOriginal_fails()
      throws ExecutionException, InterruptedException {
    assertThrows(
        ExecutionException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            Glide.with(context)
                .load(ResourceIds.drawable.shape_drawable)
                .apply(centerCropTransform())
                .submit()
                .get();
          }
        });
  }

  @Test
  public void load_withShapeDrawableResourceId_asDrawable_withTransformation_validSize_succeeds()
      throws ExecutionException, InterruptedException {
    Drawable drawable =
        Glide.with(context)
            .load(ResourceIds.drawable.shape_drawable)
            .apply(bitmapTransform(new RoundedCorners(10)))
            .submit(100, 200)
            .get();
    assertThat(drawable).isNotNull();
    assertThat(drawable.getIntrinsicWidth()).isEqualTo(100);
    assertThat(drawable.getIntrinsicHeight()).isEqualTo(200);
  }

  @Test
  public void load_withShapeDrawableResourceId_asBitmap_withSizeOriginal_fails()
      throws ExecutionException, InterruptedException {
    assertThrows(
        ExecutionException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            Glide.with(context).asBitmap().load(ResourceIds.drawable.shape_drawable).submit().get();
          }
        });
  }

  @Test
  public void load_withShapeDrawableResourceId_asBitmap_withValidSize_returnsNonNullBitmap()
      throws ExecutionException, InterruptedException {
    Bitmap bitmap =
        Glide.with(context)
            .asBitmap()
            .load(ResourceIds.drawable.shape_drawable)
            .submit(100, 200)
            .get();
    assertThat(bitmap).isNotNull();
    assertThat(bitmap.getWidth()).isEqualTo(100);
    assertThat(bitmap.getHeight()).isEqualTo(200);
  }

  @Test
  public void load_withShapeDrawableResourceId_asBitmap_withValidSizeAndTransform_nonNullBitmap()
      throws ExecutionException, InterruptedException {
    Bitmap bitmap =
        Glide.with(context)
            .asBitmap()
            .load(ResourceIds.drawable.shape_drawable)
            .apply(centerCropTransform())
            .submit(100, 200)
            .get();
    assertThat(bitmap).isNotNull();
    assertThat(bitmap.getWidth()).isEqualTo(100);
    assertThat(bitmap.getHeight()).isEqualTo(200);
  }

  @Test
  public void load_withStateListDrawableResourceId_asDrawable_producesNonNullDrawable()
      throws ExecutionException, InterruptedException {
    Drawable drawable =
        Glide.with(context).load(ResourceIds.drawable.state_list_drawable).submit().get();
    assertThat(drawable).isNotNull();
  }

  @Test
  public void load_withStateListDrawableResourceId_asDrawable_withTransformation_nonNullDrawable()
      throws ExecutionException, InterruptedException {
    Drawable drawable =
        Glide.with(context)
            .load(ResourceIds.drawable.state_list_drawable)
            .apply(centerCropTransform())
            .submit()
            .get();
    assertThat(drawable).isNotNull();
  }

  @Test
  public void load_withStateListDrawableResourceId_asBitmap_producesNonNullBitmap()
      throws ExecutionException, InterruptedException {
    Bitmap bitmap =
        Glide.with(context)
            .asBitmap()
            .load(ResourceIds.drawable.state_list_drawable)
            .submit()
            .get();
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void load_withStateListDrawableResourceId_asBitmap_withTransformation_nonNullBitmap()
      throws ExecutionException, InterruptedException {
    Bitmap bitmap =
        Glide.with(context)
            .asBitmap()
            .load(ResourceIds.drawable.state_list_drawable)
            .apply(centerCropTransform())
            .submit()
            .get();
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void load_withVectorDrawableResourceId_asDrawable_producesNonNullDrawable()
      throws ExecutionException, InterruptedException {
    Drawable drawable =
        Glide.with(context).load(ResourceIds.drawable.vector_drawable).submit().get();
    assertThat(drawable).isNotNull();
  }

  @Test
  public void load_withVectorDrawableResourceId_asDrawable_withTransformation_nonNullDrawable()
      throws ExecutionException, InterruptedException {
    Drawable drawable =
        Glide.with(context)
            .load(ResourceIds.drawable.vector_drawable)
            .apply(centerCropTransform())
            .submit()
            .get();
    assertThat(drawable).isNotNull();
  }

  @Test
  public void load_withVectorDrawableResourceId_asBitmap_producesNonNullBitmap()
      throws ExecutionException, InterruptedException {
    Bitmap bitmap =
        Glide.with(context).asBitmap().load(ResourceIds.drawable.vector_drawable).submit().get();
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void load_withVectorDrawableResourceId_asBitmap_withTransformation_producesNonNullBitmap()
      throws ExecutionException, InterruptedException {
    Bitmap bitmap =
        Glide.with(context)
            .asBitmap()
            .load(ResourceIds.drawable.vector_drawable)
            .apply(centerCropTransform())
            .submit()
            .get();
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void load_withNinePatchResourceId_asDrawable_producesNonNullDrawable()
      throws ExecutionException, InterruptedException {
    Drawable drawable =
        Glide.with(context).load(ResourceIds.drawable.googlelogo_color_120x44dp).submit().get();

    assertThat(drawable).isNotNull();
  }

  @Test
  public void load_withNinePatchResourceId_asDrawable_withTransformation_producesNonNullDrawable()
      throws ExecutionException, InterruptedException {
    Drawable drawable =
        Glide.with(context)
            .load(ResourceIds.drawable.googlelogo_color_120x44dp)
            .apply(centerCropTransform())
            .submit()
            .get();

    assertThat(drawable).isNotNull();
  }

  @Test
  public void load_withNinePatchResourceId_asBitmap_producesNonNullBitmap()
      throws ExecutionException, InterruptedException {
    Bitmap bitmap =
        Glide.with(context)
            .asBitmap()
            .load(ResourceIds.drawable.googlelogo_color_120x44dp)
            .submit()
            .get();

    assertThat(bitmap).isNotNull();
  }

  @Test
  public void load_withNinePatchResourceId_asBitmap_withTransformation_producesNonNullBitmap()
      throws ExecutionException, InterruptedException {
    Bitmap bitmap =
        Glide.with(context)
            .asBitmap()
            .load(ResourceIds.drawable.googlelogo_color_120x44dp)
            .apply(centerCropTransform())
            .submit()
            .get();

    assertThat(bitmap).isNotNull();
  }

  @Test
  public void load_withApplicationIconResourceIdUri_asDrawable_producesNonNullDrawable()
      throws NameNotFoundException, ExecutionException, InterruptedException {
    for (String packageName : getInstalledPackages()) {
      int iconResourceId = getResourceId(packageName);

      Uri uri =
          new Uri.Builder()
              .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
              .authority(packageName)
              .path(String.valueOf(iconResourceId))
              .build();

      Drawable drawable = Glide.with(context).load(uri).submit().get();
      assertThat(drawable).isNotNull();
    }
  }

  @Test
  public void load_withApplicationIconResourceIdUri_asDrawable_withTransformation_nonNullDrawable()
      throws NameNotFoundException, ExecutionException, InterruptedException {
    for (String packageName : getInstalledPackages()) {
      int iconResourceId = getResourceId(packageName);

      Uri uri =
          new Uri.Builder()
              .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
              .authority(packageName)
              .path(String.valueOf(iconResourceId))
              .build();

      Drawable drawable = Glide.with(context).load(uri).apply(centerCropTransform()).submit().get();
      assertThat(drawable).isNotNull();
    }
  }

  @Test
  public void load_withApplicationIconResourceIdUri_asBitmap_producesNonNullBitmap()
      throws NameNotFoundException, ExecutionException, InterruptedException {
    for (String packageName : getInstalledPackages()) {
      int iconResourceId = getResourceId(packageName);

      Uri uri =
          new Uri.Builder()
              .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
              .authority(packageName)
              .path(String.valueOf(iconResourceId))
              .build();

      Bitmap bitmap = Glide.with(context).asBitmap().load(uri).submit().get();
      assertThat(bitmap).isNotNull();
    }
  }

  @Test
  public void load_withApplicationIconResourceIdUri_asBitmap_withTransformation_nonNullBitmap()
      throws ExecutionException, InterruptedException {
    for (String packageName : getInstalledPackages()) {
      int iconResourceId = getResourceId(packageName);

      Uri uri =
          new Uri.Builder()
              .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
              .authority(packageName)
              .path(String.valueOf(iconResourceId))
              .build();

      Bitmap bitmap =
          Glide.with(context).asBitmap().apply(centerCropTransform()).load(uri).submit().get();
      assertThat(bitmap).isNotNull();
    }
  }

  @Test
  public void load_withApplicationIconResourceNameUri_asDrawable_producesNonNullDrawable()
      throws ExecutionException, InterruptedException, NameNotFoundException {
    for (String packageName : getInstalledPackages()) {
      int iconResourceId = getResourceId(packageName);

      Context toUse = context.createPackageContext(packageName, /* flags= */ 0);
      Resources resources = toUse.getResources();
      Uri uri =
          new Uri.Builder()
              .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
              .authority(packageName)
              .appendPath(resources.getResourceTypeName(iconResourceId))
              .appendPath(resources.getResourceEntryName(iconResourceId))
              .build();

      Drawable drawable = Glide.with(context).load(uri).submit().get();
      assertThat(drawable).isNotNull();
    }
  }

  @Test
  public void load_withApplicationIconResourceNameUri_asDrawable_withTransform_nonNullDrawable()
      throws ExecutionException, InterruptedException, NameNotFoundException {
    for (String packageName : getInstalledPackages()) {
      int iconResourceId = getResourceId(packageName);

      Context toUse = context.createPackageContext(packageName, /* flags= */ 0);
      Resources resources = toUse.getResources();
      Uri uri =
          new Uri.Builder()
              .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
              .authority(packageName)
              .appendPath(resources.getResourceTypeName(iconResourceId))
              .appendPath(resources.getResourceEntryName(iconResourceId))
              .build();

      Drawable drawable = Glide.with(context).load(uri).apply(centerCropTransform()).submit().get();
      assertThat(drawable).isNotNull();
    }
  }

  @Test
  public void load_withApplicationIconResourceNameUri_asBitmap_producesNonNullBitmap()
      throws ExecutionException, InterruptedException, NameNotFoundException {
    for (String packageName : getInstalledPackages()) {
      int iconResourceId = getResourceId(packageName);

      Context toUse = context.createPackageContext(packageName, /* flags= */ 0);
      Resources resources = toUse.getResources();
      Uri uri =
          new Uri.Builder()
              .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
              .authority(packageName)
              .appendPath(resources.getResourceTypeName(iconResourceId))
              .appendPath(resources.getResourceEntryName(iconResourceId))
              .build();

      Bitmap bitmap = Glide.with(context).asBitmap().load(uri).submit().get();
      assertThat(bitmap).isNotNull();
    }
  }

  @Test
  public void load_withApplicationIconResourceNameUri_asBitmap_withTransform_nonNullBitmap()
      throws ExecutionException, InterruptedException, NameNotFoundException {
    for (String packageName : getInstalledPackages()) {
      int iconResourceId = getResourceId(packageName);

      Context toUse = context.createPackageContext(packageName, /* flags= */ 0);
      Resources resources = toUse.getResources();
      Uri uri =
          new Uri.Builder()
              .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
              .authority(packageName)
              .appendPath(resources.getResourceTypeName(iconResourceId))
              .appendPath(resources.getResourceEntryName(iconResourceId))
              .build();

      Bitmap bitmap =
          Glide.with(context).asBitmap().apply(centerCropTransform()).load(uri).submit().get();
      assertThat(bitmap).isNotNull();
    }
  }

  private Set<String> getInstalledPackages() {
    Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
    PackageManager packageManager = context.getPackageManager();
    List<ResolveInfo> pkgAppsList =
        packageManager.queryIntentActivities(mainIntent, /* flags= */ 0);
    Set<String> result = new HashSet<>();
    for (ResolveInfo info : pkgAppsList) {
      String packageName = info.activityInfo.packageName;
      int iconResourceId = getResourceId(packageName);
      if (iconResourceId != 0
          && doesApplicationPackageNameMatchResourcePackageName(packageName, iconResourceId)) {
        result.add(info.activityInfo.packageName);
      }
    }
    return result;
  }

  private int getResourceId(String packageName) {
    PackageInfo packageInfo;
    try {
      packageInfo = context.getPackageManager().getPackageInfo(packageName, /* flags= */ 0);
    } catch (NameNotFoundException e) {
      return 0;
    }
    return packageInfo.applicationInfo.icon;
  }

  /**
   * Returns {@code true} iff the resource package name is exactly the same as the containing
   * application package name for a given resource id.
   *
   * <p>The resource package name is the value returned by {@link
   * Resources#getResourcePackageName(int)}. The application package name is package name of the
   * enclosing application. If these two things are equal, then we can both construct a Context for
   * that package and retrieve a resource id for that package from a "standard" resource Uri
   * containing a name instead of an id. If they aren't equal, then we can do only one of the two
   * required tasks, so our Uri load will always fail. To handle this properly, we'd need callers to
   * include both package names in the Uri. I'm not aware of any standardized Uri format for doing
   * so, so these requests will just be treated as unsupported for the time being.
   *
   * <p>Take Calendar (emulators API 24 and below) as an example:
   *
   * <ul>
   *   <li>package name: com.google.android.calendar
   *   <li>resource package name: com.android.calendar
   * </ul>
   *
   * We can construct one of two possible Uris:
   *
   * <ul>
   *   <li>android.resource://com.google.android.calendar/mipmap/ic_icon_calendar.
   *   <li>android.resource://com.android.calendar/mipmap/ic_icon_calendar.<
   * </ul>
   *
   * From the first Uri, we can obtain the correct Context/Resources for the calendar package, but
   * our attempts to resolve the correct resource id will fail because we do not have the resource
   * package name. From the second Uri we cannot obtain the Context/Resources for the calendar
   * package because the resource package name doesn't match the application package name.
   */
  private boolean doesApplicationPackageNameMatchResourcePackageName(
      String applicationPackageName, int iconResourceId) {
    try {
      Context current = context.createPackageContext(applicationPackageName, /* flags= */ 0);
      String resourcePackageName = current.getResources().getResourcePackageName(iconResourceId);
      return applicationPackageName.equals(resourcePackageName);
    } catch (NameNotFoundException e) {
      // This should never happen
      throw new RuntimeException(e);
    }
  }
}
