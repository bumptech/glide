package com.bumptech.glide.load.engine;

/**
 * An exception indicating that code outside of Glide threw an unexpected exception.
 *
 * <p>This is useful to allow us to distinguish developer errors on the part of users of Glide from
 * developer errors on the part of developers of Glide itself.
 */
final class CallbackException extends RuntimeException {
  private static final long serialVersionUID = -7530898992688511851L;

  CallbackException(Throwable cause) {
    super("Unexpected exception thrown by non-Glide code", cause);
  }
}
