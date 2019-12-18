package com.bumptech.glide.integration.cronet;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.chromium.net.CronetEngine.Builder;
import org.chromium.net.ExperimentalCronetEngine;
import org.chromium.net.RequestFinishedInfo;
import org.chromium.net.RequestFinishedInfo.Listener;
import org.chromium.net.UrlResponseInfo;

/** A simple model loader for fetching media over http/https using Cronet. */
public class CronetUrlLoader implements ModelLoader<GlideUrl, InputStream> {

  private static final String TAG = "CronetUrlLoader";
  private final ExperimentalCronetEngine client;

  // Public API.
  @SuppressWarnings("WeakerAccess")
  public CronetUrlLoader(@NonNull ExperimentalCronetEngine client) {
    this.client = client;
  }

  @Override
  public boolean handles(@NonNull GlideUrl url) {
    return true;
  }

  @Override
  public LoadData<InputStream> buildLoadData(
      @NonNull GlideUrl model, int width, int height, @NonNull Options options) {
    return new LoadData<>(model, new CronetStreamFetcher(client, model));
  }

  /** The default factory for {@link CronetUrlLoader}s. */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {
    private static volatile ExperimentalCronetEngine internalClient;

    private final ExperimentalCronetEngine client;
    private final java.util.concurrent.Executor executor;

    private static ExperimentalCronetEngine getInternalClient(Context context) {
      if (internalClient == null) {
        synchronized (Factory.class) {
          if (internalClient == null) {
            internalClient = initCronetEngine(context);
          }
        }
      }
      return internalClient;
    }

    private static ExperimentalCronetEngine initCronetEngine(Context context) {
      ExperimentalCronetEngine.Builder builder = new ExperimentalCronetEngine.Builder(context);
      builder
          .enableNetworkQualityEstimator(true)
          .enableHttpCache(Builder.HTTP_CACHE_DISABLED, 0)
          .enableHttp2(true)
          .enableQuic(true);

      ExperimentalCronetEngine experimentalCronetEngine = builder.build();

      experimentalCronetEngine.addRequestFinishedListener(
          new Listener(CronetStreamFetcher.DEFAULT_REQUEST_EXECUTOR) {
            @Override
            public void onRequestFinished(RequestFinishedInfo requestFinishedInfo) {
              if (Log.isLoggable(TAG, Log.DEBUG)) {
                onRequestFinishedHandle(requestFinishedInfo);
              }
            }
          });
      return experimentalCronetEngine;
    }

    /** Constructor for a new Factory that runs requests using a static singleton client. */
    public Factory(Context context) {
      this(getInternalClient(context), CronetStreamFetcher.DEFAULT_REQUEST_EXECUTOR);
    }

    /**
     * Constructor for a new Factory that runs requests using given client.
     *
     * @param client this is typically an instance of {@code OkHttpClient}.
     */
    public Factory(
        @NonNull ExperimentalCronetEngine client, @NonNull java.util.concurrent.Executor executor) {
      this.client = client;
      this.executor = executor;
    }

    @NonNull
    @Override
    public ModelLoader<GlideUrl, InputStream> build(MultiModelLoaderFactory multiFactory) {
      return new CronetUrlLoader(client);
    }

    @Override
    public void teardown() {
      // Do nothing, this instance doesn't own the client.
    }
  }

  /**
   * Print the request info.
   *
   * @param requestInfo requestInfo
   */
  private static void onRequestFinishedHandle(final RequestFinishedInfo requestInfo) {
    Log.d(TAG, "############# url: " + requestInfo.getUrl() + " #############");
    Log.d(TAG, "onRequestFinished: " + requestInfo.getFinishedReason());
    RequestFinishedInfo.Metrics metrics = requestInfo.getMetrics();
    if (metrics != null) {
      Log.d(
          TAG,
          "RequestStart: "
              + (metrics.getRequestStart() == null ? -1 : metrics.getRequestStart().getTime()));
      Log.d(
          TAG,
          "DnsStart: " + (metrics.getDnsStart() == null ? -1 : metrics.getDnsStart().getTime()));
      Log.d(TAG, "DnsEnd: " + (metrics.getDnsEnd() == null ? -1 : metrics.getDnsEnd().getTime()));
      Log.d(
          TAG,
          "ConnectStart: "
              + (metrics.getConnectStart() == null ? -1 : metrics.getConnectStart().getTime()));
      Log.d(
          TAG,
          "ConnectEnd: "
              + (metrics.getConnectEnd() == null ? -1 : metrics.getConnectEnd().getTime()));
      Log.d(
          TAG,
          "SslStart: " + (metrics.getSslStart() == null ? -1 : metrics.getSslStart().getTime()));
      Log.d(TAG, "SslEnd: " + (metrics.getSslEnd() == null ? -1 : metrics.getSslEnd().getTime()));
      Log.d(
          TAG,
          "SendingStart: "
              + (metrics.getSendingStart() == null ? -1 : metrics.getSendingStart().getTime()));
      Log.d(
          TAG,
          "SendingEnd: "
              + (metrics.getSendingEnd() == null ? -1 : metrics.getSendingEnd().getTime()));
      Log.d(
          TAG,
          "PushStart: " + (metrics.getPushStart() == null ? -1 : metrics.getPushStart().getTime()));
      Log.d(
          TAG, "PushEnd: " + (metrics.getPushEnd() == null ? -1 : metrics.getPushEnd().getTime()));
      Log.d(
          TAG,
          "ResponseStart: "
              + (metrics.getResponseStart() == null ? -1 : metrics.getResponseStart().getTime()));
      Log.d(
          TAG,
          "RequestEnd: "
              + (metrics.getRequestEnd() == null ? -1 : metrics.getRequestEnd().getTime()));
      Log.d(TAG, "TotalTimeMs: " + metrics.getTotalTimeMs());
      Log.d(TAG, "RecvByteCount: " + metrics.getReceivedByteCount());
      Log.d(TAG, "SentByteCount: " + metrics.getSentByteCount());
      Log.d(TAG, "SocketReused: " + metrics.getSocketReused());
      Log.d(TAG, "TtfbMs: " + metrics.getTtfbMs());
    }

    Exception exception = requestInfo.getException();
    if (exception != null) {
      Log.e(TAG, Log.getStackTraceString(exception));
    }

    UrlResponseInfo urlResponseInfo = requestInfo.getResponseInfo();
    if (urlResponseInfo != null) {
      Log.d(TAG, "Cache: " + urlResponseInfo.wasCached());
      Log.d(TAG, "Protocol: " + urlResponseInfo.getNegotiatedProtocol());
      Log.d(TAG, "HttpCode: " + urlResponseInfo.getHttpStatusCode());
      Log.d(TAG, "ProxyServer: " + urlResponseInfo.getProxyServer());
      List<Entry<String, String>> headers = urlResponseInfo.getAllHeadersAsList();
      for (Map.Entry<String, String> entry : headers) {
        Log.d(TAG, "=== " + entry.getKey() + " : " + entry.getValue() + " ===");
      }
    }

    Log.d(TAG, "############# END #############");
  }
}
