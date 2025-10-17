package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeTrue;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.test.GlideApp;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.testutil.ConcurrencyHelper;
import com.bumptech.glide.testutil.TearDownGlide;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

/**
 * Tests that Glide is able to load animated images (WebP and AVIF) stored in resources and loaded
 * as {@link android.graphics.drawable.AnimatedImageDrawable}s when the underlying Android platform
 * supports it.
 */
@RunWith(AndroidJUnit4.class)
public class LoadAnimatedImageResourceTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();

  private Context context;

  private static final boolean IS_ANIMATED_WEBP_SUPPORTED =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
  private static final boolean IS_ANIMATED_AVIF_SUPPORTED =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    context = ApplicationProvider.getApplicationContext();
  }

  @Test
  public void loadAnimatedImageResourceId_fromInt_decodesAnimatedImageDrawable_Webp() {
    assumeTrue(IS_ANIMATED_WEBP_SUPPORTED);
    Drawable frame =
        concurrency.get(Glide.with(context).load(ResourceIds.raw.animated_webp).submit());

    assertThat(frame).isNotNull();
    assertThat(frame).isInstanceOf(AnimatedImageDrawable.class);
  }

  @Test
  public void loadAnimatedImageResourceId_fromInt_decodesAnimatedImageDrawable_Avif() {
    assumeTrue(IS_ANIMATED_AVIF_SUPPORTED);
    Drawable frame =
        concurrency.get(Glide.with(context).load(ResourceIds.raw.animated_avif).submit());

    assertThat(frame).isNotNull();
    assertThat(frame).isInstanceOf(AnimatedImageDrawable.class);
  }

  @Test
  public void loadAnimatedImageUri_fromId_decodesAnimatedImageDrawable_Webp() {
    assumeTrue(IS_ANIMATED_WEBP_SUPPORTED);
    Uri uri =
        new Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.getPackageName())
            .path(String.valueOf(ResourceIds.raw.animated_webp))
            .build();

    Drawable frame = concurrency.get(GlideApp.with(context).load(uri).submit());

    assertThat(frame).isNotNull();
    assertThat(frame).isInstanceOf(AnimatedImageDrawable.class);
  }

  @Test
  public void loadAnimatedImageUri_fromId_decodesAnimatedImageDrawable_Avif() {
    assumeTrue(IS_ANIMATED_AVIF_SUPPORTED);
    Uri uri =
        new Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.getPackageName())
            .path(String.valueOf(ResourceIds.raw.animated_avif))
            .build();

    Drawable frame = concurrency.get(GlideApp.with(context).load(uri).submit());

    assertThat(frame).isNotNull();
    assertThat(frame).isInstanceOf(AnimatedImageDrawable.class);
  }
}
