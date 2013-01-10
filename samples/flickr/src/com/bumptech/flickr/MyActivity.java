package com.bumptech.flickr;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
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
                        getSupportActionBar().getSelectedTab().select(); //reselect
                    }
                });
            }
        });

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        final GridFragment medium = new GridFragment();
        final Resources res = getResources();
        medium.setup(flickerApi,  imageManager, cacheDir, res.getDimensionPixelSize(R.dimen.medium_photo_side));
        GridFragment small = new GridFragment();
        small.setup(flickerApi, imageManager, cacheDir, res.getDimensionPixelSize(R.dimen.small_photo_side));
        actionBar.addTab(actionBar.newTab().setText(R.string.small).setTabListener(new TabListener<GridFragment>("small", small) {
            @Override
            protected void refreshFragment(GridFragment fragment) {
                if (currentPhotos != null)
                    fragment.setPhotos(currentPhotos);
            }
        }));
        actionBar.addTab(actionBar.newTab().setText(R.string.medium).setTabListener(new TabListener<GridFragment>("medium", medium) {
            @Override
            protected void refreshFragment(GridFragment fragment) {
                if (currentPhotos != null)
                    fragment.setPhotos(currentPhotos);
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

    private abstract class TabListener<T extends SherlockFragment> implements ActionBar.TabListener {
        private final String tag;
        private final T fragment;
        private boolean added = false;

        public TabListener(String tag, T fragment) {
            this.tag = tag;
            this.fragment = fragment;
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            if (!added) {
                ft.add(R.id.fragment_container, fragment, tag);
                added = true;
            } else {
                ft.attach(fragment);
            }
            refreshFragment(fragment);
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            if (fragment != null) {
                ft.detach(fragment);
            }
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
            refreshFragment(fragment);
        }

        protected abstract void refreshFragment(T fragment);
    }
}
