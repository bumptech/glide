package com.bumptech.glide.load.data.mediastore;

import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;
import android.provider.MediaStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class MediaStoreUtilTest {

  @Test
  public void isAndroidPickerUri_identifiesAndroidPickerUris() {
    Uri mediaStoreUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "123");
    Uri androidPickerUri =
        Uri.parse("content://media/picker/0/com.android.providers.media.photopicker/media/123");

    assertThat(MediaStoreUtil.isAndroidPickerUri(mediaStoreUri)).isFalse();
    assertThat(MediaStoreUtil.isAndroidPickerUri(androidPickerUri)).isTrue();
   }
}
