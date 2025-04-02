package com.bumptech.glide.load.data.mediastore;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;
import android.provider.MediaStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class MediaStoreUtilTest {

  @Test
  public void isAndroidPickerUri_notAndroidPickerUri_returnsFalse() {
    Uri mediaStoreUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "123");

    assertThat(MediaStoreUtil.isAndroidPickerUri(mediaStoreUri)).isFalse();
  }

  @Test
  public void isAndroidPickerUri_identifiesAndroidPickerUri_returnsTrue() {
    Uri androidPickerUri =
        Uri.parse("content://media/picker/0/com.android.providers.media.photopicker/media/123");

    assertThat(MediaStoreUtil.isAndroidPickerUri(androidPickerUri)).isTrue();
  }
}
