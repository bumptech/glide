package com.bumptech.glide.samples.flickr;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.samples.flickr.api.Api;
import com.bumptech.glide.samples.flickr.api.Photo;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment that shows cropped image thumbnails half the width of the screen in a scrolling list.
 */
public class FlickrPhotoList extends Fragment implements PhotoViewer {
    private static final String STATE_POSITION_INDEX = "state_position_index";
    private static final String STATE_POSITION_OFFSET = "state_position_offset";
    private FlickrPhotoListAdapter adapter;
    private List<Photo> currentPhotos;
    private FlickrListPreloader preloader;
    private ListView list;
    private DrawableRequestBuilder<Photo> fullRequest;
    private DrawableRequestBuilder<Photo> thumbRequest;
    private DrawableRequestBuilder<Photo> preloadRequest;

    public static FlickrPhotoList newInstance() {
        return new FlickrPhotoList();
    }

    @Override
    public void onPhotosUpdated(List<Photo> photos) {
        currentPhotos = photos;
        if (adapter != null) {
            adapter.setPhotos(currentPhotos);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View result = inflater.inflate(R.layout.flickr_photo_list, container, false);
        list = (ListView) result.findViewById(R.id.flickr_photo_list);
        adapter = new FlickrPhotoListAdapter();
        list.setAdapter(adapter);
        preloader = new FlickrListPreloader(5);
        list.setOnScrollListener(preloader);
        if (currentPhotos != null) {
            adapter.setPhotos(currentPhotos);
        }

        fullRequest = Glide.with(FlickrPhotoList.this)
                .from(Photo.class)
                .placeholder(new ColorDrawable(Color.GRAY))
                .centerCrop();

        preloadRequest = Glide.with(FlickrPhotoList.this)
                .from(Photo.class)
                .placeholder(new ColorDrawable(Color.GRAY))
                .centerCrop()
                .priority(Priority.HIGH);

        thumbRequest = Glide.with(FlickrPhotoList.this)
                .from(Photo.class)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .override(Api.SQUARE_THUMB_SIZE, Api.SQUARE_THUMB_SIZE);


        if (savedInstanceState != null) {
            int index = savedInstanceState.getInt(STATE_POSITION_INDEX);
            int offset = savedInstanceState.getInt(STATE_POSITION_OFFSET);
            list.setSelectionFromTop(index, offset);
        }

        return result;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (list != null) {
            int index = list.getFirstVisiblePosition();
            View topView = list.getChildAt(0);
            int offset = topView != null ? topView.getTop() : 0;
            outState.putInt(STATE_POSITION_INDEX, index);
            outState.putInt(STATE_POSITION_OFFSET, offset);
        }
    }

    private static class ViewHolder {
        private final TextView titleText;
        private final ImageView imageView;

        public ViewHolder(ImageView imageView, TextView titleText) {
            this.imageView = imageView;
            this.titleText = titleText;
        }
    }

    private class FlickrListPreloader extends ListPreloader<Photo> {
        private int[] photoDimens = null;

        public FlickrListPreloader(int maxPreload) {
            super(maxPreload);
        }

        public boolean isDimensSet() {
            return photoDimens != null;
        }

        public void setDimens(int width, int height) {
            if (photoDimens == null) {
                photoDimens = new int[] { width, height };
            }
        }

        @Override
        protected int[] getDimensions(Photo item) {
            return photoDimens;
        }

        @Override
        protected List<Photo> getItems(int start, int end) {
            return currentPhotos.subList(start, end);
        }

        @Override
        protected GenericRequestBuilder getRequestBuilder(Photo item) {
            return preloadRequest
                    .thumbnail(thumbRequest.load(item))
                    .load(item);
        }
    }

    private class FlickrPhotoListAdapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private List<Photo> photos = new ArrayList<Photo>(0);

        public FlickrPhotoListAdapter() {
            this.inflater = LayoutInflater.from(getActivity());
        }

        public void setPhotos(List<Photo> photos) {
            this.photos = photos;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return photos.size();
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
        public View getView(int position, View view, ViewGroup container) {
            final Photo current = photos.get(position);
            final ViewHolder viewHolder;
            if (view == null) {
                view = inflater.inflate(R.layout.flickr_photo_list_item, container, false);
                final ImageView imageView = (ImageView) view.findViewById(R.id.photo_view);
                TextView titleView = (TextView) view.findViewById(R.id.title_view);
                viewHolder = new ViewHolder(imageView, titleView);
                view.setTag(viewHolder);
                if (!preloader.isDimensSet()) {
                    imageView.post(new Runnable() {
                        @Override
                        public void run() {
                            preloader.setDimens(imageView.getWidth(), imageView.getHeight());
                        }
                    });
                }
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            fullRequest
                    .thumbnail(thumbRequest.load(current))
                    .load(current)
                    .into(viewHolder.imageView);

            viewHolder.titleText.setText(current.getTitle());
            return view;
        }
    }
}
