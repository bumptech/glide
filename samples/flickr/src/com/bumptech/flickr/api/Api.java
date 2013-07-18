package com.bumptech.flickr.api;

import android.content.Context;
import com.android.volley.Request;
import com.bumptech.flickr.R;
import com.bumptech.glide.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/6/13
 * Time: 10:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class Api {
    private static Api API;
    public static final String SEARCH_COMPLETED_ACTION = "search_completed";

    private static final String API_KEY = "f0e6fbb5fdf1f3842294a1d21f84e8a6";
    private static final String SIGNED_API_URL = "http://api.flickr.com/services/rest/?method=%s&format=json&api_key=" + API_KEY;
    private static final String PHOTO_URL = "http://farm%s.staticflickr.com/%s/%s_%s_%s.jpg";

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
    }

    public interface PhotoCallback {
        public void onDownloadComplete(String path);
    }

    private final Downloader downloader;
    private Set<String> downloadedFilesNames = new HashSet<String>();
    private final String sizeKey;

    public static Api get(Context applicationContext) {
        if (API == null) {
            API = new Api(applicationContext, applicationContext.getResources().getDimensionPixelSize(R.dimen.large_photo_side));
        }
        return API;
    }

    protected Api(Context applicationContext, int maxPhotoSize) {
        this.downloader = Downloader.get(applicationContext);
        this.sizeKey = getSizeKey(maxPhotoSize, maxPhotoSize);
    }

    public static URL getPhotoURL(Photo photo, int width, int height) {
        try {
            return new URL(getPhotoUrl(photo, getSizeKey(width, height)));
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getUrlForMethod(String method) {
        return String.format(SIGNED_API_URL, method);
    }

    private static String getSearchUrl(String text) {
        return getUrlForMethod("flickr.photos.search") + "&text=" + text + "&per_page=500";
    }

    private static String getPhotoUrl(Photo photo, String sizeKey) {
        return String.format(PHOTO_URL, photo.farm, photo.server, photo.id, photo.secret, sizeKey);
    }

    public void search(String text, final SearchCallback cb) {
        Log.d("API: searching");
        downloader.download(getSearchUrl(text), new Downloader.StringCallback() {
            @Override
            public void onDownloadReady(String result) {
                try {
                    //cut out initial flickJsonApi(
                    JSONObject searchResults = new JSONObject(result.substring(14, result.length()-1));
                    JSONArray photos = searchResults.getJSONObject("photos").getJSONArray("photo");
                    List<Photo> results = new ArrayList<Photo>(photos.length());
                    for (int i = 0; i < photos.length(); i++) {
                        results.add(new Photo(photos.getJSONObject(i)));
                    }
                    cb.onSearchCompleted(results);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public Request downloadPhoto(Photo photo, File cacheDir, final PhotoCallback cb) {
        File out = new File(cacheDir.getPath() + File.separator + photo.id + photo.secret + sizeKey);
        final String path = out.getPath();
        Request result = null;
        if (downloadedFilesNames.contains(path)) {
            cb.onDownloadComplete(path);
        } else {
            Log.d("API: missing photo, downloading");
            result = downloader.download(getPhotoUrl(photo, sizeKey), out, new Downloader.DiskCallback() {
                @Override
                public void onDownloadReady(String path) {
                    downloadedFilesNames.add(path);
                    cb.onDownloadComplete(path);
                }
            });
       }
        return result;
    }
}
