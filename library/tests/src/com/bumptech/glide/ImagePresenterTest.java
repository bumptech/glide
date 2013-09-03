package com.bumptech.glide;

import android.R;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.test.AndroidTestCase;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.presenter.ImagePresenter;

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
}
