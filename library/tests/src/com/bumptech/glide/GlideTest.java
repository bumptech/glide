package com.bumptech.glide;

import android.net.Uri;
import android.test.ActivityTestCase;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.loader.model.ModelLoader;
import com.bumptech.glide.loader.stream.StreamLoader;
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
public class GlideTest extends ActivityTestCase {
    private ImageView imageView;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        imageView = new ImageView(getInstrumentation().getContext());
        //this is a quick hack to get the SizeDeterminer in ImagePresenter to think the view has been measured
        imageView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
    }

    private void checkImagePresenter(Object model) {
        ImagePresenter imagePresenter = getImagePresenterFromView();
        imagePresenter.setModel(model);

        boolean caughtException = false;
        try {
            imagePresenter.setModel(new Float(4.4f));
        } catch (ClassCastException e) {
            caughtException = true;
        }

        assertTrue(caughtException);
    }

    private ImagePresenter getImagePresenterFromView() {
        return (ImagePresenter) imageView.getTag();
    }

    public void testFileDefaultLoader() {
        File file = new File("fake");
        Glide.load(file).into(imageView);
        checkImagePresenter(file);
    }

    public void testUrlDefaultLoader() throws MalformedURLException {
        URL url = new URL("http://www.google.com");
        Glide.load(url).into(imageView);
        checkImagePresenter(url);
    }

    public void testUriDefaultLoader() {
        Uri uri = Uri.fromFile(new File("Fake"));
        Glide.load(uri).into(imageView);
        checkImagePresenter(uri);
    }

    public void testStringDefaultLoader() {
        String string = "http://www.google.com";
        Glide.load(string).into(imageView);
        checkImagePresenter(string);
    }

    public void testIntegerDefaultLoader() {
        int integer = 1234;
        Glide.load(integer).into(imageView);
        checkImagePresenter(integer);
    }

    public void testGlideDoesNotReplaceIdenticalPresenters() {
        Glide.load("fake")
                .centerCrop()
                .animate(android.R.anim.fade_in)
                .placeholder(com.bumptech.glide.tests.R.raw.ic_launcher)
                .error(com.bumptech.glide.tests.R.raw.ic_launcher)
                .into(imageView);
        ImagePresenter first = getImagePresenterFromView();

        Glide.load("fake2")
                .centerCrop()
                .animate(android.R.anim.fade_in)
                .placeholder(com.bumptech.glide.tests.R.raw.ic_launcher)
                .error(com.bumptech.glide.tests.R.raw.ic_launcher)
                .into(imageView);
        ImagePresenter second = getImagePresenterFromView();

        assertSame(first, second);
    }

    public void testCanHandleWrapContent() {
        imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        Glide.load("fake").into(imageView);

        assertNotNull(getImagePresenterFromView());
    }

    public void testDifferentModlsReplacesPresenters() {
        Glide.load("fake").into(imageView);

        ImagePresenter first = getImagePresenterFromView();
        Glide.load(4).into(imageView);
        ImagePresenter second = getImagePresenterFromView();

        assertNotSame(first, second);
    }

    public void testDifferentModelLoadersReplacesPresenter() {
        Glide.using(new ModelLoader<Object>() {
            @Override
            public StreamLoader getStreamLoader(Object model, int width, int height) {
                return new StreamLoader() {
                    @Override
                    public void loadStream(Object t, StreamReadyCallback cb) {
                    }

                    @Override
                    public void cancel() {
                    }
                };
            }

            @Override
            public String getId(Object model) {
                return String.valueOf(model.hashCode());
            }

        }).load(new Object()).into(imageView);

        ImagePresenter first = getImagePresenterFromView();

        Glide.using(new ModelLoader<Object>() {
            @Override
            public StreamLoader getStreamLoader(Object model, int width, int height) {
                return new StreamLoader() {
                    @Override
                    public void loadStream(Object object, StreamReadyCallback cb) {
                    }

                    @Override
                    public void cancel() {
                    }
                };
            }

            @Override
            public String getId(Object model) {
                return String.valueOf(model.hashCode());
            }

        }).load(new Object()).into(imageView);

        ImagePresenter second = getImagePresenterFromView();

        assertNotSame(first, second);
    }

    public void testDifferentImageLoadersReplacesPresenter() {
        final File file = new File("fake");
        Glide.load(file).centerCrop().into(imageView);
        ImagePresenter first = getImagePresenterFromView();

        Glide.load(file).into(imageView);
        ImagePresenter second = getImagePresenterFromView();

        assertNotSame(first, second);
    }

    public void testDifferentPlaceholdersReplacesPresenter() {
        final File file = new File("fake");
        Glide.load(file).placeholder(com.bumptech.glide.tests.R.raw.ic_launcher).into(imageView);
        ImagePresenter first = getImagePresenterFromView();

        Glide.load(file).into(imageView);
        ImagePresenter second = getImagePresenterFromView();

        assertNotSame(first, second);
    }

    public void testDifferentAnimationsReplacesPresenter() {
        final File file = new File("fake");
        Glide.load(file).animate(android.R.anim.fade_in).into(imageView);
        ImagePresenter first = getImagePresenterFromView();

        Glide.load(file).animate(android.R.anim.fade_out).into(imageView);
        ImagePresenter second = getImagePresenterFromView();

        assertNotSame(first, second);
    }

    public void testDifferentErrorIdsReplacesPresenter() {
        final File file = new File("fake");
        Glide.load(file).error(com.bumptech.glide.tests.R.raw.ic_launcher).into(imageView);
        ImagePresenter first = getImagePresenterFromView();

        Glide.load(file).error(android.R.drawable.btn_star).into(imageView);
        ImagePresenter second = getImagePresenterFromView();

        assertNotSame(first, second);
    }

}
