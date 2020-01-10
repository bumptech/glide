package com.bumptech.glide.integration.cronet;

import com.google.common.base.Supplier;
import java.util.Map;
import java.util.concurrent.Executor;
import org.chromium.net.CronetEngine;
import org.chromium.net.UrlRequest;

/** Default implementation for building cronet requests. */
public final class CronetRequestFactoryImpl implements CronetRequestFactory {

  private final Supplier<CronetEngine> cronetEngineGetter;

  public CronetRequestFactoryImpl(Supplier<CronetEngine> cronetEngineGetter) {
    this.cronetEngineGetter = cronetEngineGetter;
  }

  @Override
  public UrlRequest.Builder newRequest(
      String url, int requestPriority, Map<String, String> headers, UrlRequest.Callback listener) {
    CronetEngine engine = cronetEngineGetter.get();
    UrlRequest.Builder builder =
        engine.newUrlRequestBuilder(
            url,
            listener,
            new Executor() {
              @Override
              public void execute(Runnable runnable) {
                runnable.run();
              }
            });
    builder.allowDirectExecutor();
    builder.setPriority(requestPriority);
    for (Map.Entry<String, String> header : headers.entrySet()) {
      // Cronet owns the Accept-Encoding header and user agent
      String key = header.getKey();
      if ("Accept-Encoding".equalsIgnoreCase(key) || "User-Agent".equalsIgnoreCase(key)) {
        continue;
      }
      builder.addHeader(key, header.getValue());
    }
    return builder;
  }
}
