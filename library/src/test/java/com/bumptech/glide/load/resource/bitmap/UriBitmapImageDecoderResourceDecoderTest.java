package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(sdk = Build.VERSION_CODES.Q)
public final class UriBitmapImageDecoderResourceDecoderTest {

  private Context context;
  private UriBitmapImageDecoderResourceDecoder decoder;
  private Options options;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    decoder = new UriBitmapImageDecoderResourceDecoder(context);
    options = new Options();
  }

  @Test
  public void handles_returnsTrueForUri() throws IOException {
    ContentValues values = new ContentValues();
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
    Uri uri =
        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    assertThat(decoder.handles(uri, options)).isTrue();
  }

  @Test
  public void decode_withNonExistentUri_throwsIOException() {
    Uri uri = Uri.parse("file:///non-existent-file.png");
    assertThrows(
        IOException.class, () -> decoder.decode(uri, /* width= */ 100, /* height= */ 100, options));
  }

  @Test
  public void handles_returnsTrueForFileUri() throws IOException {
    Uri uri = Uri.parse("file:///path/to/image.png");
    assertThat(decoder.handles(uri, options)).isTrue();
  }

  @Test
  public void handles_returnsTrueForResourceUri() throws IOException {
    Uri uri = Uri.parse("android.resource://com.bumptech.glide.test/raw/image");
    assertThat(decoder.handles(uri, options)).isTrue();
  }

  @Test
  public void handles_returnsFalseForHttpUri() throws IOException {
    Uri uri = Uri.parse("http://example.com/image.png");
    assertThat(decoder.handles(uri, options)).isFalse();
  }

  @Test
  public void handles_returnsFalseForGifUri() throws IOException {
    ContentValues values = new ContentValues();
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/gif");
    Uri uri =
        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    assertThat(decoder.handles(uri, options)).isFalse();
  }

  @Test
  public void decode_solidColor_returnsExactColor() throws IOException {
    Bitmap bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bmp);
    canvas.drawColor(Color.RED);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    bmp.compress(Bitmap.CompressFormat.PNG, 0, out);

    ContentValues values = new ContentValues();
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
    Uri uri =
        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

    try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
      out.writeTo(os);
    }

    Resource<Bitmap> resource = decoder.decode(uri, /* width= */ 100, /* height= */ 100, options);
    Bitmap decoded = resource.get();

    assertThat(decoded.getPixel(0, 0)).isEqualTo(Color.RED);
  }
}
