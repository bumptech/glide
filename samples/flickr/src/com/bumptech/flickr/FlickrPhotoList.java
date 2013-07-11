package com.bumptech.flickr;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.bumptech.flickr.api.Api;
import com.bumptech.flickr.api.Photo;
import com.bumptech.glide.presenter.ImagePresenter;
import com.bumptech.glide.presenter.ImageSetCallback;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.loader.CenterCrop;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/10/13
 * Time: 12:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlickrPhotoList extends SherlockFragment implements PhotoViewer {
    private FlickrPhotoListAdapter adapter;
    private Api api;
    private ImageManager imageManager;
    private List<Photo> currentPhotos;

    public void setup(Api api, ImageManager imageManager) {
        this.api = api;
        this.imageManager = imageManager;
    }

    @Override
    public void onPhotosUpdated(List<Photo> photos) {
        currentPhotos = photos;
        if (adapter != null)
            adapter.setPhotos(currentPhotos);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View result = inflater.inflate(R.layout.flickr_photo_list, container, false);
        ListView list = (ListView) result.findViewById(R.id.flickr_photo_list);
        adapter = new FlickrPhotoListAdapter();
        list.setAdapter(adapter);
        if (currentPhotos != null)
            adapter.setPhotos(currentPhotos);
        return result;
    }

    private static class ViewHolder {
        private final ImagePresenter<Photo> presenter;
        private final TextView titleText;

        public ViewHolder(ImagePresenter<Photo> presenter, TextView titleText) {
            this.presenter = presenter;
            this.titleText = titleText;
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
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public long getItemId(int i) {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public View getView(int position, View view, ViewGroup container) {
            final ViewHolder viewHolder;
            if (view == null) {
                view = inflater.inflate(R.layout.flickr_photo_list_item, container, false);
                ImageView imageView = (ImageView) view.findViewById(R.id.photo_view);

                final Animation fadeIn = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
                ImagePresenter<Photo> presenter = new ImagePresenter.Builder<Photo>()
                        .setImageView(imageView)
                        .setModelStreamLoader(new DirectFlickrStreamLoader(api))
                        .setImageLoader(new CenterCrop(imageManager))
                        .setImageSetCallback(new ImageSetCallback() {
                            @Override
                            public void onImageSet(ImageView view, boolean fromCache) {
                                view.clearAnimation();
                                if (!fromCache)
                                    view.startAnimation(fadeIn);
                            }
                        })
                        .build();
                TextView titleView = (TextView) view.findViewById(R.id.title_view);
                viewHolder = new ViewHolder(presenter, titleView);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            final Photo current = photos.get(position);
            viewHolder.presenter.setModel(current);
            viewHolder.titleText.setText(current.title);
            return view;
        }
    }
}
