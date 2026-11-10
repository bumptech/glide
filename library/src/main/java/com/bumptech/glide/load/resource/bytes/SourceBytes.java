package com.bumptech.glide.load.resource.bytes;

import androidx.annotation.NonNull;
import com.bumptech.glide.util.Preconditions;

/**
 * The encoded bytes of an image exactly as they were loaded, for example from the network, a file
 * or the disk cache.
 *
 * <p>Load them with {@code Glide.with(context).as(SourceBytes.class).load(model)}. Unlike {@code
 * as(byte[].class)}, which decodes the image first and, for most formats, compresses the decoded
 * {@link android.graphics.Bitmap} again, the bytes are neither decoded nor re-encoded. They keep
 * the original format, quality and metadata. Requested sizes, downsampling and transformations have
 * no effect.
 */
public final class SourceBytes {
  private final byte[] bytes;

  public SourceBytes(@NonNull byte[] bytes) {
    this.bytes = Preconditions.checkNotNull(bytes);
  }

  /**
   * Returns the encoded bytes.
   *
   * <p>This is the same array every time, not a copy, so it must not be modified.
   */
  @NonNull
  @SuppressWarnings("PMD.MethodReturnsInternalArray")
  public byte[] getBytes() {
    return bytes;
  }
}
