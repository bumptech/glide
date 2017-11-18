package com.bumptech.glide.samples.giphy;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;

/**
 * A java wrapper for Giphy's http api based on https://github.com/Giphy/GiphyAPI.
 */
public final class Api {
  private static volatile Api api = null;
  private static final String BETA_KEY = "dc6zaTOxFJmzC";
  private static final String BASE_URL = "https://api.giphy.com/";
  private static final String TRENDING_PATH = "v1/gifs/trending";
  private static final int LIMIT = 100;
  private static final int OFFSET = 0;
  private final Handler bgHandler;
  private final Handler mainHandler;
  private final HashSet<Monitor> monitors = new HashSet<>();

  private static String signUrl(String url) {
    return url + "&api_key=" + BETA_KEY;
  }

  private static String getTrendingUrl() {
    return signUrl(BASE_URL + TRENDING_PATH + "?limit=" + LIMIT + "&offset=" + OFFSET);
  }

  /**
   * An interface for listening for search results.
   */
  public interface Monitor {
    /**
     * Called when a search completes.
     *
     * @param result The results returned from Giphy's search api.
     */
    void onSearchComplete(SearchResult result);
  }

  static Api get() {
    if (api == null) {
      synchronized (Api.class) {
        if (api == null) {
          api = new Api();
        }
      }
    }
    return api;
  }

  private Api() {
    HandlerThread bgThread = new HandlerThread("api_thread");
    bgThread.start();
    bgHandler = new Handler(bgThread.getLooper());
    mainHandler = new Handler(Looper.getMainLooper());
    // Do nothing.
  }

  void addMonitor(Monitor monitor) {
    monitors.add(monitor);
  }

  void removeMonitor(Monitor monitor) {
    monitors.remove(monitor);
  }

  void getTrending() {
    String trendingUrl = getTrendingUrl();
    query(trendingUrl);
  }

  private void query(final String apiUrl) {
    bgHandler.post(new Runnable() {
      @Override
      public void run() {
        URL url;
        try {
          url = new URL(apiUrl);
        } catch (MalformedURLException e) {
          throw new RuntimeException(e);
        }

        HttpURLConnection urlConnection = null;
        InputStream is = null;
        SearchResult result = new SearchResult();
        try {
          urlConnection = (HttpURLConnection) url.openConnection();
          is = urlConnection.getInputStream();
          InputStreamReader reader = new InputStreamReader(is);
          result = new Gson().fromJson(reader, SearchResult.class);
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          if (is != null) {
            try {
              is.close();
            } catch (IOException e) {
              // Do nothing.
            }
          }
          if (urlConnection != null) {
            urlConnection.disconnect();
          }
        }

        final SearchResult finalResult = result;
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            for (Monitor monitor : monitors) {
              monitor.onSearchComplete(finalResult);
            }
          }
        });
      }
    });
  }

  /**
   * A POJO mirroring the top level result JSON object returned from Giphy's api.
   */
  public static final class SearchResult {
    public GifResult[] data;

    @Override
    public String toString() {
      return "SearchResult{" + "data=" + Arrays.toString(data) + '}';
    }
  }

  /**
   * A POJO mirroring an individual GIF image returned from Giphy's api.
   */
  public static final class GifResult {
    public String id;
    GifUrlSet images;

    @Override
    public String toString() {
      return "GifResult{" + "id='" + id + '\'' + ", images=" + images
          + '}';
    }
  }

  /**
   * A POJO mirroring a JSON object with a put of urls of different sizes and dimensions returned
   * for a single image from Giphy's api.
   */
  public static final class GifUrlSet {
    GifImage original;
    GifImage fixed_width;
    GifImage fixed_height;

    @Override
    public String toString() {
      return "GifUrlSet{" + "original=" + original + ", fixed_width="
          + fixed_width + ", fixed_height=" + fixed_height
          + '}';
    }
  }

  /**
   * A POJO mirroring a JSON object for an image with one particular url, size and dimension
   * returned from Giphy's api.
   */
  public static final class GifImage {
    String url;
    int width;
    int height;

    @Override
    public String toString() {
      return "GifImage{" + "url='" + url + '\'' + ", width=" + width + ", height=" + height + '}';
    }
  }
}
