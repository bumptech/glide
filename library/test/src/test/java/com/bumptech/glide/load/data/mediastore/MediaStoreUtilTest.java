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

    Uri androidPickerGetContentUri =
        Uri.parse(
            "content://media/picker_get_content/0/com.android.providers.media.photopicker/media/123");

    assertThat(MediaStoreUtil.isAndroidPickerUri(androidPickerGetContentUri)).isTrue();

    Uri androidPickerTranscodedUri =
        Uri.parse(
            "content://media/picker_transcoded/0/com.android.providers.media.photopicker/media/123");

    assertThat(MediaStoreUtil.isAndroidPickerUri(androidPickerTranscodedUri)).isTrue();

    Uri androidModifiedPickerUri =
        Uri.parse(
            "content://media/picker.component1-true.component2:"
                + " false}/0/com.android.providers.media.photopicker/media/123");

    assertThat(MediaStoreUtil.isAndroidPickerUri(androidModifiedPickerUri)).isTrue();

    Uri androidModifiedPickerGetContentUri =
        Uri.parse(
            "content://media/picker_get_content.component1-true.component2:"
                + " false}/0/com.android.providers.media.photopicker/media/123");

    assertThat(MediaStoreUtil.isAndroidPickerUri(androidModifiedPickerGetContentUri)).isTrue();

    Uri androidModifiedPickerTranscodedUri =
        Uri.parse(
            "content://media/picker_transcoded.component1-true.component2-xxxx"
                + " false}/0/com.android.providers.media.photopicker/media/123");

    assertThat(MediaStoreUtil.isAndroidPickerUri(androidModifiedPickerTranscodedUri)).isTrue();
  }
}
