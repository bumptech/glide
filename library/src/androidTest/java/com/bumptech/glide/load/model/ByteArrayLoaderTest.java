package com.bumptech.glide.load.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

@RunWith(JUnit4.class)
public class ByteArrayLoaderTest {

  @Mock ByteArrayLoader.Converter<Object> converter;
  @Mock DataFetcher.DataCallback<Object> callback;
  private ByteArrayLoader<Object> loader;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    loader = new ByteArrayLoader<>(converter);
  }

  @Test
  public void testCanHandleByteArray() {
    byte[] data = new byte[10];
    DataFetcher<Object> fetcher = loader.getDataFetcher(data, -1, -1);
    assertNotNull(fetcher);
  }

  @Test
  public void testFetcherReturnsObjectReceivedFromConverter() throws IOException {
    byte[] data = "fake".getBytes();
    Object expected = new Object();
    when(converter.convert(eq(data))).thenReturn(expected);

    loader.getDataFetcher(data, 10, 10).loadData(Priority.HIGH, callback);
    verify(callback).onDataReady(eq(expected));
  }

  @Test
  public void testFetcherRetrunsDataClassFromConverter() {
    when(converter.getDataClass()).thenReturn(Object.class);
    assertEquals(Object.class, loader.getDataFetcher(new byte[10], 10, 10).getDataClass());
  }
}
