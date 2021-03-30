package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.test.GlideApp;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.testutil.ConcurrencyHelper;
import com.bumptech.glide.testutil.TearDownGlide;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

/**
 * Tests that Glide is able to load videos stored in resources and loaded as {@link
 * android.content.res.AssetFileDescriptor}s.
 */
@RunWith(AndroidJUnit4.class)
public class LoadVideoResourceTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();

  private Context context;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    context = ApplicationProvider.getApplicationContext();
  }

  @Test
  public void loadVideoResourceId_fromInt_decodesFrame() {
    Drawable frame = concurrency.get(Glide.with(context).load(ResourceIds.raw.video).submit());

    assertThat(frame).isNotNull();
  }

  @Test
  public void loadVideoResourceId_fromInt_withFrameTime_decodesFrame() {
    Drawable frame =
        concurrency.get(
            GlideApp.with(context)
                .load(ResourceIds.raw.video)
                .frame(TimeUnit.SECONDS.toMicros(1))
                .submit());

    assertThat(frame).isNotNull();
  }

  // Testing boxed integer.
  @SuppressWarnings("UnnecessaryBoxing")
  @Test
  public void loadVideoResourceId_fromInteger_decodesFrame() {
    Drawable frame =
        concurrency.get(Glide.with(context).load(new Integer(ResourceIds.raw.video)).submit());

    assertThat(frame).isNotNull();
  }

  // Testing boxed integer.
  @SuppressWarnings("UnnecessaryBoxing")
  @Test
  public void loadVideoResourceId_fromInteger_withFrameTime_decodesFrame() {
    Drawable frame =
        concurrency.get(
            GlideApp.with(context)
                .load(new Integer(ResourceIds.raw.video))
                .frame(TimeUnit.SECONDS.toMicros(1))
                .submit());

    assertThat(frame).isNotNull();
  }

  @Test
  public void loadVideoResourceId_asBitmap_decodesFrame() {
    Bitmap frame =
        concurrency.get(Glide.with(context).asBitmap().load(ResourceIds.raw.video).submit());

    assertThat(frame).isNotNull();
  }

  @Test
  public void loadVideoResourceId_asBitmap_withFrameTime_decodesFrame() {
    Bitmap frame =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .load(ResourceIds.raw.video)
                .frame(TimeUnit.SECONDS.toMicros(1))
                .submit());

    assertThat(frame).isNotNull();
  }

  @Test
  public void loadVideoResourceUri_fromId_decodesFrame() {
    Uri uri =
        new Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.getPackageName())
            .path(String.valueOf(ResourceIds.raw.video))
            .build();

    Drawable frame = concurrency.get(GlideApp.with(context).load(uri).submit());

    assertThat(frame).isNotNull();
  }

  @Test
  public void loadVideoResourceUri_asBitmap_fromId_decodesFrame() {
    Uri uri =
        new Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.getPackageName())
            .path(String.valueOf(ResourceIds.raw.video))
            .build();

    Bitmap frame = concurrency.get(GlideApp.with(context).asBitmap().load(uri).submit());

    assertThat(frame).isNotNull();
  }

  @Test
  public void loadVideoResourceUri_fromId_withFrame_decodesFrame() {
    Uri uri =
        new Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.getPackageName())
            .path(String.valueOf(ResourceIds.raw.video))
            .build();

    Bitmap frame =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .load(uri)
                .frame(TimeUnit.SECONDS.toMicros(1))
                .submit());

    assertThat(frame).isNotNull();
  }

  @Test
  public void loadVideoResourceUriString_fromId_decodesFrame() {
    Uri uri =
        new Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.getPackageName())
            .path(String.valueOf(ResourceIds.raw.video))
            .build();

    Bitmap frame = concurrency.get(GlideApp.with(context).asBitmap().load(uri.toString()).submit());

    assertThat(frame).isNotNull();
  }

  @Test
  public void loadVideoResourceUriString_fromId_withFrame_decodesFrame() {
    Uri uri =
        new Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.getPackageName())
            .path(String.valueOf(ResourceIds.raw.video))
            .build();

    Bitmap frame =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .load(uri.toString())
                .frame(TimeUnit.SECONDS.toMicros(1))
                .submit());

    assertThat(frame).isNotNull();
  }

  @Test
  public void loadVideoResourceUri_fromName_decodesFrame() {
    Resources resources = context.getResources();
    int resourceId = ResourceIds.raw.video;
    Uri uri =
        new Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(resourceId))
            .appendPath(resources.getResourceTypeName(resourceId))
            .appendPath(resources.getResourceEntryName(resourceId))
            .build();

    Drawable frame = concurrency.get(GlideApp.with(context).load(uri).submit());

    assertThat(frame).isNotNull();
  }

  @Test
  public void loadVideoResourceUri_asBitmap_fromName_decodesFrame() {
    Resources resources = context.getResources();
    int resourceId = ResourceIds.raw.video;
    Uri uri =
        new Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(resourceId))
            .appendPath(resources.getResourceTypeName(resourceId))
            .appendPath(resources.getResourceEntryName(resourceId))
            .build();

    Bitmap frame = concurrency.get(GlideApp.with(context).asBitmap().load(uri).submit());

    assertThat(frame).isNotNull();
  }

  @Test
  public void loadVideoResourceUri_fromName_withFrame_decodesFrame() {
    Resources resources = context.getResources();
    int resourceId = ResourceIds.raw.video;
    Uri uri =
        new Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(resourceId))
            .appendPath(resources.getResourceTypeName(resourceId))
            .appendPath(resources.getResourceEntryName(resourceId))
            .build();

    Bitmap frame =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .load(uri)
                .frame(TimeUnit.SECONDS.toMicros(1))
                .submit());

    assertThat(frame).isNotNull();
  }

  @Test
  public void loadVideoResourceUriString_fromName_decodesFrame() {
    Resources resources = context.getResources();
    int resourceId = ResourceIds.raw.video;
    Uri uri =
        new Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(resourceId))
            .appendPath(resources.getResourceTypeName(resourceId))
            .appendPath(resources.getResourceEntryName(resourceId))
            .build();

    Bitmap frame = concurrency.get(GlideApp.with(context).asBitmap().load(uri.toString()).submit());

    assertThat(frame).isNotNull();
  }

  @Test
  public void loadVideoResourceUriString_fromName_withFrame_decodesFrame() {
    Resources resources = context.getResources();
    int resourceId = ResourceIds.raw.video;
    Uri uri =
        new Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(resourceId))
            .appendPath(resources.getResourceTypeName(resourceId))
            .appendPath(resources.getResourceEntryName(resourceId))
            .build();

    Bitmap frame =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .load(uri.toString())
                .frame(TimeUnit.SECONDS.toMicros(1))
                .submit());

    assertThat(frame).isNotNull();
  }
}
