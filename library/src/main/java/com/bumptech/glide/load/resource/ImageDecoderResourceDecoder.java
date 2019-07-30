package com.bumptech.glide.load.resource;

import android.annotation.SuppressLint;
import android.graphics.ColorSpace;
import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.DecodeException;
import android.graphics.ImageDecoder.ImageInfo;
import android.graphics.ImageDecoder.OnHeaderDecodedListener;
import android.graphics.ImageDecoder.OnPartialImageListener;
import android.graphics.ImageDecoder.Source;
import android.os.Build;
import android.util.Log;
import android.util.Size;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.PreferredColorSpace;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.load.resource.bitmap.HardwareConfigState;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.Synthetic;
import java.io.IOException;

/**
 * Downsamples, decodes, and rotates images according to their exif orientation using {@link
 * ImageDecoder}.
 *
 * <p>Obeys all options in {@link Downsampler} except for {@link
 * Downsampler#FIX_BITMAP_SIZE_TO_REQUESTED_DIMENSIONS}.
 *
 * @param <T> The type of resource to be decoded (Bitmap, Drawable etc).
 */
@RequiresApi(api = 28)
public abstract class ImageDecoderResourceDecoder<T> implements ResourceDecoder<Source, T> {
  private static final String TAG = "ImageDecoder";

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  final HardwareConfigState hardwareConfigState = HardwareConfigState.getInstance();

  @Override
  public final boolean handles(@NonNull Source source, @NonNull Options options) {
    return true;
  }

  @Nullable
  @Override
  public final Resource<T> decode(
      @NonNull Source source,
      final int requestedWidth,
      final int requestedHeight,
      @NonNull Options options)
      throws IOException {
    final DecodeFormat decodeFormat = options.get(Downsampler.DECODE_FORMAT);
    final DownsampleStrategy strategy = options.get(DownsampleStrategy.OPTION);
    final boolean isHardwareConfigAllowed =
        options.get(Downsampler.ALLOW_HARDWARE_CONFIG) != null
            && options.get(Downsampler.ALLOW_HARDWARE_CONFIG);
    final PreferredColorSpace preferredColorSpace = options.get(Downsampler.PREFERRED_COLOR_SPACE);

    return decode(
        source,
        requestedWidth,
        requestedHeight,
        new OnHeaderDecodedListener() {
          @SuppressLint("Override")
          @Override
          public void onHeaderDecoded(ImageDecoder decoder, ImageInfo info, Source source) {
            if (hardwareConfigState.isHardwareConfigAllowed(
                requestedWidth,
                requestedHeight,
                isHardwareConfigAllowed,
                /*isExifOrientationRequired=*/ false)) {
              decoder.setAllocator(ImageDecoder.ALLOCATOR_HARDWARE);
            } else {
              decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            }

            if (decodeFormat == DecodeFormat.PREFER_RGB_565) {
              decoder.setMemorySizePolicy(ImageDecoder.MEMORY_POLICY_LOW_RAM);
            }

            decoder.setOnPartialImageListener(
                new OnPartialImageListener() {
                  @Override
                  public boolean onPartialImage(@NonNull DecodeException e) {
                    // Never return partial images.
                    return false;
                  }
                });

            Size size = info.getSize();
            int targetWidth = requestedWidth;
            if (requestedWidth == Target.SIZE_ORIGINAL) {
              targetWidth = size.getWidth();
            }
            int targetHeight = requestedHeight;
            if (requestedHeight == Target.SIZE_ORIGINAL) {
              targetHeight = size.getHeight();
            }

            float scaleFactor =
                strategy.getScaleFactor(
                    size.getWidth(), size.getHeight(), targetWidth, targetHeight);

            int resizeWidth = Math.round(scaleFactor * size.getWidth());
            int resizeHeight = Math.round(scaleFactor * size.getHeight());
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
              Log.v(
                  TAG,
                  "Resizing"
                      + " from ["
                      + size.getWidth()
                      + "x"
                      + size.getHeight()
                      + "]"
                      + " to ["
                      + resizeWidth
                      + "x"
                      + resizeHeight
                      + "]"
                      + " scaleFactor: "
                      + scaleFactor);
            }

            decoder.setTargetSize(resizeWidth, resizeHeight);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
              boolean isP3Eligible =
                  preferredColorSpace == PreferredColorSpace.DISPLAY_P3
                      && info.getColorSpace() != null
                      && info.getColorSpace().isWideGamut();
              decoder.setTargetColorSpace(
                  ColorSpace.get(
                      isP3Eligible ? ColorSpace.Named.DISPLAY_P3 : ColorSpace.Named.SRGB));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB));
            }
          }
        });
  }

  protected abstract Resource<T> decode(
      Source source, int requestedWidth, int requestedHeight, OnHeaderDecodedListener listener)
      throws IOException;
}
