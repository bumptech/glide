package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Base64;
import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.testutil.ConcurrencyHelper;
import com.bumptech.glide.testutil.TearDownGlide;
import com.bumptech.glide.util.Preconditions;
import java.io.ByteArrayOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DataUriTest {
  @Rule public TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private final Context context = ApplicationProvider.getApplicationContext();

  @Test
  public void load_withJpegAsDataUriString_returnsBitmap() {
    Bitmap bitmap =
        concurrency.get(
            Glide.with(context).asBitmap().load(getDataUriString(CompressFormat.JPEG)).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void load_withPngDataUriString_returnsBitmap() {
    Bitmap bitmap =
        concurrency.get(
            Glide.with(context).asBitmap().load(getDataUriString(CompressFormat.PNG)).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void load_withJpegAsDataUri_returnsBitmap() {
    Bitmap bitmap =
        concurrency.get(
            Glide.with(context).asBitmap().load(getDataUri(CompressFormat.JPEG)).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void load_withPngAsDataUri_returnsBitmap() {
    Bitmap bitmap =
        concurrency.get(
            Glide.with(context).asBitmap().load(getDataUri(CompressFormat.PNG)).submit());
    assertThat(bitmap).isNotNull();
  }

  private Uri getDataUri(CompressFormat format) {
    return Uri.parse(getDataUriString(format));
  }

  private String getDataUriString(CompressFormat format) {
    String bytes = getBase64BitmapBytes(format);
    String imageType;
    switch (format) {
      case PNG:
        imageType = "png";
        break;
      case JPEG:
        imageType = "jpeg";
        break;
      case WEBP:
        imageType = "webp";
        break;
      default:
        throw new IllegalArgumentException("Unrecognized format: " + format);
    }

    String mimeType = "image/" + imageType;
    return "data:" + mimeType + ";base64," + bytes;
  }

  @SuppressWarnings("deprecation")
  private String getBase64BitmapBytes(CompressFormat format) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    Drawable drawable =
        Preconditions.checkNotNull(ContextCompat.getDrawable(context, ResourceIds.raw.canonical));
    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
    bitmap.compress(format, 100, bos);
    byte[] data = bos.toByteArray();
    return Base64.encodeToString(data, /* flags= */ 0);
  }
}
