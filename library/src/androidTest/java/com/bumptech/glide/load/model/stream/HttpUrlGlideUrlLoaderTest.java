package com.bumptech.glide.load.model.stream;

import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.HttpUrlFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.InputStream;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(JUnit4.class)
public class HttpUrlGlideUrlLoaderTest {
    private HttpUrlGlideUrlLoader loader;
    private GlideUrl model;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        loader = new HttpUrlGlideUrlLoader();
        model = mock(GlideUrl.class);
    }
    @Test
    public void testReturnsValidFetcher() {
        DataFetcher<InputStream> result = loader.getResourceFetcher(model, 100, 100);
        assertThat(result, instanceOf(HttpUrlFetcher.class));
    }
}
