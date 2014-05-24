package com.bumptech.glide;

import android.content.Context;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.loader.bitmap.model.GenericLoaderFactory;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.model.ModelLoaderFactory;
import com.bumptech.glide.loader.bitmap.model.stream.StreamModelLoader;
import com.bumptech.glide.resize.target.ImageViewTarget;
import com.bumptech.glide.resize.target.Target;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Tests for the {@link Glide} interface and singleton.
 */
@RunWith(RobolectricTestRunner.class)
public class GlideTest {
    private ImageView imageView;
    private ImageViewTarget imageViewTarget;

    @Before
    public void setUp() throws Exception {
        imageView = new ImageView(getContext());
        //this is a quick hack to get the SizeDeterminer in ImagePresenter to think the view has been measured
        imageView.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
        imageViewTarget = new ImageViewTarget(imageView);
    }

    private Context getContext() {
        return Robolectric.application;
    }

    @Test
    public void testFileDefaultLoader() {
        File file = new File("fake");
        Target target = Glide.with(getContext()).load(file).into(imageViewTarget);
        assertNotNull(target.getRequest());
    }

    @Test
    public void testUrlDefaultLoader() throws MalformedURLException {
        URL url = new URL("http://www.google.com");
        Target target = Glide.with(getContext()).loadFromImage(url).into(imageViewTarget);
        assertNotNull(target.getRequest());
    }

    @Test
    public void testUriDefaultLoader() {
        Uri uri = Uri.fromFile(new File("Fake"));
        Target target = Glide.with(getContext()).load(uri).into(imageViewTarget);
        assertNotNull(target.getRequest());
    }

    @Test
    public void testStringDefaultLoader() {
        String string = "http://www.google.com";
        Target target = Glide.with(getContext()).load(string).into(imageViewTarget);
        assertNotNull(target.getRequest());
    }

    @Test
    public void testIntegerDefaultLoader() {
        int integer = 1234;
        Target target = Glide.with(getContext()).load(integer).into(imageViewTarget);
        assertNotNull(target.getRequest());
    }

    @Test
    public void testByteArrayDefaultLoader() {
        byte[] bytes = new byte[10];
        Target target = Glide.with(getContext()).loadFromImage(bytes).into(imageViewTarget);
        assertNotNull(target.getRequest());
    }

    @Test
    public void testByteArrayWithIdDefaultLoader() {
        byte[] bytes = new byte[10];
        String id = "test";
        Target target = Glide.with(getContext()).loadFromImage(bytes, id).into(imageViewTarget);
        assertNotNull(target.getRequest());
    }

    @Test(expected = Exception.class)
    public void testUnregisteredModelThrowsException() {
        Float unregistered = 0.5f;
        Glide.with(getContext()).load(unregistered).into(imageViewTarget);
    }

    @Test
    public void testUnregisteredModelWithGivenLoaderDoesNotThrow() {
        Float unregistered = 0.5f;
        StreamModelLoader<Float> mockLoader = mock(StreamModelLoader.class);
        Glide.with(getContext())
                .using(mockLoader)
                .load(unregistered)
                .into(imageViewTarget);
    }

    @Test
    public void testNonDefaultModelWithRegisteredFactoryDoesNotThrow() {
        Glide glide = Glide.get(getContext());
        glide.register(Float.class, InputStream.class, new ModelLoaderFactory<Float, InputStream>() {
            @Override
            public ModelLoader<Float, InputStream> build(Context context, GenericLoaderFactory factories) {
                return mock(ModelLoader.class);
            }

            @Override
            public void teardown() {
            }
        });
        Glide.with(getContext()).load(0.5f).into(imageViewTarget);
        glide.unregister(Float.class, InputStream.class);
    }

    @Test
    public void testNullModelDoesNotThrow() {
        String nullString = null;
        Glide.with(getContext()).load(nullString).into(imageViewTarget);
    }
}
