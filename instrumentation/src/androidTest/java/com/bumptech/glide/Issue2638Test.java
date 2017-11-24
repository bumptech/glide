package com.bumptech.glide;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.widget.AbsListView.LayoutParams;
import android.widget.ImageView;
import com.bumptech.glide.test.BitmapSubject;
import com.bumptech.glide.test.ConcurrencyHelper;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.test.TearDownGlide;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class Issue2638Test {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private Context context;

  @Before
  public void setUp() {
    context = InstrumentationRegistry.getTargetContext();
  }

  @Test
  public void intoImageView_withDifferentByteArrays_loadsDifferentImages()
      throws IOException, ExecutionException, InterruptedException {
    final ImageView imageView = new ImageView(context);
    imageView.setLayoutParams(new LayoutParams(/*w=*/ 100, /*h=*/ 100));

    final byte[] canonicalBytes = getCanonicalBytes();
    final byte[] modifiedBytes = getModifiedBytes();

    Glide.with(context)
        .load(canonicalBytes)
        .submit()
        .get();

    concurrency.loadOnMainThread(Glide.with(context).load(canonicalBytes), imageView);
    Bitmap firstBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

    concurrency.loadOnMainThread(Glide.with(context).load(modifiedBytes), imageView);
    Bitmap secondBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

    BitmapSubject.assertThat(firstBitmap).isNotSameAs(secondBitmap);
  }

  private byte[] getModifiedBytes() throws IOException {
    byte[] canonicalBytes = getCanonicalBytes();
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inMutable = true;
    Bitmap bitmap =
        BitmapFactory.decodeByteArray(canonicalBytes, 0, canonicalBytes.length, options);
    bitmap.setPixel(0, 0, Color.TRANSPARENT);
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    bitmap.compress(CompressFormat.PNG, /*quality=*/ 100, os);
    return os.toByteArray();
  }

  private byte[] getCanonicalBytes() throws IOException {
    int resourceId = ResourceIds.raw.canonical;
    Resources resources = context.getResources();
    InputStream is = resources.openRawResource(resourceId);
    return ByteStreams.toByteArray(is);
  }
}
