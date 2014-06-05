package com.bumptech.glide.samples.flickr;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.samples.flickr.api.Photo;

import java.util.ArrayList;
import java.util.List;

public class FlickrPhotoList extends SherlockFragment implements PhotoViewer {
    private FlickrPhotoListAdapter adapter;
    private List<Photo> currentPhotos;
    private FlickrListPreloader preloader;

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
        ListView list = (ListView) result.findViewById(R.id.flickr_photo_list);
        adapter = new FlickrPhotoListAdapter();
        list.setAdapter(adapter);
        preloader = new FlickrListPreloader(getActivity(), 5);
        list.setOnScrollListener(preloader);
        if (currentPhotos != null) {
            adapter.setPhotos(currentPhotos);
        }
        return result;
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
        private final Context context;
        private int[] photoDimens = null;

        public FlickrListPreloader(Context context, int maxPreload) {
            super(maxPreload);
            this.context = context;
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
        protected BitmapRequestBuilder getRequestBuilder(Photo item) {
            return Glide.with(context)
                    .loadFromImage(item)
                    .thumbnail(0.1f)
                    .centerCrop();
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

            Glide.with(getActivity())
                    .loadFromImage(current)
                    .thumbnail(0.1f)
                    .centerCrop()
                    .animate(R.anim.fade_in)
                    .into(viewHolder.imageView);

            viewHolder.titleText.setText(current.title);
            return view;
        }
    }
}
