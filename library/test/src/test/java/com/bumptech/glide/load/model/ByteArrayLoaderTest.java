package com.bumptech.glide.load.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.util.Preconditions;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class ByteArrayLoaderTest {

  @Mock private ByteArrayLoader.Converter<Object> converter;
  @Mock private DataFetcher.DataCallback<Object> callback;
  private ByteArrayLoader<Object> loader;
  private Options options;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    loader = new ByteArrayLoader<>(converter);
    options = new Options();
  }

  @Test
  public void testCanHandleByteArray() {
    byte[] data = new byte[10];
    DataFetcher<Object> fetcher =
        Preconditions.checkNotNull(loader.buildLoadData(data, -1, -1, options)).fetcher;
    assertNotNull(fetcher);
  }

  @Test
  public void testFetcherReturnsObjectReceivedFromConverter() throws IOException {
    byte[] data = "fake".getBytes("UTF-8");
    Object expected = new Object();
    when(converter.convert(eq(data))).thenReturn(expected);

    Preconditions.checkNotNull(loader.buildLoadData(data, 10, 10, options))
        .fetcher
        .loadData(Priority.HIGH, callback);
    verify(callback).onDataReady(eq(expected));
  }

  @Test
  public void testFetcherReturnsDataClassFromConverter() {
    when(converter.getDataClass()).thenReturn(Object.class);
    assertEquals(
        Object.class,
        Preconditions.checkNotNull(loader.buildLoadData(new byte[10], 10, 10, options))
            .fetcher
            .getDataClass());
  }
}
