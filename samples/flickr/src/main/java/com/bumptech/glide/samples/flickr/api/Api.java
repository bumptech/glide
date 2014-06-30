package com.bumptech.glide.samples.flickr.api;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Api {
    private static Api API;
    private static final String TAG = "FlickrApi";
    private static final String API_KEY = "f0e6fbb5fdf1f3842294a1d21f84e8a6";
    private static final String SIGNED_API_URL = "https://api.flickr.com/services/rest/?method=%s&format=json&api_key="
            + API_KEY;
    //incomplete size independent url for photos that can be cached per photo
    private static final String CACHEABLE_PHOTO_URL = "http://farm%s.staticflickr.com/%s/%s_%s_";


    private static final Map<Integer, String> EDGE_TO_SIZE_KEY = new HashMap<Integer, String>() {{
        put(75, "s");
        put(100, "t");
        put(150, "q");
        put(240, "m");
        put(320, "n");
        put(640, "z");
        put(1024, "b");
    }};
    private static final List<Integer> SORTED_SIZE_KEYS = new ArrayList<Integer>(EDGE_TO_SIZE_KEY.size());
    static {
        SORTED_SIZE_KEYS.addAll(EDGE_TO_SIZE_KEY.keySet());
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

    public static String getCacheableUrl(Photo photo) {
        return String.format(CACHEABLE_PHOTO_URL, photo.farm, photo.server, photo.id, photo.secret);
    }

    public static String getPhotoURL(Photo photo, int width, int height) {
        return getPhotoUrl(photo, getSizeKey(width, height));
    }

    private static String getUrlForMethod(String method) {
        return String.format(SIGNED_API_URL, method);
    }

    private static String getSearchUrl(String text) {
        return getUrlForMethod("flickr.photos.search") + "&text=" + text + "&per_page=300";
    }

    private static String getPhotoUrl(Photo photo, String sizeKey) {
        return photo.getPartialUrl() + sizeKey + ".jpg";
    }

    public interface SearchListener {
        public void onSearchCompleted(String searchString, List<Photo> photos);
        public void onSearchFailed(String searchString, Exception e);
    }

    public static Api get(Context context) {
        if (API == null) {
            API = new Api(context);
        }
        return API;
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

        StringRequest request = new StringRequest(Request.Method.GET, getSearchUrl(text),
                new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    //cut out initial flickJsonApi(
                    JSONObject searchResults = new JSONObject(response.substring(14, response.length() - 1));
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
        public final String searchString;
        public final List<Photo> results;

        public SearchResult(String searchString, List<Photo> results) {

            this.searchString = searchString;
            this.results = results;
        }

    }
}
