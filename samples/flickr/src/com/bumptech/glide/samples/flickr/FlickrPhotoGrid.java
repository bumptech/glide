package com.bumptech.glide.samples.flickr;

import android.content.Context;
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
import com.bumptech.glide.samples.flickr.api.Photo;
import com.bumptech.glide.loader.image.ImageManagerLoader;
import com.bumptech.glide.loader.transformation.CenterCrop;
import com.bumptech.glide.presenter.ImagePresenter;
import com.bumptech.glide.presenter.ImageReadyCallback;
import com.bumptech.glide.presenter.target.Target;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/10/13
 * Time: 9:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class FlickrPhotoGrid extends SherlockFragment implements PhotoViewer {
    private static final String IMAGE_SIZE_KEY = "image_size";

    private PhotoAdapter adapter;
    private List<Photo> currentPhotos;
    private int photoSize;

    public static FlickrPhotoGrid newInstance(int size) {
        FlickrPhotoGrid photoGrid = new FlickrPhotoGrid();
        Bundle args = new Bundle();
        args.putInt(IMAGE_SIZE_KEY, size);
        photoGrid.setArguments(args);
        return photoGrid;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();
        photoSize = args.getInt(IMAGE_SIZE_KEY);

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
            final Photo current = photos.get(position);
            final ImagePresenter<Photo> imagePresenter;
            if (view == null) {
                ImageView imageView = (ImageView) inflater.inflate(R.layout.flickr_photo_grid_item, container, false);
                ViewGroup.LayoutParams params = imageView.getLayoutParams();
                params.width = photoSize;
                params.height = photoSize;

                final Context context = getActivity();

                //this is an example of how one might use ImagePresenter directly, there is no particular reason
                //why ImagePresenter is used here and not in FlickrPhotoList otherwise.
                final Animation fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in);
                imagePresenter = new ImagePresenter.Builder<Photo>()
                        .setImageView(imageView)
                        .setModelLoader(new FlickrModelLoader(context))
                        .setImageLoader(new ImageManagerLoader(context))
                        .setTransformationLoader(new CenterCrop<Photo>())
                        .setImageReadyCallback(new ImageReadyCallback() {
                            @Override
                            public void onImageReady(Target target, boolean fromCache) {
                                if (!fromCache) {
                                    target.startAnimation(fadeIn);
                                }
                            }
                        })
                        .build();
                view = imageView;
                view.setTag(imagePresenter);
            } else {
                imagePresenter = (ImagePresenter<Photo>) view.getTag();
            }

            imagePresenter.setModel(current);
            return view;
        }
    }

}
