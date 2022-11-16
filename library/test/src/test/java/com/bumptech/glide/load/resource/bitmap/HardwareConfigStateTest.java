package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBuild;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class HardwareConfigStateTest {

  @Config(sdk = Build.VERSION_CODES.O)
  @Test
  public void
      setHardwareConfigIfAllowed_withAllowedState_setsInPreferredConfigAndMutable_returnsTrue() {
    HardwareConfigState state = new HardwareConfigState();
    state.unblockHardwareBitmaps();
    BitmapFactory.Options options = new BitmapFactory.Options();
    boolean result =
        state.setHardwareConfigIfAllowed(
            /* targetWidth= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
            /* targetHeight= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
            options,
            /* isHardwareConfigAllowed= */ true,
            /* isExifOrientationRequired= */ false);

    assertThat(result).isTrue();
    assertThat(options.inPreferredConfig).isEqualTo(Bitmap.Config.HARDWARE);
  }

  @Config(sdk = Build.VERSION_CODES.O)
  @Test
  public void
      setHardwareConfigIfAllowed_withAllowedState_afterReblock_returnsFalseAndDoesNotSetValues() {
    HardwareConfigState state = new HardwareConfigState();
    state.unblockHardwareBitmaps();
    state.blockHardwareBitmaps();
    BitmapFactory.Options options = new BitmapFactory.Options();
    boolean result =
        state.setHardwareConfigIfAllowed(
            /* targetWidth= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
            /* targetHeight= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
            options,
            /* isHardwareConfigAllowed= */ true,
            /* isExifOrientationRequired= */ false);

    assertThat(result).isFalse();
    assertThat(options.inPreferredConfig).isNotEqualTo(Bitmap.Config.HARDWARE);
  }

  @Config(sdk = Build.VERSION_CODES.O)
  @Test
  public void setHardwareConfigIfAllowed_withSmallerThanMinWidth_returnsFalse_doesNotSetValues() {
    HardwareConfigState state = new HardwareConfigState();
    state.unblockHardwareBitmaps();
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferredConfig = null;
    options.inMutable = true;

    boolean result =
        state.setHardwareConfigIfAllowed(
            /* targetWidth= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O - 1,
            /* targetHeight= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
            options,
            /* isHardwareConfigAllowed= */ true,
            /* isExifOrientationRequired= */ false);

    assertThat(result).isFalse();
    assertThat(options.inMutable).isTrue();
    assertThat(options.inPreferredConfig).isNull();
  }

  @Config(sdk = Build.VERSION_CODES.O)
  @Test
  public void setHardwareConfigIfAllowed_withSmallerThanMinHeight_returnsFalse_doesNotSetValues() {
    HardwareConfigState state = new HardwareConfigState();
    state.unblockHardwareBitmaps();
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferredConfig = null;
    options.inMutable = true;

    boolean result =
        state.setHardwareConfigIfAllowed(
            /* targetWidth= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
            /* targetHeight= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O - 1,
            options,
            /* isHardwareConfigAllowed= */ true,
            /* isExifOrientationRequired= */ false);

    assertThat(result).isFalse();
    assertThat(options.inMutable).isTrue();
    assertThat(options.inPreferredConfig).isNull();
  }

  @Config(sdk = Build.VERSION_CODES.O)
  @Test
  public void
      setHardwareConfigIfAllowed_withHardwareConfigDisallowed_returnsFalse_doesNotSetValues() {
    HardwareConfigState state = new HardwareConfigState();
    state.unblockHardwareBitmaps();
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferredConfig = null;
    options.inMutable = true;

    boolean result =
        state.setHardwareConfigIfAllowed(
            /* targetWidth= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
            /* targetHeight= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
            options,
            /* isHardwareConfigAllowed= */ false,
            /* isExifOrientationRequired= */ false);

    assertThat(result).isFalse();
    assertThat(options.inMutable).isTrue();
    assertThat(options.inPreferredConfig).isNull();
  }

  @Config(sdk = Build.VERSION_CODES.O)
  @Test
  public void
      setHardwareConfigIfAllowed_withExifOrientationRequired_returnsFalse_doesNotSetValues() {
    HardwareConfigState state = new HardwareConfigState();
    state.unblockHardwareBitmaps();
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferredConfig = null;
    options.inMutable = true;

    boolean result =
        state.setHardwareConfigIfAllowed(
            /* targetWidth= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
            /* targetHeight= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
            options,
            /* isHardwareConfigAllowed= */ true,
            /* isExifOrientationRequired= */ true);

    assertThat(result).isFalse();
    assertThat(options.inMutable).isTrue();
    assertThat(options.inPreferredConfig).isNull();
  }

  @Config(sdk = Build.VERSION_CODES.N_MR1)
  @Test
  public void setHardwareConfigIfAllowed_withOsLessThanO_returnsFalse_doesNotSetValues() {
    HardwareConfigState state = new HardwareConfigState();
    state.unblockHardwareBitmaps();
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferredConfig = null;
    options.inMutable = true;

    boolean result =
        state.setHardwareConfigIfAllowed(
            /* targetWidth= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
            /* targetHeight= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
            options,
            /* isHardwareConfigAllowed= */ true,
            /* isExifOrientationRequired= */ false);

    assertThat(result).isFalse();
    assertThat(options.inMutable).isTrue();
    assertThat(options.inPreferredConfig).isNull();
  }

  @Config(sdk = Build.VERSION_CODES.P)
  @Test
  public void
      setHardwareConfigIfAllowed_withOsLessThanQ_beforeUnblockingHardwareBitmaps_returnsFalseAndDoesNotSetValues() {
    HardwareConfigState state = new HardwareConfigState();
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferredConfig = null;
    options.inMutable = true;

    boolean result =
        state.setHardwareConfigIfAllowed(
            /* targetWidth= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
            /* targetHeight= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
            options,
            /* isHardwareConfigAllowed= */ true,
            /* isExifOrientationRequired= */ false);

    assertThat(result).isFalse();
    assertThat(options.inMutable).isTrue();
    assertThat(options.inPreferredConfig).isNull();
  }

  @Config(sdk = Build.VERSION_CODES.O)
  @Test
  public void
      setHardwareConfigIfAllowed_withDisallowedSamsungDevices_returnsFalse_doesNotSetValues() {
    for (String model :
        new String[] {
          "SM-N9351", "SM-J72053", "SM-G9600", "SM-G965ab", "SM-G935.", "SM-G930", "SM-A5204"
        }) {
      ShadowBuild.setModel(model);
      HardwareConfigState state = new HardwareConfigState();
      state.unblockHardwareBitmaps();
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inPreferredConfig = null;
      options.inMutable = true;

      boolean result =
          state.setHardwareConfigIfAllowed(
              /* targetWidth= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
              /* targetHeight= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
              options,
              /* isHardwareConfigAllowed= */ true,
              /* isExifOrientationRequired= */ false);

      assertWithMessage("model: " + model).that(result).isFalse();
      assertWithMessage("model: " + model).that(options.inMutable).isTrue();
      assertWithMessage("model: " + model).that(options.inPreferredConfig).isNull();
    }
  }

  @Config(sdk = Build.VERSION_CODES.O_MR1)
  @Test
  public void setHardwareConfigIfAllowed_withDisallowedSamsungDevices_OMR1_returnsTrue() {
    for (String model :
        new String[] {
          "SM-N9351", "SM-J72053", "SM-G9600", "SM-G965ab", "SM-G935.", "SM-G930", "SM-A5204"
        }) {
      ShadowBuild.setModel(model);
      HardwareConfigState state = new HardwareConfigState();
      state.unblockHardwareBitmaps();
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inPreferredConfig = null;
      options.inMutable = true;

      boolean result =
          state.setHardwareConfigIfAllowed(
              /* targetWidth= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
              /* targetHeight= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
              options,
              /* isHardwareConfigAllowed= */ true,
              /* isExifOrientationRequired= */ false);

      assertWithMessage("model: " + model).that(result).isTrue();
      assertWithMessage("model: " + model).that(options.inMutable).isFalse();
      assertWithMessage("model: " + model)
          .that(options.inPreferredConfig)
          .isEqualTo(Bitmap.Config.HARDWARE);
    }
  }

  @Config(sdk = Build.VERSION_CODES.O)
  @Test
  public void setHardwareConfigIfAllowed_withShortOrEmptyModelNames_returnsTrue() {
    for (String model : new String[] {".", "-", "", "S", "SM", "SM-", "SM-G", "SM-G9.", "SM-G93"}) {
      ShadowBuild.setModel(model);
      HardwareConfigState state = new HardwareConfigState();
      state.unblockHardwareBitmaps();
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inPreferredConfig = null;
      options.inMutable = true;

      boolean result =
          state.setHardwareConfigIfAllowed(
              /* targetWidth= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
              /* targetHeight= */ HardwareConfigState.MIN_HARDWARE_DIMENSION_O,
              options,
              /* isHardwareConfigAllowed= */ true,
              /* isExifOrientationRequired= */ false);

      assertWithMessage("model: " + model).that(result).isTrue();
      assertWithMessage("model: " + model).that(options.inMutable).isFalse();
      assertWithMessage("model: " + model)
          .that(options.inPreferredConfig)
          .isEqualTo(Bitmap.Config.HARDWARE);
    }
  }
}
