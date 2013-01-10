package com.bumptech.flickr.api;

import com.bumptech.photos.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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
    private static final String API_KEY = "f0e6fbb5fdf1f3842294a1d21f84e8a6";
    private static final String SIGNED_API_URL = "http://api.flickr.com/services/rest/?method=%s&format=json&api_key=" + API_KEY;
    private static final String PHOTO_URL = "http://farm%s.staticflickr.com/%s/%s_%s_%s.jpg";
    private final Downloader downloader;

    private static final Map<Integer, String> EDGE_TO_SIZE_KEY = new HashMap<Integer, String>() {{
        put(75, "s");
        put(100, "t");
        put(150, "q");
        put(240, "m");
        put(320, "n");
        put(500, "-");
        put(640, "z");
        put(1024, "b");
        put(Integer.MAX_VALUE, "o");
    }};

    private static String getSizeKey(int width, int height) {
        final int largestEdge = width > height ? width : height;

        final String result = EDGE_TO_SIZE_KEY.get(Integer.MAX_VALUE);
        for (int edge : EDGE_TO_SIZE_KEY.keySet()) {
            if (largestEdge <= edge) {
                return EDGE_TO_SIZE_KEY.get(edge);
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

    public Api() {
        this.downloader = Downloader.get();
    }

    private static String getUrlForMethod(String method) {
        return String.format(SIGNED_API_URL, method);
    }

    private static String getSearchUrl(String text) {
        return getUrlForMethod("flickr.photos.search") + "&text=" + text + "&per_page=500";
    }

    private static String getPhotoUrl(Photo photo, int width, int height) {
        String sizeKey = getSizeKey(width, height);
        return String.format(PHOTO_URL, photo.farm, photo.server, photo.id, photo.secret, sizeKey);
    }

    public void search(String text, final SearchCallback cb) {
        Log.d("API: searching");
        downloader.download(getSearchUrl(text), new Downloader.MemoryCallback() {
            @Override
            public void onDownloadReady(byte[] data) {
                try {
                    String stringResults = new String(data, "UTF-8");
                    //cut out initial flickJsonApi(
                    JSONObject searchResults = new JSONObject(stringResults.substring(14, stringResults.length()-1));
                    JSONArray photos = searchResults.getJSONObject("photos").getJSONArray("photo");
                    List<Photo> results = new ArrayList<Photo>(photos.length());
                    for (int i = 0; i < photos.length(); i++) {
                        results.add(new Photo(photos.getJSONObject(i)));
                    }
                    cb.onSearchCompleted(results);
                } catch (JSONException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        });
    }

    public void downloadPhoto(Photo photo, int width, int height, File cacheDir, final PhotoCallback cb) {
        File out = new File(cacheDir.getPath() + File.separator + photo.id + photo.secret + width + height);
        if (out.exists()) {
            cb.onDownloadComplete(out.getPath());
        } else {
            Log.d("API: missing photo, downloading");
            downloader.download(getPhotoUrl(photo, width, height), out, new Downloader.DiskCallback() {
                @Override
                public void onDownloadReady(String path) {
                    cb.onDownloadComplete(path);
                }
            });
        }
    }
}
