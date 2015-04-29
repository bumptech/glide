package com.bumptech.glide.samples.flickr;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.prefill.PreFillType;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.samples.flickr.api.Api;
import com.bumptech.glide.samples.flickr.api.Photo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * An activity that allows users to search for images on Flickr and that contains a series of
 * fragments that display retrieved image thumbnails.
 */
public class FlickrSearchActivity extends ActionBarActivity {
  private static final String TAG = "FlickrSearchActivity";
  private static final String STATE_SEARCH_STRING = "state_search_string";

  private EditText searchText;
  private View searching;
  private TextView searchTerm;
  private Set<PhotoViewer> photoViewers = new HashSet<PhotoViewer>();
  private List<Photo> currentPhotos = new ArrayList<Photo>();
  private View searchLoading;
  private String currentSearchString;
  private final SearchListener searchListener = new SearchListener();
  private BackgroundThumbnailFetcher backgroundThumbnailFetcher;
  private HandlerThread backgroundThread;
  private Handler backgroundHandler;

  private enum Page {
    SMALL,
    MEDIUM,
    LIST
  }

  private static final Map<Page, Integer> PAGE_TO_TITLE = new HashMap<Page, Integer>() {
    {
      put(Page.SMALL, R.string.small);
      put(Page.MEDIUM, R.string.medium);
      put(Page.LIST, R.string.list);
    }
  };

  @Override
  public void onAttachFragment(Fragment fragment) {
    super.onAttachFragment(fragment);
    if (fragment instanceof PhotoViewer) {
      PhotoViewer photoViewer = (PhotoViewer) fragment;
      photoViewer.onPhotosUpdated(currentPhotos);
      if (!photoViewers.contains(photoViewer)) {
        photoViewers.add(photoViewer);
      }
    }
  }

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    backgroundThread = new HandlerThread("BackgroundThumbnailHandlerThread");
    backgroundThread.start();
    backgroundHandler = new Handler(backgroundThread.getLooper());

    setContentView(R.layout.flickr_search_activity);
    StrictMode
        .setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
    searching = findViewById(R.id.searching);
    searchLoading = findViewById(R.id.search_loading);
    searchTerm = (TextView) findViewById(R.id.search_term);

