package com.bumptech.glide.presenter.target;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import com.bumptech.glide.presenter.ImagePresenter;

import static android.view.ViewGroup.LayoutParams;

/**
 * A target wrapping an ImageView that makes use of {@link ImageView#setTag(Object)} to store and retrieve
 * ImagePresenters. Also obtains the runtime dimensions of the ImageView.
 */
public class ImageViewTarget implements Target {
    private final ImageView imageView;
    private final SizeDeterminer sizeDeterminer;

    public ImageViewTarget(ImageView imageView) {
        this.imageView = imageView;
        this.sizeDeterminer = new SizeDeterminer(imageView);
    }

    @Override
    public void onImageReady(Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);
    }

    @Override
    public void setPlaceholder(Drawable placeholder) {
        imageView.setImageDrawable(placeholder);
    }

    @Override
    public void getSize(SizeReadyCallback cb) {
        sizeDeterminer.getSize(cb);
    }

    @Override
    public void startAnimation(Animation animation) {
        imageView.clearAnimation();

        imageView.startAnimation(animation);
    }

    /**
     * TODO: use {@link View#setTag(int, Object)} when we can do so and still create
     * a jar
     */
    @Override
    public void setImagePresenter(ImagePresenter imagePresenter) {
        imageView.setTag(imagePresenter);
    }

    @Override
    public ImagePresenter getImagePresenter() {
        Object tag = imageView.getTag();
        ImagePresenter result = null;
        if (tag instanceof ImagePresenter) {
            result = (ImagePresenter) tag;
        }
        return result;
    }

    @Override
    public int hashCode() {
        return imageView.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (!(o instanceof ImageViewTarget)) {
            return false;
        }
        ImageViewTarget other = (ImageViewTarget) o;
        return imageView.equals(other.imageView);
    }

    private static class SizeDeterminer {
        private static final String PENDING_SIZE_CHANGE_TOKEN = "pending_load";
        private static final int PENDING_SIZE_CHANGE_DELAY = 100; //60 fps = 1000/60 = 16.67 ms

        private final View view;
        private int[] dimens = new int[2];
        private boolean valid = false;
        private SizeReadyCallback cb = null;
        private Handler handler = new Handler();
        private final Runnable getDimens = new Runnable() {
            @Override
            public void run() {
                if (cb == null) return;

                LayoutParams layoutParams = view.getLayoutParams();
                if (layoutParams.width > 0 && layoutParams.height > 0) {
                    cb.onSizeReady(layoutParams.width, layoutParams.height);
                } else if (view.getWidth() > 0 && view.getHeight() > 0) {
                    valid = true;
                    dimens[0] = view.getWidth();
                    dimens[1] = view.getHeight();
                    cb.onSizeReady(dimens[0], dimens[1]);
                }
                cb = null;
            }
        };

        public SizeDeterminer(View view) {
            this.view = view;
        }

        public void getSize(SizeReadyCallback cb) {
            handler.removeCallbacksAndMessages(PENDING_SIZE_CHANGE_TOKEN);
            this.cb = null;
            LayoutParams layoutParams = view.getLayoutParams();
            //non null layout params and either width and height have been set, or set to wrap content so they
            //will not be set until we set some content
            if (isHandled(layoutParams)) {
                cb.onSizeReady(layoutParams.width, layoutParams.height);
            } else if (valid) {
                cb.onSizeReady(dimens[0], dimens[1]);
            } else {
                this.cb = cb;
                handler.postDelayed(getDimens, PENDING_SIZE_CHANGE_DELAY);
            }
        }

        private boolean isHandled(LayoutParams layoutParams) {
            return layoutParams != null && ((layoutParams.width > 0 && layoutParams.height > 0) ||
                    (layoutParams.width == LayoutParams.WRAP_CONTENT || layoutParams.height == LayoutParams.WRAP_CONTENT));
        }

    }
}
