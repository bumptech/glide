package com.bumptech.glide.load.resource.transcode;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.bumptech.glide.tests.Util.mockResource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.NonOwnedBitmapResource;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class DrawableBytesTranscoderTest {
  private DrawableBytesTranscoder transcoder;
  private ResourceTranscoder<Bitmap, byte[]> bitmapBytesTranscoder;
  private Resource<Drawable> drawableResource;
  private BitmapDrawable bitmapDrawable;
  private Bitmap bitmap;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    BitmapPool bitmapPool = mock(BitmapPool.class);
    bitmapBytesTranscoder = mock(ResourceTranscoder.class);
    ResourceTranscoder<GifDrawable, byte[]> gifDrawableBytesTranscoder =
        mock(ResourceTranscoder.class);
    transcoder =
        new DrawableBytesTranscoder(bitmapPool, bitmapBytesTranscoder, gifDrawableBytesTranscoder);

    drawableResource = mockResource();
    bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    bitmapDrawable =
        new BitmapDrawable(ApplicationProvider.getApplicationContext().getResources(), bitmap);
    when(drawableResource.get()).thenReturn(bitmapDrawable);
  }

  @Test
  public void testTranscode_withBitmapDrawable_recyclesInputResource() {
    transcoder.transcode(drawableResource, new Options());

    verify(drawableResource).recycle();
  }

  @Test
  public void testTranscode_withBitmapDrawable_usesNonOwnedBitmapResource() {
    transcoder.transcode(drawableResource, new Options());

    verify(bitmapBytesTranscoder).transcode(any(NonOwnedBitmapResource.class), any(Options.class));
  }
}