    searchText = (EditText) findViewById(R.id.search_text);
    searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
          executeSearch();
          return true;
        }
        return false;
      }
    });

    final Button search = (Button) findViewById(R.id.search);
    search.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        executeSearch();
      }
    });

    final ViewPager pager = (ViewPager) findViewById(R.id.view_pager);
    pager.setPageMargin(50);
    pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int i, float v, int i2) {
      }

      @Override
      public void onPageSelected(int position) {
        getSupportActionBar().getTabAt(position).select();
      }

      @Override
      public void onPageScrollStateChanged(int i) {
      }
    });


    final ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

    for (Page page : Page.values()) {
      final int textId = PAGE_TO_TITLE.get(page);
      actionBar.addTab(actionBar.newTab().setText(textId).setTabListener(new TabListener(pager)));
    }

    pager.setAdapter(new FlickrPagerAdapter(getSupportFragmentManager()));

    Api.get(this).registerSearchListener(searchListener);
    if (savedInstanceState != null) {
      String savedSearchString = savedInstanceState.getString(STATE_SEARCH_STRING);
      if (!TextUtils.isEmpty(savedSearchString)) {
        executeSearch(savedSearchString);
      }
    }

    final Resources res = getResources();
    int smallGridSize = res.getDimensionPixelSize(R.dimen.small_photo_side);
    int mediumGridSize = res.getDimensionPixelSize(R.dimen.medium_photo_side);
    int listHeightSize = res.getDimensionPixelSize(R.dimen.flickr_list_item_height);
    int screenWidth = getScreenWidth();

    // Weight values determined experimentally by measuring the number of incurred GCs while
    // scrolling through the various photo grids/lists.
    Glide.get(this).preFillBitmapPool(new PreFillType.Builder(smallGridSize).setWeight(1),
        new PreFillType.Builder(mediumGridSize).setWeight(1),
        new PreFillType.Builder(screenWidth / 2, listHeightSize).setWeight(6));
  }

  private int getScreenWidth() {
    return getResources().getDisplayMetrics().widthPixels;
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (!TextUtils.isEmpty(currentSearchString)) {
      outState.putString(STATE_SEARCH_STRING, currentSearchString);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Api.get(this).unregisterSearchListener(searchListener);
    if (backgroundThumbnailFetcher != null) {
      backgroundThumbnailFetcher.cancel();
      backgroundThumbnailFetcher = null;
      backgroundThread.quit();
      backgroundThread = null;
    }
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  @Override
  public void onTrimMemory(int level) {
    super.onTrimMemory(level);
    Glide.get(this).trimMemory(level);
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    Glide.get(this).clearMemory();
  }

  private void executeSearch() {
    String searchString = searchText.getText().toString();
    searchText.getText().clear();
    executeSearch(searchString);
  }

  private void executeSearch(String searchString) {
    currentSearchString = searchString;

    if (TextUtils.isEmpty(searchString)) {
      return;
    }

    searching.setVisibility(View.VISIBLE);
    searchLoading.setVisibility(View.VISIBLE);
    searchTerm.setText(getString(R.string.searching_for, currentSearchString));

    Api.get(this).search(currentSearchString);
  }

  private static class TabListener implements ActionBar.TabListener {
    private final ViewPager pager;

    public TabListener(ViewPager pager) {
      this.pager = pager;
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
      pager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }
  }

  private class SearchListener implements Api.SearchListener {
    @Override
    public void onSearchCompleted(String searchString, List<Photo> photos) {
      if (!TextUtils.equals(currentSearchString, searchString)) {
        return;
      }

      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Search completed, got " + photos.size() + " results");
      }
      searching.setVisibility(View.INVISIBLE);

      for (PhotoViewer viewer : photoViewers) {
        viewer.onPhotosUpdated(photos);
      }

      if (backgroundThumbnailFetcher != null) {
        backgroundThumbnailFetcher.cancel();
      }

      backgroundThumbnailFetcher =
          new BackgroundThumbnailFetcher(FlickrSearchActivity.this, photos);
      backgroundHandler.post(backgroundThumbnailFetcher);

      currentPhotos = photos;
    }

    @Override
    public void onSearchFailed(String searchString, Exception e) {
      if (!TextUtils.equals(currentSearchString, searchString)) {
        return;
      }

      if (Log.isLoggable(TAG, Log.ERROR)) {
        Log.e(TAG, "Search failed", e);
      }
      searching.setVisibility(View.VISIBLE);
      searchLoading.setVisibility(View.INVISIBLE);
      searchTerm.setText(getString(R.string.search_failed, currentSearchString));
    }
  }

  private class FlickrPagerAdapter extends FragmentPagerAdapter {

    private int mLastPosition = -1;
    private Fragment mLastFragment;

    public FlickrPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      return pageToFragment(position);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
      super.setPrimaryItem(container, position, object);
      if (position != mLastPosition) {
        if (mLastPosition >= 0) {
          Glide.with(mLastFragment).pauseRequests();
        }
        Fragment current = (Fragment) object;
        mLastPosition = position;
        mLastFragment = current;
        if (current.isAdded()) {
          Glide.with(current).resumeRequests();
        }
      }
    }

    @Override
    public int getCount() {
      return Page.values().length;
    }

    private Fragment pageToFragment(int position) {
      Page page = Page.values()[position];
      if (page == Page.SMALL) {
        int pageSize = getPageSize(R.dimen.small_photo_side);
        return FlickrPhotoGrid.newInstance(pageSize, 30, false /*thumbnail*/);
      } else if (page == Page.MEDIUM) {
        int pageSize = getPageSize(R.dimen.medium_photo_side);
        return FlickrPhotoGrid.newInstance(pageSize, 10, true /*thumbnail*/);
      } else if (page == Page.LIST) {
        return FlickrPhotoList.newInstance();
      } else {
        throw new IllegalArgumentException("No fragment class for page=" + page);
      }
    }

    private int getPageSize(int id) {
      return getResources().getDimensionPixelSize(id);
    }
  }

  private static class BackgroundThumbnailFetcher implements Runnable {
    private boolean isCancelled;
    private Context context;
    private List<Photo> photos;

    public BackgroundThumbnailFetcher(Context context, List<Photo> photos) {
      this.context = context;
      this.photos = photos;
    }

    public void cancel() {
      isCancelled = true;
    }

    @Override
    public void run() {
      Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
      for (Photo photo : photos) {
        if (isCancelled) {
          return;
        }

        // TODO: Calling asDrawable (or Bitmap/Gif) and then downloadOnly is weird.
        FutureTarget<File> futureTarget = Glide.with(context)
            .asDrawable()
            .load(photo)
            .downloadOnly(Api.SQUARE_THUMB_SIZE, Api.SQUARE_THUMB_SIZE);

        try {
          futureTarget.get();
        } catch (InterruptedException e) {
          if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Interrupted waiting for background downloadOnly", e);
          }
        } catch (ExecutionException e) {
          if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Got ExecutionException waiting for background downloadOnly", e);
          }
        }
        futureTarget.cancel(true /*mayInterruptIfRunning*/);
      }
    }
  }
}
