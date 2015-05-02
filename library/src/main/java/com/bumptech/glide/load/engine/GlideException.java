package com.bumptech.glide.load.engine;

/**
 * Parent class thrown whenever a Glide load fails due to a recoverable error.
 */
public class GlideException extends Exception {

  private final GlideException previous;

  public GlideException(String detailMessage) {
    this(detailMessage, null);
  }

  public GlideException(String detailMessage, GlideException previous) {
    super(detailMessage);
    this.previous = previous;
  }

  public GlideException(String detailMessage, Throwable cause) {
    this(detailMessage, cause, null);
  }

  public GlideException(String detailMessage, Throwable cause, GlideException previous) {
    super(detailMessage, cause);
    this.previous = previous;
  }

  public GlideException getPrevious() {
    return previous;
  }
}
