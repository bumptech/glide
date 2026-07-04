package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.tests.TearDownGlide;
import com.bumptech.glide.util.GlideSuppliers.GlideSupplier;
import com.google.common.collect.ImmutableList;
import java.io.OutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(sdk = Build.VERSION_CODES.Q)
public class RegistryFactoryTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final Context context = ApplicationProvider.getApplicationContext();

  private static final class TestException extends RuntimeException {
    private static final long serialVersionUID = 2334956185897161236L;
  }

  @Test
  public void create_whenCalledTwiceWithThrowingModule_throwsOriginalException() {
    AppGlideModule throwingAppGlideModule =
        new AppGlideModule() {
          @Override
          public void registerComponents(
              @NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
            throw new TestException();
          }
        };

    Glide glide = Glide.get(context);
    GlideSupplier<Registry> registrySupplier =
        RegistryFactory.lazilyCreateAndInitializeRegistry(
            glide, /* manifestModules= */ ImmutableList.of(), throwingAppGlideModule);

    assertThrows(
        TestException.class,
        new ThrowingRunnable() {
          @Override
          public void run() {
            registrySupplier.get();
          }
        });

    assertThrows(
        TestException.class,
        new ThrowingRunnable() {
          @Override
          public void run() {
            registrySupplier.get();
          }
        });
  }

  @Test
  public void lazilyCreate_whenImageDecoderEnabled_localGifLoadsAsAnimated() throws Exception {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      return;
    }
    GlideBuilder builder = new GlideBuilder();
    builder.setImageDecoderEnabledForBitmaps(true);
    builder.setUriImageDecoderEnabled(true);
    Glide.init(context, builder);

    ContentValues values = new ContentValues();
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/gif");
    Uri gifUri =
        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    try (OutputStream os = context.getContentResolver().openOutputStream(gifUri)) {
      os.write(TINY_GIF);
    }

    // Load the GIF as a Drawable on a background thread.
    Future<Drawable> future =
        Executors.newSingleThreadExecutor()
            .submit(() -> Glide.with(context).asDrawable().load(gifUri).submit().get());

    Drawable drawable = future.get(5, TimeUnit.SECONDS);

    assertThat(drawable).isInstanceOf(GifDrawable.class);
  }

  @Test
  public void testImageDecoderDecodesTinyGif() throws Exception {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      return;
    }
    ContentValues values = new ContentValues();
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/gif");
    Uri gifUri =
        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    try (OutputStream os = context.getContentResolver().openOutputStream(gifUri)) {
      os.write(TINY_GIF);
    }

    android.graphics.ImageDecoder.Source source =
        android.graphics.ImageDecoder.createSource(context.getContentResolver(), gifUri);
    try {
      Bitmap bitmap = android.graphics.ImageDecoder.decodeBitmap(source);
      System.out.println("ImageDecoder succeeded: " + bitmap);
    } catch (Throwable t) {
      System.out.println("ImageDecoder failed!");
      t.printStackTrace();
      throw new RuntimeException(t);
    }
  }

  private static final byte[] TINY_GIF =
      new byte[] {
        0x47,
        0x49,
        0x46,
        0x38,
        0x39,
        0x61,
        0x01,
        0x00,
        0x01,
        0x00,
        (byte) 0x80,
        0x00,
        0x00,
        (byte) 0xff,
        (byte) 0xff,
        (byte) 0xff,
        0x00,
        0x00,
        0x00,
        0x21,
        (byte) 0xf9,
        0x04,
        0x01,
        0x00,
        0x00,
        0x00,
        0x00,
        0x2c,
        0x00,
        0x00,
        0x00,
        0x00,
        0x01,
        0x00,
        0x01,
        0x00,
        0x00,
        0x02,
        0x02,
        0x44,
        0x01,
        0x00,
        0x3b
      };
}
