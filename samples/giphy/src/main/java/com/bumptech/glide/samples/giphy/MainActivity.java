package com.bumptech.glide.samples.giphy;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The primary activity in the Giphy sample that allows users to view trending animated GIFs from Giphy's api.
 */
public class MainActivity extends Activity implements Api.Monitor {
    private static final String TAG = "GiphyActivity";

    private GifAdapter adapter;
    private DrawableRequestBuilder<Api.GifResult> gifItemRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Glide.get(this).register(Api.GifResult.class, InputStream.class, new GiphyModelLoader.Factory());

        ImageView giphyLogoView = (ImageView) findViewById(R.id.giphy_logo_view);
        Glide.with(this)
                .load(R.raw.large_giphy_logo)
                .fitCenter()
                .into(giphyLogoView);

        ListView gifList = (ListView) findViewById(R.id.gif_list);
        GiphyPreloader preloader = new GiphyPreloader(2);

        gifItemRequest = Glide.with(this)
                .from(Api.GifResult.class)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .fitCenter();

        adapter = new GifAdapter(this, preloader, gifItemRequest);
        gifList.setAdapter(adapter);
        gifList.setOnScrollListener(preloader);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Api.get().addMonitor(this);
        if (adapter.getCount() == 0) {
            Api.get().getTrending();
        }
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
            return gifItemRequest.load(item);
        }
    }

    private static class GifAdapter extends BaseAdapter {
        private static final Api.GifResult[] EMPTY_RESULTS = new Api.GifResult[0];

        private final Activity activity;
        private final GiphyPreloader preloader;
        private DrawableRequestBuilder<Api.GifResult> requestBuilder;

        private Api.GifResult[] results = EMPTY_RESULTS;

        public GifAdapter(Activity activity, GiphyPreloader preloader,
                DrawableRequestBuilder<Api.GifResult> requestBuilder) {
            this.activity = activity;
            this.preloader = preloader;
            this.requestBuilder = requestBuilder;
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

            final Api.GifResult result = results[position];
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "load result: " + result);
            }
            final ImageView gifView = (ImageView) convertView.findViewById(R.id.gif_view);
            gifView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClipboardManager clipboard =
                            (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("giphy_url", result.images.fixed_height_downsampled.url);
                    clipboard.setPrimaryClip(clip);

                    Intent fullscreenIntent = FullscreenActivity.getIntent(activity, result);
                    activity.startActivity(fullscreenIntent);
                }
            });

            requestBuilder
                    .load(result)
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
