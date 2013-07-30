package com.bumptech.flickr.api;

import android.content.Context;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/6/13
 * Time: 10:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class Api {
    private static Api API;
    private static final String API_KEY = "f0e6fbb5fdf1f3842294a1d21f84e8a6";
    private static final String SIGNED_API_URL = "http://api.flickr.com/services/rest/?method=%s&format=json&api_key=" + API_KEY;
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

    private final RequestQueue requestQueue;

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

    public interface SearchCallback {
        public void onSearchCompleted(List<Photo> photos);
        public void onSearchFailed(Exception e);
    }

    public static Api get(Context applicationContext) {
        if (API == null) {
            API = new Api(applicationContext);
        }
        return API;
    }

    protected Api(Context applicationContext) {
        this.requestQueue = Glide.get().getRequestQueue(applicationContext);
    }

    public static String getPhotoURL(Photo photo, int width, int height) {
        return getPhotoUrl(photo, getSizeKey(width, height));
    }

    public static String getCacheableUrl(Photo photo) {
        return String.format(CACHEABLE_PHOTO_URL, photo.farm, photo.server, photo.id, photo.secret);
    }

    private static String getUrlForMethod(String method) {
        return String.format(SIGNED_API_URL, method);
    }

    private static String getSearchUrl(String text) {
        return getUrlForMethod("flickr.photos.search") + "&text=" + text + "&per_page=500";
    }

    private static String getPhotoUrl(Photo photo, String sizeKey) {
        return photo.getPartialUrl() + sizeKey + ".jpg";
    }

    public void search(String text, final SearchCallback cb) {
        StringRequest request = new StringRequest(Request.Method.GET, getSearchUrl(text), new Response.Listener<String>() {
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
                    cb.onSearchCompleted(results);
                } catch (JSONException e) {
                    cb.onSearchFailed(e);
                    Log.d("ERROR: response=" + response);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                cb.onSearchFailed(error);
            }
        });
        request.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }
}
