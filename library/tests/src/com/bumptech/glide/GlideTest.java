package com.bumptech.glide;

import android.net.Uri;
import android.test.AndroidTestCase;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.presenter.ImagePresenter;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 7/25/13
 * Time: 12:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class GlideTest extends AndroidTestCase {
    private ImageView imageView;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        imageView = new ImageView(getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
    }

    private <T> void checkImagePresenter(T model) {
        Glide.load(model).into(imageView).begin();

        ImagePresenter imagePresenter = getImagePresenterFromView();
        imagePresenter.setModel(model);

        boolean caughtException = false;
        try {
            imagePresenter.setModel(new Integer(4));
        } catch (ClassCastException e) {
            caughtException = true;
        }

        assertTrue(caughtException);
    }

    private ImagePresenter getImagePresenterFromView() {
        return (ImagePresenter) imageView.getTag(R.id.image_presenter_id);
    }

    public void testFileDefaultLoader() {
        checkImagePresenter(new File("fake"));
    }

    public void testUrlDefaultLoader() throws MalformedURLException {
        checkImagePresenter(new URL("http://www.google.com"));
    }

    public void testUriDefaultLoader() {
        checkImagePresenter(Uri.fromFile(new File("Fake")));
    }

    public void testStringDefaultLoader() {
        checkImagePresenter("http://www.google.com");
    }

    public void testGlideDoesNotReplacePresenters() {
        Glide.load(new File("fake")).into(imageView).begin();

        ImagePresenter first = getImagePresenterFromView();

        Glide.load(new File("fake2")).into(imageView).begin();

        ImagePresenter second = getImagePresenterFromView();

        assertEquals(first, second);
    }

    public void testLoadingTwoDifferentTypesOfModelsThrows() {
        Glide.load(new File("fake")).into(imageView).begin();

        boolean thrown = false;
        try {
            Glide.load(new Integer(4)).into(imageView).begin();
        } catch (ClassCastException e) {
            thrown = true;
        }

        assertTrue(thrown);
    }
}
