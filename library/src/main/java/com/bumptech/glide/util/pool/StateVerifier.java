package com.bumptech.glide.util.pool;

import androidx.annotation.NonNull;
import com.bumptech.glide.util.Synthetic;

/** Verifies that the job is not in the recycled state. */
public abstract class StateVerifier {
  private static final boolean DEBUG = false;

  /** Creates a new {@link StateVerifier} instance. */
  @NonNull
  public static StateVerifier newInstance() {
    if (DEBUG) {
      return new DebugStateVerifier();
    } else {
      return new DefaultStateVerifier();
    }
  }

  private StateVerifier() {}

  /**
   * Throws an exception if we believe our object is recycled and inactive (i.e. is currently in an
   * object pool).
   */
  public abstract void throwIfRecycled();

  /** Sets whether or not our object is recycled. */
  abstract void setRecycled(boolean isRecycled);

  private static class DefaultStateVerifier extends StateVerifier {
    private volatile boolean isReleased;

    @Synthetic
    DefaultStateVerifier() {}

    @Override
    public void throwIfRecycled() {
      if (isReleased) {
        throw new IllegalStateException("Already released");
      }
    }

    @Override
    public void setRecycled(boolean isRecycled) {
      this.isReleased = isRecycled;
    }
  }

  private static class DebugStateVerifier extends StateVerifier {
    // Keeps track of the stack trace where our state was set to recycled.
    private volatile RuntimeException recycledAtStackTraceException;

    @Synthetic
    DebugStateVerifier() {}

    @Override
    public void throwIfRecycled() {
      if (recycledAtStackTraceException != null) {
        throw new IllegalStateException("Already released", recycledAtStackTraceException);
      }
    }

    @Override
    void setRecycled(boolean isRecycled) {
      if (isRecycled) {
        recycledAtStackTraceException = new RuntimeException("Released");
      } else {
        recycledAtStackTraceException = null;
      }
    }
  }
}
