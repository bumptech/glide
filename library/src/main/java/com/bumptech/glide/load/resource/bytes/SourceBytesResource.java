package com.bumptech.glide.load.resource.bytes;

import androidx.annotation.NonNull;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.Preconditions;

/** A {@link Resource} wrapping {@link SourceBytes}. */
final class SourceBytesResource implements Resource<SourceBytes> {
  private final SourceBytes sourceBytes;

  SourceBytesResource(@NonNull SourceBytes sourceBytes) {
    this.sourceBytes = Preconditions.checkNotNull(sourceBytes);
  }

  @NonNull
  @Override
  public Class<SourceBytes> getResourceClass() {
    return SourceBytes.class;
  }

  @NonNull
  @Override
  public SourceBytes get() {
    return sourceBytes;
  }

  @Override
  public int getSize() {
    return sourceBytes.getBytes().length;
  }

  @Override
  public void recycle() {
    // Do nothing.
  }
}
