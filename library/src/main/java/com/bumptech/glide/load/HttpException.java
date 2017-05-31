package com.bumptech.glide.load;

import android.support.annotation.Nullable;
import java.io.IOException;

/**
 * Thrown when an http request fails.
 *
 * <p>Exposes the specific status code or {@link #UNKNOWN} via {@link #getStatusCode()} so
 * users may attempt to retry or otherwise uniformly handle certain types of errors regardless of
 * the underlying http library.
 */
public final class HttpException extends IOException {
  public static final int UNKNOWN = -1;
  private final int statusCode;

  public HttpException(int statusCode) {
    this("Http request failed with status code: " + statusCode, statusCode);
  }

  public HttpException(String message) {
    this(message, UNKNOWN);
  }

  public HttpException(String message, int statusCode) {
    this(message, statusCode, null /*cause*/);
  }

  public HttpException(String message, int statusCode, @Nullable Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
  }

  /**
   * Returns the http status code, or {@link #UNKNOWN} if the request failed without providing
   * a status code.
   */
  public int getStatusCode() {
    return statusCode;
  }
}
