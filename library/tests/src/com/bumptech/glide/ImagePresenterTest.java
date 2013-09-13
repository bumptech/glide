package com.bumptech.glide;

import android.R;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.test.AndroidTestCase;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.loader.image.ImageLoader;
import com.bumptech.glide.loader.image.ImageManagerLoader;
import com.bumptech.glide.loader.model.ModelLoader;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.presenter.ImagePresenter;
import com.bumptech.glide.resize.load.Transformation;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 8/30/13
 * Time: 3:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImagePresenterTest extends AndroidTestCase {

    private ImageView imageView;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        imageView = new ImageView(getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
    }

    public void testCanSetErrorDrawable() {
        new ImagePresenter.Builder()
                .setErrorDrawable(new ColorDrawable(Color.RED));
    }

    public void testCanSetErrorResource() {
        new ImagePresenter.Builder()
                .setErrorResource(R.drawable.btn_star);
    }

    public void testCantSetErrorResourceAndDrawable() {
        Exception exception = null;
        try {
            new ImagePresenter.Builder()
                    .setErrorDrawable(new ColorDrawable(Color.RED))
                    .setErrorResource(R.drawable.btn_star);
        } catch (Exception e) {
            exception = e;
        }

        assertNotNull(exception);
        exception = null;
        try {
            new ImagePresenter.Builder()
                    .setErrorResource(R.drawable.btn_star)
                    .setErrorDrawable(new ColorDrawable(Color.RED));
        } catch (Exception e) {
            exception = e;
        }

        assertNotNull(exception);
    }

    public void testErrorPlaceholderIsSetOnException() {
        Drawable errorDrawable = new ColorDrawable(Color.RED);
        ImagePresenter<Object> imagePresenter = new ImagePresenter.Builder<Object>()
                .setImageView(imageView)
                .setModelLoader(new ModelLoader<Object>() {
                    @Override
                    public StreamLoader getStreamLoader(Object model, int width, int height) {
                        return new StreamLoader() {
                            @Override
                            public void loadStream(StreamReadyCallback cb) {
                            }

                            @Override
                            public void cancel() {
                            }
                        };
                    }

                    @Override
                    public String getId(Object model) {
                        return model.toString();
                    }
                })
                .setImageLoader(new ImageLoader() {
                    @Override
                    public Object fetchImage(String id, StreamLoader streamLoader, Transformation transformation, int width, int height, ImageReadyCallback cb) {
                        cb.onException(new Exception("Test"));
                        return null;
                    }

                    @Override
                    public void clear() { }
                })
                .setErrorDrawable(errorDrawable)
                .build();

        assertNull(imageView.getDrawable());
        imagePresenter.setModel(new Object());
        assertEquals(errorDrawable, imageView.getDrawable());
    }
}
