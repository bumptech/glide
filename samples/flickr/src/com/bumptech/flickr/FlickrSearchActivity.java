package com.bumptech.flickr;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.bumptech.flickr.api.Api;
import com.bumptech.flickr.api.Photo;
import com.bumptech.glide.Glide;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.cache.DiskCache;
import com.bumptech.glide.resize.cache.DiskCacheAdapter;
import com.bumptech.glide.resize.cache.DiskLruCacheWrapper;
import com.bumptech.glide.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlickrSearchActivity extends SherlockFragmentActivity {
    private int searchCount = 0;
    private EditText searchText;
    private View searching;
    private TextView searchTerm;
    private Set<PhotoViewer> photoViewers = new HashSet<PhotoViewer>();
    private File cacheDir;
    private List<Photo> currentPhotos = new ArrayList<Photo>();
    private View searchLoading;

    private enum Page {
        SMALL,
        MEDIUM,
        LIST
    }

    private static final Map<Page, Integer> PAGE_TO_TITLE = new HashMap<Page, Integer>() {{
        put(Page.SMALL, R.string.small);
        put(Page.MEDIUM, R.string.medium);
        put(Page.LIST, R.string.list);
    }};

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (!(fragment instanceof PhotoViewer)) {
            throw new IllegalStateException("Fragment class " + fragment.getClass() + " does not implement PhotoViewer");
        } else {
            PhotoViewer photoViewer = (PhotoViewer) fragment;
            photoViewer.onPhotosUpdated(currentPhotos);
            photoViewers.add(photoViewer);
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.flickr_search_activity);
        String cacheName = "flickr_cache";
        cacheDir = ImageManager.getPhotoCacheDir(this, cacheName);

        DiskCache diskCache;
        try {
            diskCache = DiskLruCacheWrapper.get(cacheDir, 50 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
            diskCache = new DiskCacheAdapter();
        }

        final Glide glide = Glide.get();
        if (!glide.isImageManagerSet()) {
            glide.setImageManager(new ImageManager.Builder(this)
                    .setBitmapCompressQuality(70)
                    .setDiskCache(diskCache));

        }

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

        ViewPager pager = (ViewPager) findViewById(R.id.view_pager);
        pager.setPageMargin(50);
        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) { }

            @Override
            public void onPageSelected(int position) {
                getSupportActionBar().getTabAt(position).select();
            }

            @Override
            public void onPageScrollStateChanged(int i) { }
        });


        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        for (Page page : Page.values()) {
            final int textId = PAGE_TO_TITLE.get(page);
            actionBar.addTab(actionBar.newTab().setText(textId).setTabListener(new TabListener(pager)));
        }

        pager.setAdapter(new FlickrPagerAdapter(getSupportFragmentManager()));
    }

    private void executeSearch() {
        final String searchString = searchText.getText().toString();
        searchText.getText().clear();

        if ("".equals(searchString.trim())) return;

        final int currentSearch = ++searchCount;

        searching.setVisibility(View.VISIBLE);
        searchLoading.setVisibility(View.VISIBLE);
        searchTerm.setText(getString(R.string.searching_for, searchString));

        Api.get(this).search(searchString, new Api.SearchCallback() {
            @Override
            public void onSearchCompleted(List<Photo> photos) {
                if (currentSearch != searchCount) return;

                Log.d("SEARCH: completed, got " + photos.size() + " results");
                searching.setVisibility(View.INVISIBLE);

                for (PhotoViewer viewer : photoViewers) {
                    viewer.onPhotosUpdated(photos);
                }

                currentPhotos = photos;
            }

            @Override
            public void onSearchFailed(Exception e) {
                if (currentSearch != searchCount) return;
                e.printStackTrace();

                Log.d("SEARCH: failed :(");
                searching.setVisibility(View.VISIBLE);
                searchLoading.setVisibility(View.INVISIBLE);
                searchTerm.setText(getString(R.string.search_failed, searchString));
            }
        });
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
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) { }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) { }
    }

    private class FlickrPagerAdapter extends FragmentPagerAdapter {

        public FlickrPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return pageToFragment(position);
        }

        @Override
        public int getCount() {
            return Page.values().length;
        }

        private Fragment pageToFragment(int position) {
            Page page = Page.values()[position];
            if (page == Page.SMALL) {
                int pageSize = getPageSize(R.dimen.small_photo_side);
                return FlickrPhotoGrid.newInstance(cacheDir, pageSize);

            } else if (page == Page.MEDIUM) {
                int pageSize = getPageSize(R.dimen.medium_photo_side);
                return FlickrPhotoGrid.newInstance(cacheDir, pageSize);
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
}
