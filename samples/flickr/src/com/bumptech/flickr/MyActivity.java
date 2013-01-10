package com.bumptech.flickr;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.flickr.api.Api;
import com.bumptech.flickr.api.Photo;
import com.bumptech.photos.presenter.ImagePresenter;
import com.bumptech.photos.presenter.ImageSetCallback;
import com.bumptech.photos.resize.ImageManager;
import com.bumptech.photos.resize.loader.CenterCrop;
import com.bumptech.photos.util.Log;

import java.io.File;
import java.util.List;

public class MyActivity extends Activity {
    private Api flickerApi;
    private ImageManager imageManager;
    private File cacheDir;
    private int searchCount = 0;

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
        imageManager = new ImageManager(this, options);
        flickerApi = new Api();

        final GridView images = (GridView) findViewById(R.id.images);

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

                images.setAdapter(null);
                searching.setVisibility(View.VISIBLE);
                searchTerm.setText(getString(R.string.searching_for, searchString));

                flickerApi.search(searchString, new Api.SearchCallback() {
                    @Override
                    public void onSearchCompleted(List<Photo> photos) {
                        if (currentSearch != searchCount) return;

                        Log.d("SEARCH: completed, got " + photos.size() + " results");
                        searching.setVisibility(View.INVISIBLE);
                        images.setAdapter(new PhotoAdapter(photos));
                    }
                });
            }
        });
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

    private class PhotoAdapter extends BaseAdapter {

        private final List<Photo> photos;
        private final LayoutInflater inflater;

        public PhotoAdapter(List<Photo> photos) {
            this.photos = photos;
            this.inflater = getLayoutInflater();
        }

        @Override
        public int getCount() {
            return photos.size();
        }

        @Override
        public Object getItem(int i) {
            return photos.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int position, View view, ViewGroup container) {
            final ImagePresenter<Photo> presenter;
            if (view == null) {
                Log.d("MyActivity: inflate");
                ImageView imageView = (ImageView) inflater.inflate(R.layout.photo_grid_square, container, false);
                final Animation fadeIn = AnimationUtils.loadAnimation(MyActivity.this, R.anim.fade_in);
                presenter = new ImagePresenter.Builder<Photo>()
                        .setImageView(imageView)
                        .setPathLoader(new FlickPathLoader(flickerApi, cacheDir))
                        .setImageLoader(new CenterCrop<Photo>(imageManager))
                        .setImageSetCallback(new ImageSetCallback() {
                            @Override
                            public void onImageSet(ImageView view, boolean fromCache) {
                                view.clearAnimation();
                                if (!fromCache)
                                    view.startAnimation(fadeIn);
                            }
                        })
                        .build();
                imageView.setTag(presenter);
                view = imageView;
            } else {
                presenter = (ImagePresenter<Photo>) view.getTag();
            }

            presenter.setModel(photos.get(position));
            return view;
        }
    }
}
