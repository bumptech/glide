package com.bumptech.flickr;

import android.content.res.Resources;
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
import com.bumptech.photos.resize.ImageManager;
import com.bumptech.photos.resize.cache.DiskLruCacheWrapper;
import com.bumptech.photos.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FlickrSearchActivity extends SherlockFragmentActivity {
    private Api flickerApi;
    private ImageManager imageManager;
    private File cacheDir;
    private int searchCount = 0;

    private List<PhotoViewer> photoViewers = new ArrayList<PhotoViewer>();
    private EditText searchText;
    private View searching;
    private TextView searchTerm;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.flickr_search_activity);
        String cacheName = "flickr_cache";
        cacheDir = ImageManager.getPhotoCacheDir(this, cacheName);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }

        imageManager = new ImageManager.Builder(this)
                .setDiskCache(DiskLruCacheWrapper.get(ImageManager.getPhotoCacheDir(this), 50 * 1024 * 1024))
                .setMaxBitmapsPerSize(40)
                .build();

        final Resources res = getResources();
        flickerApi = new Api(res.getDimensionPixelSize(R.dimen.large_photo_side));

        searching = findViewById(R.id.searching);
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

        final List<Fragment> fragments = new ArrayList<Fragment>();
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        FlickrPhotoGrid small = new FlickrPhotoGrid();
        small.setup(flickerApi, imageManager, cacheDir, res.getDimensionPixelSize(R.dimen.small_photo_side));
        fragments.add(small);
        photoViewers.add(small);

        final FlickrPhotoGrid medium = new FlickrPhotoGrid();
        medium.setup(flickerApi,  imageManager, cacheDir, res.getDimensionPixelSize(R.dimen.medium_photo_side));
        fragments.add(medium);
        photoViewers.add(medium);

        FlickrPhotoList list =  new FlickrPhotoList();
        list.setup(flickerApi, imageManager, cacheDir);
        fragments.add(list);
        photoViewers.add(list);

        actionBar.addTab(actionBar.newTab().setText(R.string.small).setTabListener(new TabListener(pager)));
        actionBar.addTab(actionBar.newTab().setText(R.string.medium).setTabListener(new TabListener(pager)));
        actionBar.addTab(actionBar.newTab().setText(R.string.list).setTabListener(new TabListener(pager)));

        pager.setAdapter(new FlickrPagerAdapter(getSupportFragmentManager(), fragments));

    }

    private void executeSearch() {
        final String searchString = searchText.getText().toString();
        searchText.getText().clear();

        if ("".equals(searchString.trim())) return;

        final int currentSearch = ++searchCount;

        searching.setVisibility(View.VISIBLE);
        searchTerm.setText(getString(R.string.searching_for, searchString));

        flickerApi.search(searchString, new Api.SearchCallback() {
            @Override
            public void onSearchCompleted(List<Photo> photos) {
                if (currentSearch != searchCount) return;

                Log.d("SEARCH: completed, got " + photos.size() + " results");
                searching.setVisibility(View.INVISIBLE);

                for (PhotoViewer viewer : photoViewers) {
                    viewer.onPhotosUpdated(photos);
                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        imageManager.shutdown();
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

    private static class FlickrPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> fragments;

        public FlickrPagerAdapter(FragmentManager fm, List<Fragment> fragments){
            super(fm);
            this.fragments = fragments;
        }

        @Override
        public Fragment getItem(int i) {
            return fragments.get(i);  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public int getCount() {
            return fragments.size();  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
