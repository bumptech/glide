package com.bumptech.glide.load.model.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelCache;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.util.Preconditions;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class BaseGlideUrlLoaderTest {

  @Mock private ModelCache<Object, GlideUrl> modelCache;
  @Mock private ModelLoader<GlideUrl, InputStream> wrapped;
  @Mock private DataFetcher<InputStream> fetcher;
  private TestLoader urlLoader;
  private Options options;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    options = new Options();
    urlLoader = new TestLoader(wrapped, modelCache);
  }

  @Test
  public void testReturnsNullIfUrlIsNull() {
    urlLoader.resultUrl = null;
    assertNull(urlLoader.buildLoadData(new Object(), 100, 100, options));
  }

  @Test
  public void testReturnsNullIfUrlIsEmpty() {
    urlLoader.resultUrl = "    ";
    assertNull(urlLoader.buildLoadData(new Object(), 100, 100, options));
  }

  @Test
  public void testReturnsUrlFromCacheIfPresent() {
    Object model = new Object();
    int width = 100;
    int height = 200;
    GlideUrl expectedUrl = mock(GlideUrl.class);
    when(modelCache.get(eq(model), eq(width), eq(height))).thenReturn(expectedUrl);

    when(wrapped.buildLoadData(eq(expectedUrl), eq(width), eq(height), eq(options)))
        .thenReturn(new ModelLoader.LoadData<>(mock(Key.class), fetcher));

    assertEquals(
        fetcher,
        Preconditions.checkNotNull(urlLoader.buildLoadData(model, width, height, options)).fetcher);
  }

  @Test
  public void testBuildsNewUrlIfNotPresentInCache() {
    int width = 10;
    int height = 11;

    urlLoader.resultUrl = "fakeUrl";
    when(wrapped.buildLoadData(any(GlideUrl.class), eq(width), eq(height), eq(options)))
        .thenAnswer(
            new Answer<ModelLoader.LoadData<InputStream>>() {
              @Override
              public ModelLoader.LoadData<InputStream> answer(InvocationOnMock invocationOnMock) {
                GlideUrl glideUrl = (GlideUrl) invocationOnMock.getArguments()[0];
                assertEquals(urlLoader.resultUrl, glideUrl.toStringUrl());
                return new ModelLoader.LoadData<>(mock(Key.class), fetcher);
              }
            });
    assertEquals(
        fetcher,
        Preconditions.checkNotNull(
                urlLoader.buildLoadData(new GlideUrl(urlLoader.resultUrl), width, height, options))
            .fetcher);
  }

  @Test
  public void testAddsNewUrlToCacheIfNotPresentInCache() {
    urlLoader.resultUrl = "fakeUrl";
    Object model = new Object();
    int width = 400;
    int height = 500;

    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocationOnMock) {
                GlideUrl glideUrl = (GlideUrl) invocationOnMock.getArguments()[3];
                assertEquals(urlLoader.resultUrl, glideUrl.toStringUrl());
                return null;
              }
            })
        .when(modelCache)
        .put(eq(model), eq(width), eq(height), any(GlideUrl.class));

    urlLoader.buildLoadData(model, width, height, options);

    verify(modelCache).put(eq(model), eq(width), eq(height), any(GlideUrl.class));
  }

  @Test
  public void testDoesNotInteractWithModelCacheIfNull() {
    TestLoader urlLoader = new TestLoader(wrapped, null);
    urlLoader.resultUrl = "fakeUrl";

    int width = 456;
    int height = 789;

    when(wrapped.buildLoadData(any(GlideUrl.class), eq(width), eq(height), eq(options)))
        .thenReturn(new ModelLoader.LoadData<>(mock(Key.class), fetcher));

    assertEquals(
        fetcher,
        Preconditions.checkNotNull(urlLoader.buildLoadData(new Object(), width, height, options))
            .fetcher);
  }

  private static final class TestLoader extends BaseGlideUrlLoader<Object> {
    String resultUrl;

    TestLoader(
        ModelLoader<GlideUrl, InputStream> concreteLoader,
        ModelCache<Object, GlideUrl> modelCache) {
      super(concreteLoader, modelCache);
    }

    @Override
    protected String getUrl(Object model, int width, int height, Options options) {
      return resultUrl;
    }

    @Override
    public boolean handles(@NonNull Object model) {
      return true;
    }
  }
}
