package com.bumptech.glide.samples.giphy;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.bumptech.glide.Glide;

import java.io.InputStream;


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
        adapter = new GifAdapter(this);
        gifList.setAdapter(adapter);
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

    private static class GifAdapter extends BaseAdapter {
        private static final Api.GifResult[] EMPTY_RESULTS = new Api.GifResult[0];

        private Api.GifResult[] results = EMPTY_RESULTS;
        private Activity activity;

        public GifAdapter(Activity activity) {
            this.activity = activity;
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
        public Object getItem(int i) {
            return null;
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
            ImageView gifView = (ImageView) convertView.findViewById(R.id.gif_view);

            Glide.with(activity)
                    .load(result)
                    .fitCenter()
                    .into(gifView);

            return convertView;
        }
    }
}
