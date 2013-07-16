package com.bumptech.flickr;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import com.actionbarsherlock.app.SherlockFragment;
import com.bumptech.flickr.api.Api;
import com.bumptech.flickr.api.Photo;
import com.bumptech.glide.Glide;
import com.bumptech.glide.presenter.ImagePresenter;
import com.bumptech.glide.presenter.ImageSetCallback;
import com.bumptech.glide.resize.loader.CenterCrop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/10/13
 * Time: 9:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class FlickrPhotoGrid extends SherlockFragment implements PhotoViewer{
    private PhotoAdapter adapter;
    private List<Photo> currentPhotos;
    private Api api;
    private File cacheDir;
    private int photoSize;

    public void setup(Api api, File cacheDir, int photoSize) {
        this.api = api;
        this.cacheDir = cacheDir;
        this.photoSize = photoSize;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View result = inflater.inflate(R.layout.flickr_photo_grid, container, false);
        GridView grid = (GridView) result.findViewById(R.id.images);
        grid.setColumnWidth(photoSize);
        adapter = new PhotoAdapter();
        grid.setAdapter(adapter);
        if (currentPhotos != null)
            adapter.setPhotos(currentPhotos);

        return result;
    }

    @Override
    public void onPhotosUpdated(List<Photo> photos) {
        currentPhotos = photos;
        if (adapter != null)
            adapter.setPhotos(currentPhotos);
    }

    private class PhotoAdapter extends BaseAdapter {

        private List<Photo> photos = new ArrayList<Photo>(0);
        private final LayoutInflater inflater;

        public PhotoAdapter() {
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
                ImageView imageView = (ImageView) inflater.inflate(R.layout.flickr_photo_grid_item, container, false);
                ViewGroup.LayoutParams params = imageView.getLayoutParams();
                params.width = photoSize;
                params.height = photoSize;

                final Animation fadeIn = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
                presenter = new ImagePresenter.Builder<Photo>()
                        .setImageView(imageView)
                        .setModelStreamLoader(new FlickrStreamLoader(api, cacheDir))
                        .setImageLoader(new CenterCrop(Glide.get().getImageManager(getActivity())))
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
