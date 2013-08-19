package com.bumptech.glide;

import android.net.Uri;
import android.test.ActivityTestCase;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.loader.model.ModelLoader;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.presenter.ImagePresenter;
import com.bumptech.glide.tests.R;

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
                .asIs()
                .centerCrop()
                .animate(android.R.anim.fade_in)
                .placeholder(com.bumptech.glide.tests.R.raw.ic_launcher)
                .error(com.bumptech.glide.tests.R.raw.ic_launcher)
                .into(imageView);
        ImagePresenter first = getImagePresenterFromView();

        Glide.load("fake2")
                .asIs()
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

    public void testDifferentModelTypesReplacesPresenters() {
        assertDifferentPresenters(
                Glide.load(4),
                Glide.load("fake")
        );
    }

    public void testDifferentModelLoadersReplacesPresenter() {
        ModelLoader<Object> first = new ModelLoader<Object>() {
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

        };

        ModelLoader<Object> second = new ModelLoader<Object>() {
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
        };

        final Object object = new Object();
        assertDifferentPresenters(
                Glide.using(first).load(object),
                Glide.using(second).load(object)
        );
    }

    public void testDifferentDownsamplersReplacesPresenter() {
        assertDifferentPresenters(
                Glide.load("a").approximate(),
                Glide.load("a").asIs()
        );
    }

    public void testDifferentTransformationsReplacesPresenter() {
        final File file = new File("fake");
        assertDifferentPresenters(
                Glide.load(file).centerCrop().fitCenter(),
                Glide.load(file).centerCrop()
        );
    }

    public void testDifferentPlaceholdersReplacesPresenter() {
        final File file = new File("fake");
        assertDifferentPresenters(
                Glide.load(file).placeholder(com.bumptech.glide.tests.R.raw.ic_launcher),
                Glide.load(file)

        );
    }

    public void testDifferentAnimationsReplacesPresenter() {
        final File file = new File("fake");
        assertDifferentPresenters(
                Glide.load(file).animate(android.R.anim.fade_in),
                Glide.load(file).animate(android.R.anim.fade_out)
        );
    }

    public void testDifferentErrorIdsReplacesPresenter() {
        assertDifferentPresenters(
                Glide.load("b").error(R.raw.ic_launcher),
                Glide.load("b").error(android.R.drawable.btn_star)
        );
    }

    private void assertDifferentPresenters(Glide.Request a, Glide.Request b) {
        a.into(imageView);
        ImagePresenter first = getImagePresenterFromView();

        a.into(imageView);
        ImagePresenter second = getImagePresenterFromView();

        b.into(imageView);
        ImagePresenter third = getImagePresenterFromView();

        assertSame(first, second);
        assertNotSame(first, third);
    }

}
