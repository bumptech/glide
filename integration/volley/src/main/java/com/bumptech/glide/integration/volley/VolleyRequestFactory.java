package com.bumptech.glide.integration.volley;

import com.android.volley.Request;
import com.android.volley.Request.Priority;
import com.bumptech.glide.load.data.DataFetcher.DataCallback;
import java.io.InputStream;
import java.util.Map;

/** Used to construct a custom Volley request, such as for authentication header decoration. */
public interface VolleyRequestFactory {

  /**
   * Returns a Volley request for the given image url. The given future should be put as a listener
   * or called when the request completes.
   */
  Request<byte[]> create(
      String url,
      DataCallback<? super InputStream> callback,
      Priority priority,
      Map<String, String> headers);
}
