package com.bumptech.glide.load.model.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelCache;
import com.bumptech.glide.load.model.ModelLoader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class BaseGlideUrlLoaderTest {

    private ModelCache<Object, GlideUrl> modelCache;
    private ModelLoader<GlideUrl, InputStream> wrapped;
    private TestLoader urlLoader;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        modelCache = mock(ModelCache.class);
        wrapped = mock(ModelLoader.class);
        urlLoader = new TestLoader(wrapped, modelCache);
    }

    @Test
    public void testReturnsNullIfUrlIsNull() {
        urlLoader.resultUrl = null;
        assertNull(urlLoader.getResourceFetcher(new Object(), 100, 100));
    }

    @Test
    public void testReturnsNullIfUrlIsEmpty() {
        urlLoader.resultUrl = "    ";
        assertNull(urlLoader.getResourceFetcher(new Object(), 100, 100));
    }

    @Test
    public void testReturnsUrlFromCacheIfPresent() {
        Object model = new Object();
        int width = 100;
        int height = 200;
        GlideUrl expectedUrl = mock(GlideUrl.class);
        when(modelCache.get(eq(model), eq(width), eq(height))).thenReturn(expectedUrl);
        DataFetcher<InputStream> expectedFetcher = mock(DataFetcher.class);

        when(wrapped.getResourceFetcher(eq(expectedUrl), eq(width), eq(height))).thenReturn(expectedFetcher);

        assertEquals(expectedFetcher, urlLoader.getResourceFetcher(model, width, height));
    }

    @Test
    public void testBuildsNewUrlIfNotPresentInCache() {
        int width = 10;
        int height = 11;

        urlLoader.resultUrl = "fakeUrl";
        final DataFetcher<InputStream> expected = mock(DataFetcher.class);
        when(wrapped.getResourceFetcher(any(GlideUrl.class), eq(width), eq(height))).thenAnswer(
                new Answer<DataFetcher<InputStream>>() {
                    @Override
                    public DataFetcher<InputStream> answer(InvocationOnMock invocationOnMock) throws Throwable {
                        GlideUrl glideUrl = (GlideUrl) invocationOnMock.getArguments()[0];
                        assertEquals(urlLoader.resultUrl, glideUrl.toStringUrl());
                        return expected;

                    }
                });
        assertEquals(expected, urlLoader.getResourceFetcher(new Object(), width, height));
    }

    @Test
    public void testAddsNewUrlToCacheIfNotPresentInCache() {
        urlLoader.resultUrl = "fakeUrl";
        Object model = new Object();
        int width = 400;
        int height = 500;

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                GlideUrl glideUrl = (GlideUrl) invocationOnMock.getArguments()[3];
                assertEquals(urlLoader.resultUrl, glideUrl.toStringUrl());
                return null;
            }
        }).when(modelCache).put(eq(model), eq(width), eq(height), any(GlideUrl.class));

        urlLoader.getResourceFetcher(model, width, height);

        verify(modelCache).put(eq(model), eq(width), eq(height), any(GlideUrl.class));
    }

    @Test
    public void testDoesNotInteractWithModelCacheIfNull() {
        TestLoader urlLoader = new TestLoader(wrapped, null);
        urlLoader.resultUrl = "fakeUrl";

        int width = 456;
        int height = 789;

        DataFetcher<InputStream> expected = mock(DataFetcher.class);
        when(wrapped.getResourceFetcher(any(GlideUrl.class), eq(width), eq(height))).thenReturn(expected);

        assertEquals(expected, urlLoader.getResourceFetcher(new Object(), width, height));
    }

    private class TestLoader extends BaseGlideUrlLoader<Object> {
        public String resultUrl;

        public TestLoader(ModelLoader<GlideUrl, InputStream> concreteLoader, ModelCache<Object, GlideUrl> modelCache) {
            super(concreteLoader, modelCache);
        }

        @Override
        protected String getUrl(Object model, int width, int height) {
            return resultUrl;
        }
    }
}