package com.bumptech.flickr;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.bumptech.flickr.api.Api;
import com.bumptech.flickr.api.Photo;
import com.bumptech.photos.resize.ImageManager;
import com.bumptech.photos.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MyActivity extends SherlockFragmentActivity {
    private Api flickerApi;
    private ImageManager imageManager;
    private File cacheDir;
    private int searchCount = 0;

    private List<Photo> currentPhotos = new ArrayList<Photo>(0);
    private List<PhotoViewer> photoViewers = new ArrayList<PhotoViewer>();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        String cacheName = "flickr_cache";
        cacheDir = ImageManager.getPhotoCacheDir(this, cacheName);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }

        ImageManager.Options options = new ImageManager.Options();
        options.maxPerSize = 40;
        options.maxDiskCacheSize = 50 * 1024 * 1024;
        imageManager = new ImageManager(this, options);

        flickerApi = new Api();

        final View searching = findViewById(R.id.searching);
        final TextView searchTerm = (TextView) findViewById(R.id.search_term);

        final EditText searchText = (EditText) findViewById(R.id.search_text);
        final Button search = (Button) findViewById(R.id.search);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

                        currentPhotos = photos;
                        for (PhotoViewer viewer : photoViewers) {
                            viewer.onPhotosUpdated(photos);
                        }
                    }
                });
            }
        });

        ViewPager pager = (ViewPager) findViewById(R.id.view_pager);

        final List<Fragment> fragments = new ArrayList<Fragment>();
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        final Resources res = getResources();

        GridFragment small = new GridFragment();
        small.setup(flickerApi, imageManager, cacheDir, res.getDimensionPixelSize(R.dimen.small_photo_side));
        fragments.add(small);
        photoViewers.add(small);

        final GridFragment medium = new GridFragment();
        medium.setup(flickerApi,  imageManager, cacheDir, res.getDimensionPixelSize(R.dimen.medium_photo_side));
        fragments.add(medium);
        photoViewers.add(medium);

        actionBar.addTab(actionBar.newTab().setText(R.string.small).setTabListener(new TabListener2(pager)));
        actionBar.addTab(actionBar.newTab().setText(R.string.medium).setTabListener(new TabListener2(pager)));

        pager.setAdapter(new FlickrPagerAdapter(getSupportFragmentManager(), fragments, new FlickrPagerAdapter.PrimaryItemListener() {
            @Override
            public void onPrimaryItemSet(int position) {
                actionBar.getTabAt(position).select();
            }
        }));

    }

    @Override
    protected void onResume() {
        super.onResume();
        imageManager.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        imageManager.pause();
    }

    private static class TabListener2 implements ActionBar.TabListener {
        private final ViewPager pager;

        public TabListener2(ViewPager pager) {
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
        private final PrimaryItemListener listener;

        private interface PrimaryItemListener {
            public void onPrimaryItemSet(int position);
        }
        private final List<Fragment> fragments;
        private int lastPosition = 0;

        public FlickrPagerAdapter(FragmentManager fm, List<Fragment> fragments, PrimaryItemListener listener) {
            super(fm);
            this.fragments = fragments;
            this.listener = listener;
        }

        @Override
        public Fragment getItem(int i) {
            return fragments.get(i);  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public int getCount() {
            return fragments.size();  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (lastPosition != position) {
                listener.onPrimaryItemSet(position);
                lastPosition = position;
            }
            super.setPrimaryItem(container, position, object);    //To change body of overridden methods use File | Settings | File Templates.
        }
    }
}
