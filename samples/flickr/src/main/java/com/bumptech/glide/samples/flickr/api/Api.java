package com.bumptech.glide.samples.flickr.api;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.util.LruCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class for interfacing with Flickr's http API.
 */
public class Api {
  private static Api api;
  private static final String TAG = "FlickrApi";
  private static final String API_KEY = "f0e6fbb5fdf1f3842294a1d21f84e8a6";
  private static final String SIGNED_API_URL =
      "https://api.flickr.com/services/rest/?method=%s&format=json&api_key=" + API_KEY;
  // Incomplete size independent url for photos that can be cached per photo
  private static final String CACHEABLE_PHOTO_URL = "http://farm%s.staticflickr.com/%s/%s_%s_";
  private static final int MAX_URLS_TO_CACHE = 2000;
  private static final LruCache<UrlCacheKey, String> CACHED_URLS =
      new LruCache<>(MAX_URLS_TO_CACHE);

  private static final SparseArray<String> EDGE_TO_SIZE_KEY = new SparseArray<String>() {
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
  private static final List<Integer> SORTED_SIZE_KEYS =
      new ArrayList<Integer>(EDGE_TO_SIZE_KEY.size());

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
    for (int edge : SORTED_SIZE_KEYS) {
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

  public static String getCacheableUrl(Photo photo) {
    return String.format(CACHEABLE_PHOTO_URL, photo.getFarm(), photo.getServer(), photo.getId(),
        photo.getSecret());
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

  private static String getSearchUrl(String text) {
    return getUrlForMethod("flickr.photos.search") + "&text=" + text + "&per_page=300";
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

  /**
   * An interface for listening for search results from the Flickr API.
   */
  public interface SearchListener {
    /**
     * Called when a search completes successfully.
     *
     * @param searchString The term that was searched for.
     * @param photos       A list of images that were found for the given search term.
     */
    public void onSearchCompleted(String searchString, List<Photo> photos);

    /**
     * Called when a search fails.
     *
     * @param searchString The term that was searched for.
     * @param e            The exception that caused the search to fail.
     */
    public void onSearchFailed(String searchString, Exception e);
  }

  public static Api get(Context context) {
    if (api == null) {
      api = new Api(context);
    }
    return api;
  }

  private final RequestQueue requestQueue;
  private final Set<SearchListener> searchListeners = new HashSet<SearchListener>();
  private SearchResult lastSearchResult;

  protected Api(Context context) {
    this.requestQueue = Volley.newRequestQueue(context.getApplicationContext());
  }

  public void registerSearchListener(SearchListener searchListener) {
    searchListeners.add(searchListener);
  }

  public void unregisterSearchListener(SearchListener searchListener) {
    searchListeners.remove(searchListener);
  }

  public void search(final String text) {
    if (lastSearchResult != null && TextUtils.equals(lastSearchResult.searchString, text)) {
      for (SearchListener listener : searchListeners) {
        listener.onSearchCompleted(lastSearchResult.searchString, lastSearchResult.results);
      }
      return;
    }

    StringRequest request =
        new StringRequest(Request.Method.GET, getSearchUrl(text), new Response.Listener<String>() {
          @Override
          public void onResponse(String response) {
            try {
              // Cut out initial flickJsonApi(
              JSONObject searchResults =
                  new JSONObject(response.substring(14, response.length() - 1));
              JSONArray photos = searchResults.getJSONObject("photos").getJSONArray("photo");
              List<Photo> results = new ArrayList<Photo>(photos.length());
              for (int i = 0; i < photos.length(); i++) {
                results.add(new Photo(photos.getJSONObject(i)));
              }
              lastSearchResult = new SearchResult(text, results);
              for (SearchListener listener : searchListeners) {
                listener.onSearchCompleted(text, results);
              }
            } catch (JSONException e) {
              for (SearchListener listener : searchListeners) {
                listener.onSearchFailed(text, e);
              }
              if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Search failed response=" + response, e);
              }
            }
          }
        }, new Response.ErrorListener() {
          @Override
          public void onErrorResponse(VolleyError error) {
            for (SearchListener listener : searchListeners) {
              listener.onSearchFailed(text, error);
            }
          }
        });
    request.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 3,
        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    requestQueue.add(request);
  }

  private static class SearchResult {
    private final String searchString;
    private final List<Photo> results;

    public SearchResult(String searchString, List<Photo> results) {

      this.searchString = searchString;
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
