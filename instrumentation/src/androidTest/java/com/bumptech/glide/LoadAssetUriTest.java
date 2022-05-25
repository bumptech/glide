package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.test.GlideApp;
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
 * Tests that Glide is able to load images and videos stored in assets and loaded as {@link
 * android.content.res.AssetFileDescriptor}s.
 */
@RunWith(AndroidJUnit4.class)
public class LoadAssetUriTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private static final String VIDEO_ASSET_NAME = "video.mp4";
  private static final String IMAGE_ASSET_NAME = "canonical.jpg";

  private Context context;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    context = ApplicationProvider.getApplicationContext();
  }

  @Test
  public void loadVideoAssetUri_decodesFrame() {
    Uri uri = Uri.parse(assetNameToUri(VIDEO_ASSET_NAME));

    Drawable frame = concurrency.get(GlideApp.with(context).load(uri).submit());

    assertThat(frame).isNotNull();
  }

  @Test
  public void loadVideoAssetUri_asBitmap_decodesFrame() {
    Uri uri = Uri.parse(assetNameToUri(VIDEO_ASSET_NAME));

    Bitmap frame = concurrency.get(GlideApp.with(context).asBitmap().load(uri).submit());

    assertThat(frame).isNotNull();
  }

  @Test
  public void loadVideoAssetUri_withFrame_decodesFrame() {
    Uri uri = Uri.parse(assetNameToUri(VIDEO_ASSET_NAME));

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
  public void loadVideoAssetUriString_decodesFrame() {
    Uri uri = Uri.parse(assetNameToUri(VIDEO_ASSET_NAME));

    Bitmap frame = concurrency.get(GlideApp.with(context).asBitmap().load(uri.toString()).submit());

    assertThat(frame).isNotNull();
  }

  @Test
  public void loadVideoAssetUriString_withFrame_decodesFrame() {
    Uri uri = Uri.parse(assetNameToUri(VIDEO_ASSET_NAME));

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
  public void loadImageAssetUri_decodesImage() {
    Uri uri = Uri.parse(assetNameToUri(IMAGE_ASSET_NAME));

    Drawable image = concurrency.get(GlideApp.with(context).load(uri).submit());

    assertThat(image).isNotNull();
  }

  @Test
  public void loadImageAssetUri_asBitmap_decodesImage() {
    Uri uri = Uri.parse(assetNameToUri(IMAGE_ASSET_NAME));

    Bitmap image = concurrency.get(GlideApp.with(context).asBitmap().load(uri).submit());

    assertThat(image).isNotNull();
  }

  @Test
  public void loadImageAssetUriString_decodesImage() {
    Uri uri = Uri.parse(assetNameToUri(IMAGE_ASSET_NAME));

    Bitmap image = concurrency.get(GlideApp.with(context).asBitmap().load(uri.toString()).submit());

    assertThat(image).isNotNull();
  }

  private static String assetNameToUri(String assetName) {
    return "file:///android_asset/" + assetName;
  }
}
