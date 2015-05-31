package com.bumptech.glide.load.engine;

/**
 * Verifies that the job is not in the recycled state.
 */
interface StateVerifier {
  final class Factory {
    private static final boolean DEBUG = false;

    private Factory() { }

    static StateVerifier build() {
      if (DEBUG) {
        return new DebugStateVerifier();
      } else {
        return new DefaultStateVerifier();
      }
    }
  }

  void throwIfRecycled();

  void setRecycled(boolean isRecycled);

  class DefaultStateVerifier implements StateVerifier {

    private volatile boolean isReleased;

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

  class DebugStateVerifier implements StateVerifier {

    // Keeps track of the stack trace where our state was set to recycled.
    private volatile RuntimeException recycledAtStackTraceException;

    @Override
    public void throwIfRecycled() {
      if (recycledAtStackTraceException != null) {
        throw new IllegalStateException("Already released", recycledAtStackTraceException);
      }
    }

    @Override
    public void setRecycled(boolean isRecycled) {
      if (isRecycled) {
        this.recycledAtStackTraceException = new RuntimeException("Released");
      } else {
        this.recycledAtStackTraceException = null;
      }
    }
  }
}
