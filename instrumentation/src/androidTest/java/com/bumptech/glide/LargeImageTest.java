package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.model.UnitModelLoader;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.testutil.ConcurrencyHelper;
import com.bumptech.glide.testutil.TearDownGlide;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LargeImageTest {
  @Rule public final TestRule tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private final Context context = ApplicationProvider.getApplicationContext();

  @Test
  public void loadLargeJpeg_asByteArray_succeeds() throws IOException {
    byte[] data = getLargeImageBytes();
    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(data).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void loadLargeJpeg_asByteBuffer_succeeds() throws IOException {
    // Using UnitModelLoader lets us mimic loading the ByteBuffer from some other data type, which
    // reduces the scope of our test.
    Glide.get(context)
        .getRegistry()
        .append(
            ByteBuffer.class, ByteBuffer.class, UnitModelLoader.Factory.<ByteBuffer>getInstance());

    ByteBuffer buffer = ByteBuffer.wrap(getLargeImageBytes());
    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(buffer).submit());
    assertThat(bitmap).isNotNull();
  }

  private byte[] getLargeImageBytes() throws IOException {
    Resources resources = context.getResources();
    int resourceId = ResourceIds.raw.canonical_large;
    Uri uri =
        new Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(resourceId))
            .appendPath(resources.getResourceTypeName(resourceId))
            .appendPath(resources.getResourceEntryName(resourceId))
            .build();

    InputStream is = Objects.requireNonNull(context.getContentResolver().openInputStream(uri));
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024 * 1024 * 5];
    int read;
    while ((read = is.read(buffer, 0, buffer.length)) != -1) {
      os.write(buffer, 0, read);
    }
    return os.toByteArray();
  }
}
