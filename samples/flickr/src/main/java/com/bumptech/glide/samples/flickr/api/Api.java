package com.bumptech.glide.samples.flickr.api;

import android.content.Context;
import android.util.SparseArray;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.util.LruCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A class for interfacing with Flickr's http API. */
public final class Api {
  private static Api api;
  private static final String API_KEY = "f0e6fbb5fdf1f3842294a1d21f84e8a6";
  private static final String SIGNED_API_URL =
      "https://api.flickr.com/services/rest/?method=%s&format=json&api_key=" + API_KEY;
  // Incomplete size independent url for photos that can be cached per photo
  private static final String CACHEABLE_PHOTO_URL = "http://farm%s.staticflickr.com/%s/%s_%s_";
  private static final int MAX_URLS_TO_CACHE = 2000;
  private static final LruCache<UrlCacheKey, String> CACHED_URLS =
      new LruCache<>(MAX_URLS_TO_CACHE);
  private static final int MAX_ITEMS_PER_PAGE = 300;
  /**
   * Safe search is on by default in Flickr's API and/or enabling it isn't very effective. Instead
   * we'll force all images to be from Flickr's commons project, which tends to be historic images.
   * Those appear much safer than a standard search.
   */
  private static final String SAFE_SEARCH = "&is_commons=1";

  private static final String PER_PAGE = "&per_page=" + MAX_ITEMS_PER_PAGE;

  private static final SparseArray<String> EDGE_TO_SIZE_KEY =
      new SparseArray<String>() {
        {
          put(75, "s");
          put(100, "t");
          put(150, "q");
          put(240, "m");
          put(320, "n");
          put(640, "z");
          put(1024, "b");
        }
      };
  private static final List<Integer> SORTED_SIZE_KEYS = new ArrayList<>(EDGE_TO_SIZE_KEY.size());

  static {
    for (int i = 0; i < EDGE_TO_SIZE_KEY.size(); i++) {
      SORTED_SIZE_KEYS.add(EDGE_TO_SIZE_KEY.keyAt(i));
    }
    Collections.sort(SORTED_SIZE_KEYS);
  }

  public static final int SQUARE_THUMB_SIZE = SORTED_SIZE_KEYS.get(0);

  private static String getSizeKey(int width, int height) {
    final int largestEdge = Math.max(width, height);

    String result = EDGE_TO_SIZE_KEY.get(SORTED_SIZE_KEYS.get(SORTED_SIZE_KEYS.size() - 1));
    for (int edge : SORTED_SIZE_KEYS) {
      if (largestEdge <= edge) {
        result = EDGE_TO_SIZE_KEY.get(edge);
        break;
      }
    }
    return result;
  }

  private static List<String> getLargerSizeKeys(int width, int height) {
    final int largestEdge = Math.max(width, height);

    boolean isFirstLargest = true;
    List<String> result = new ArrayList<>();
    int size = SORTED_SIZE_KEYS.size();
    for (int i = 0; i < size; i++) {
      int edge = SORTED_SIZE_KEYS.get(i);
      if (largestEdge <= edge) {
        if (isFirstLargest) {
          isFirstLargest = false;
        } else {
          result.add(EDGE_TO_SIZE_KEY.get(edge));
        }
      }
    }
    return result;
  }

  static String getCacheableUrl(Photo photo) {
    return String.format(
        CACHEABLE_PHOTO_URL, photo.getFarm(), photo.getServer(), photo.getId(), photo.getSecret());
  }

  public static String getPhotoURL(Photo photo, int width, int height) {
    return getPhotoUrl(photo, getSizeKey(width, height));
  }

  public static List<String> getAlternateUrls(Photo photo, int width, int height) {
    List<String> result = new ArrayList<>();
    for (String sizeKey : getLargerSizeKeys(width, height)) {
      result.add(getPhotoUrl(photo, sizeKey));
    }
    return result;
  }

  private static String getUrlForMethod(String method) {
    return String.format(SIGNED_API_URL, method);
  }

  private static String getPhotoUrl(Photo photo, String sizeKey) {
    UrlCacheKey entry = new UrlCacheKey(photo, sizeKey);
    String result = CACHED_URLS.get(entry);
    if (result == null) {
      result = photo.getPartialUrl() + sizeKey + ".jpg";
      CACHED_URLS.put(entry, result);
    }
    return result;
  }

  static String getSearchUrl(String text, boolean requireSafeOverQuality) {
    return getUrlForMethod("flickr.photos.search")
        + "&text="
        + text
        + PER_PAGE
        + (requireSafeOverQuality ? SAFE_SEARCH : "");
  }

  static String getRecentUrl() {
    return getUrlForMethod("flickr.photos.getRecent" + PER_PAGE);
  }

  /** An interface for listening for search results from the Flickr API. */
  public interface QueryListener {
    /**
     * Called when a search completes successfully.
     *
     * @param query The query used to obtain the results.
     * @param photos A list of images that were found for the given search term.
     */
    void onSearchCompleted(Query query, List<Photo> photos);

    /**
     * Called when a search fails.
     *
     * @param query The query we attempted to obtain results for.
     * @param e The exception that caused the search to fail.
     */
    void onSearchFailed(Query query, Exception e);
  }

  public static Api get(Context context) {
    if (api == null) {
      api = new Api(context);
    }
    return api;
  }

  private final RequestQueue requestQueue;
  private final Set<QueryListener> queryListeners = new HashSet<>();
  private QueryResult lastQueryResult;

  private Api(Context context) {
    this.requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    QueryListener queryListener =
        new QueryListener() {
          @Override
          public void onSearchCompleted(Query query, List<Photo> photos) {
            lastQueryResult = new QueryResult(query, photos);
          }

          @Override
          public void onSearchFailed(Query query, Exception e) {
            lastQueryResult = null;
          }
        };
    queryListeners.add(queryListener);
  }

  public void registerSearchListener(QueryListener queryListener) {
    queryListeners.add(queryListener);
  }

  public void unregisterSearchListener(QueryListener queryListener) {
    queryListeners.remove(queryListener);
  }

  public void query(Query query) {
    if (lastQueryResult != null && lastQueryResult.query.equals(query)) {
      for (QueryListener listener : queryListeners) {
        listener.onSearchCompleted(lastQueryResult.query, lastQueryResult.results);
      }
      return;
    }

    FlickrQueryResponseListener responseListener =
        new FlickrQueryResponseListener(new PhotoJsonStringParser(), query, queryListeners);
    StringRequest request =
        new StringRequest(Request.Method.GET, query.getUrl(), responseListener, responseListener);
    request.setRetryPolicy(
        new DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    requestQueue.add(request);
  }

  private static class QueryResult {
    private final Query query;
    private final List<Photo> results;

    QueryResult(Query query, List<Photo> results) {
      this.query = query;
      this.results = results;
    }
  }

  private static final class UrlCacheKey {
    private final Photo photo;
    private final String sizeKey;

    private UrlCacheKey(Photo photo, String sizeKey) {
      this.photo = photo;
      this.sizeKey = sizeKey;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof UrlCacheKey) {
        UrlCacheKey other = (UrlCacheKey) o;
        return photo.equals(other.photo) && sizeKey.equals(other.sizeKey);
      }
      return false;
    }

    @Override
    public int hashCode() {
      int result = photo.hashCode();
      result = 31 * result + sizeKey.hashCode();
      return result;
    }
  }
}
