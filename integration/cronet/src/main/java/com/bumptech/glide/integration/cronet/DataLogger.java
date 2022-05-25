package com.bumptech.glide.integration.cronet;

import androidx.annotation.Nullable;
import org.chromium.net.UrlResponseInfo;

/** A interface for logging data information related to loading the data. */
public interface DataLogger {

  /**
   * Logs the related network information.
   *
   * @param httpUrlRequest HttpUrlRequest that contains information on the request. May be {@code
   *     null} if the request was cancelled.
   * @param startTimeMs Timestamp (ms) that the request started.
   * @param responseStartTimeMs Timestamp (ms) when the first header byte was received.
   * @param endTimeMs Timestamp (ms) that the request ended.
   */
  void logNetworkData(
      @Nullable UrlResponseInfo httpUrlRequest,
      long startTimeMs,
      long responseStartTimeMs,
      long endTimeMs);
}
