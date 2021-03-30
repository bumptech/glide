package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeTrue;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.signature.ObjectKey;
import com.bumptech.glide.test.GlideApp;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.testutil.ConcurrencyHelper;
import com.bumptech.glide.testutil.TearDownGlide;
import com.bumptech.glide.util.Util;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * On API 26, decoding a variety of different images can cause {@link BitmapFactory} with {@link
 * BitmapFactory.Options#inJustDecodeBounds} set to {@code true} to set {@link
 * BitmapFactory.Options#outConfig} to null instead of a valid value, even though the image can be
 * decoded successfully. Glide can mask these failures by decoding some image sources (notably
 * including resource ids) using other data types and decoders.
 *
 * <p>This test ensures that we've worked around the framework issue by loading a variety of images
 * and image types without the normal fallback behavior.
 */
@RunWith(AndroidJUnit4.class)
public class LoadResourcesWithDownsamplerTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private final Context context = ApplicationProvider.getApplicationContext();

  @Test
  public void loadJpegResource_withNoOtherLoaders_decodesResource() {
    Glide.get(context)
        .getRegistry()
        .prepend(Object.class, InputStream.class, new FakeModelLoader<>(ResourceIds.raw.canonical));

    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(new Object()).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void loadWideGamutJpegResource_withNoOtherLoaders_decodesWideGamutBitmap() {
    assumeTrue(
        "Wide gamut is only available on O+", Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
    Glide.get(context)
        .getRegistry()
        .prepend(
            Object.class, InputStream.class, new FakeModelLoader<>(ResourceIds.raw.webkit_logo_p3));

    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(new Object()).submit());
    assertThat(bitmap).isNotNull();
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.RGBA_F16);
    assertThat(bitmap.getColorSpace())
        .isEqualTo(ColorSpace.get(ColorSpace.Named.LINEAR_EXTENDED_SRGB));
  }

  @Test
  public void loadOpaquePngResource_withNoOtherLoaders_decodesResource() {
    Glide.get(context)
        .getRegistry()
        .prepend(
            Object.class, InputStream.class, new FakeModelLoader<>(ResourceIds.raw.canonical_png));

    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(new Object()).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void loadTransparentPngResource_withNoOtherLoaders_decodesResource() {
    Glide.get(context)
        .getRegistry()
        .prepend(
            Object.class,
            InputStream.class,
            new FakeModelLoader<>(ResourceIds.raw.canonical_transparent_png));

    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(new Object()).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void loadTransparentGifResource_withNoOtherLoaders_decodesResource() {
    Glide.get(context)
        .getRegistry()
        .prepend(
            Object.class,
            InputStream.class,
            new FakeModelLoader<>(ResourceIds.raw.transparent_gif));

    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(new Object()).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void loadTransparentGifResource_asHardware_withNoOtherLoaders_decodesResource()
      throws InterruptedException {
    assumeTrue(
        "Hardware Bitmaps are only supported on O+",
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
    // enableHardwareBitmaps must be called on the main thread.
    final CountDownLatch latch = new CountDownLatch(1);
    Util.postOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            Glide.enableHardwareBitmaps();
            latch.countDown();
          }
        });
    latch.await(5, TimeUnit.SECONDS);

    Glide.get(context)
        .getRegistry()
        .prepend(
            Object.class,
            InputStream.class,
            new FakeModelLoader<>(ResourceIds.raw.transparent_gif));

    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .set(Downsampler.ALLOW_HARDWARE_CONFIG, true)
                .format(DecodeFormat.PREFER_ARGB_8888)
                .load(new Object())
                .submit());
    assertThat(bitmap).isNotNull();
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.HARDWARE);
  }

  @Test
  public void loadTransparentGifResource_withNoOtherLoaders_fromBytes_decodesResource() {
    byte[] data = getBytes(ResourceIds.raw.transparent_gif);
    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(data).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void loadOpaqueGifResource_withNoOtherLoaders_decodesResource() {
    Glide.get(context)
        .getRegistry()
        .prepend(
            Object.class, InputStream.class, new FakeModelLoader<>(ResourceIds.raw.opaque_gif));

    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(new Object()).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void loadOpaqueGifResource_asBytes_decodesResource() {
    byte[] data = getBytes(ResourceIds.raw.opaque_gif);
    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(data).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void loadOpaqueGifResource_asHardware_withNoOtherLoaders_decodesResource() {
    assumeTrue(
        "Hardware Bitmaps are only supported on O+",
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);

    Glide.get(context)
        .getRegistry()
        .prepend(
            Object.class, InputStream.class, new FakeModelLoader<>(ResourceIds.raw.opaque_gif));

    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                // Allow HARDWARE Bitmaps.
                .format(DecodeFormat.PREFER_ARGB_8888)
                .load(new Object())
                .submit());
    assertThat(bitmap).isNotNull();
  }

  private byte[] getBytes(int resourceId) {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    InputStream is = null;
    try {
      is = context.getResources().openRawResource(resourceId);
      byte[] buffer = new byte[1024 * 1024];
      int read;
      while ((read = is.read(buffer)) != -1) {
        os.write(buffer, 0, read);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          // Ignored;
        }
      }
    }

    return os.toByteArray();
  }

  private class FakeModelLoader<T>
      implements ModelLoader<T, InputStream>, ModelLoaderFactory<T, InputStream> {

    private final int resourceId;

    FakeModelLoader(int resourceId) {
      this.resourceId = resourceId;
    }

    @androidx.annotation.Nullable
    @Override
    public LoadData<InputStream> buildLoadData(
        @NonNull Object o, int width, int height, @NonNull Options options) {
      return new LoadData<>(new ObjectKey(o), new Fetcher());
    }

    @Override
    public boolean handles(@NonNull Object o) {
      return true;
    }

    @NonNull
    @Override
    public ModelLoader<T, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
      return this;
    }

    @Override
    public void teardown() {}

    private final class Fetcher implements DataFetcher<InputStream> {
      private InputStream inputStream;

      @Override
      public void loadData(
          @NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
        inputStream = getInputStreamForResource(context, resourceId);
        callback.onDataReady(inputStream);
      }

      private InputStream getInputStreamForResource(Context context, @DrawableRes int resourceId) {
        Resources resources = context.getResources();
        try {
          Uri parse =
              Uri.parse(
                  String.format(
                      Locale.US,
                      "%s://%s/%s/%s",
                      ContentResolver.SCHEME_ANDROID_RESOURCE,
                      resources.getResourcePackageName(resourceId),
                      resources.getResourceTypeName(resourceId),
                      resources.getResourceEntryName(resourceId)));
          return context.getContentResolver().openInputStream(parse);
        } catch (Resources.NotFoundException | FileNotFoundException e) {
          throw new IllegalArgumentException("Resource ID " + resourceId + " not found", e);
        }
      }

      @Override
      public void cleanup() {
        InputStream local = inputStream;
        if (local != null) {
          try {
            local.close();
          } catch (IOException e) {
            // Ignored.
          }
        }
      }

      @Override
      public void cancel() {
        // Do nothing.
      }

      @NonNull
      @Override
      public Class<InputStream> getDataClass() {
        return InputStream.class;
      }

      @NonNull
      @Override
      public DataSource getDataSource() {
        return DataSource.LOCAL;
      }
    }
  }
}
