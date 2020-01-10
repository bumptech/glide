package com.bumptech.glide.integration.cronet;

import java.util.Map;
import org.chromium.net.UrlRequest;

/** Factory to build custom cronet requests. */
public interface CronetRequestFactory {

  UrlRequest.Builder newRequest(
      String url, int requestPriority, Map<String, String> headers, UrlRequest.Callback listener);
}
