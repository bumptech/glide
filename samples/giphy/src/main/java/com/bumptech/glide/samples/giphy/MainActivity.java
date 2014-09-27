package com.bumptech.glide.samples.giphy;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * The primary activity in the Giphy sample that allows users to view trending animated GIFs from Giphy's api.
 */
public class MainActivity extends Activity implements Api.Monitor {
    private static final String TAG = "GiphyActivity";

    private GifAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Glide.get(this).register(Api.GifResult.class, InputStream.class, new GiphyModelLoader.Factory());
        Api.get().getTrending();

        ImageView giphyLogoView = (ImageView) findViewById(R.id.giphy_logo_view);
        Glide.with(this)
                .load(R.raw.large_giphy_logo)
                .fitCenter()
                .into(giphyLogoView);

        ListView gifList = (ListView) findViewById(R.id.gif_list);
        GiphyPreloader preloader = new GiphyPreloader(2);

        adapter = new GifAdapter(this, preloader);
        gifList.setAdapter(adapter);
        gifList.setOnScrollListener(preloader);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Api.get().addMonitor(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Api.get().removeMonitor(this);
    }

    @Override
    public void onSearchComplete(Api.SearchResult result) {
        adapter.setResults(result.data);
    }

    private class GiphyPreloader extends ListPreloader<Api.GifResult> {

        private int[] dimensions;

        public GiphyPreloader(int maxPreload) {
            super(maxPreload);
        }

        @Override
        protected int[] getDimensions(Api.GifResult item) {
            return dimensions;
        }

        @Override
        protected List<Api.GifResult> getItems(int start, int end) {
            List<Api.GifResult> items = new ArrayList<Api.GifResult>(end - start);
            for (int i = start; i < end; i++) {
                items.add(adapter.getItem(i));
            }
            return items;
        }

        @Override
        protected GenericRequestBuilder getRequestBuilder(Api.GifResult item) {
            return Glide.with(MainActivity.this)
                    .load(item)
                    .fitCenter();
        }
    }

    private static class GifAdapter extends BaseAdapter {
        private static final Api.GifResult[] EMPTY_RESULTS = new Api.GifResult[0];

        private final Activity activity;
        private final GiphyPreloader preloader;

        private Api.GifResult[] results = EMPTY_RESULTS;

        public GifAdapter(Activity activity, GiphyPreloader preloader) {
            this.activity = activity;
            this.preloader = preloader;
        }

        public void setResults(Api.GifResult[] results) {
            if (results != null) {
                this.results = results;
            } else {
                this.results = EMPTY_RESULTS;
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return results.length;
        }

        @Override
        public Api.GifResult getItem(int i) {
            return results[i];
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = activity.getLayoutInflater().inflate(R.layout.gif_list_item, parent, false);
            }

            Api.GifResult result = results[position];
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "load result: " + result);
            }
            final ImageView gifView = (ImageView) convertView.findViewById(R.id.gif_view);

            Glide.with(activity)
                    .load(result)
                    .fitCenter()
                    .into(gifView);

            if (preloader.dimensions == null) {
                gifView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (gifView.getWidth() > 0 && gifView.getHeight() > 0) {
                            preloader.dimensions = new int[2];
                            preloader.dimensions[0] = gifView.getWidth();
                            preloader.dimensions[1] = gifView.getHeight();
                        }
                    }
                });
            }

            return convertView;
        }
    }
}
