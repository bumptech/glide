package com.bumptech.glide.load.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.support.annotation.Nullable;
import android.util.Base64;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Tests for the {@link DataUrlLoader} class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class DataUrlLoaderTest {

  // A valid base64-encoded PNG (a small "Google" logo).
  @SuppressWarnings("SpellCheckingInspection")
  private static final String VALID_PNG = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAALCA"
      + "YAAAAeEY8BAAADFElEQVR42mNgAAK5ig+CiiUfSmUL3mVL5r7PE8t5M1U06027eMYLMQZKQUMDE8eyxGrOJYmdDKtC"
      + "mTHkFfO/iCsUfTykUPFeASH6n1Es+3WjSM5rKQYqANbFcTmsC2OXYpWUKXw/R67ofQEhQ+5FecnfDnYxPJNmzAp35n"
      + "8Gxv/7pTT+75PQBrFh4iq5b/lk8z+aiue+tZDKeaPBMC8qh2leFNgB/xkYGO+Eu+ncCnZRAiuWyHv3VDzngxMui0EW"
      + "Pgpx6n4U4Wx7J8De86aP2blrrgaq//fwCv8/KNT//5CU0f99okn/dwse+b9fQECx9IObQvGHMrn8D66See9eiWa9s2"
      + "GYE57DMCdi6Qs3N+6HIc4T70a4mtz2t55909u0jkE85+1Tsdx30ciWSuQ+F+VPe6kskPFc4Z6XRcp9H8t2mNxVF72G"
      + "q066K//vZe//v4cDru//ds7V/7dx1MoXf9gtW/zRFGLO+x7x7DeVDDOBDpgZvvSut3nWXR/LyptuxgG33Axzr7rr2T"
      + "KIZb1eIpL1ejco3mGGCWe8cRJMf7FVKO1F/y1Xww4gng6Tu+Ko7X7JTvPo/52Mm//vYMqBO2AbU/H/LUwzpQreT5LO"
      + "f98PEhPLftslkfvGjGF6aA4QL73halh7y9XgwHVnM2G4b0G+FM549Uw440U7Q+h/eCoVSH0+GYjrrjrr2V530n16w1"
      + "qdFyR+wUYr6YKNRtH/7QzpQHzsfwMDE9gBmxl6/29hcNdu+M8G9HmCWM7bQ6I5bxPBhk0NzmGYErT0mpOe0TVHnY+X"
      + "HXRMQMKrQhkg9omkvZYUSHvZJ5T+Yh3IUoHUZ/mCqc87BdOe2UB9HXzZQWvCeTuNqPO2GgmghAROgFsZ8oCWtgBxDN"
      + "ABASC1olmveEQyX/sB8SKRzJcPgbQxw0S/IoaJvksZJvsqXnLQDLhoq7n7nI3GxHOWWs4M1AQ8ic9FhdNf7ZRKeyYC"
      + "jsrUly7AqDzOQC8glP7SFWjhCVhUKiTc5xBIebaAbg4AWcyf+qxNMPXZKoGU57UCqU+KQKGCTwsAbxBBmvLaD+cAAA"
      + "AASUVORK5CYII=";

  private static final String INVALID_URL_WRONG_SCHEME1 = "test";
  private static final String INVALID_URL_WRONG_SCHEME2 = "http://google.com";
  private static final String INVALID_URL_WRONG_SCHEME3 = "data:text";
  private static final String INVALID_URL_MISSING_COMMA = "data:image/png;base64=NOT_BASE64";
  private static final String INVALID_URL_WRONG_ENCODING = "data:image/png;base32,";

  @Mock
  private MultiModelLoaderFactory multiFactory;
  private DataUrlLoader<InputStream> dataUrlLoader;
  private DataFetcher<InputStream> fetcher;
  private Options options;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    DataUrlLoader.StreamFactory factory = new DataUrlLoader.StreamFactory();
    options = new Options();
    dataUrlLoader = (DataUrlLoader<InputStream>) factory.build(multiFactory);
    fetcher = dataUrlLoader.buildLoadData(VALID_PNG, -1, -1, options).fetcher;

  }

  @Test
  public void testHandleDataUri() {
    assertTrue(dataUrlLoader.handles(VALID_PNG));
  }

  @Test
  public void testHandleFalseDataUri() {
    assertFalse(dataUrlLoader.handles(INVALID_URL_WRONG_SCHEME1));
    assertFalse(dataUrlLoader.handles(INVALID_URL_WRONG_SCHEME2));
    assertFalse(dataUrlLoader.handles(INVALID_URL_WRONG_SCHEME3));
  }

  @Test
  public void testDecode() throws IOException {
    byte[] expected = Base64
        .decode(VALID_PNG.substring(VALID_PNG.indexOf(',') + 1), Base64.DEFAULT);
    CallBack callback = new CallBack();
    fetcher.loadData(Priority.HIGH, callback);
    byte[] result = new byte[((ByteArrayInputStream) callback.data).available()];
    assertEquals(result.length, ((ByteArrayInputStream) callback.data).read(result));
    assertTrue(Arrays.equals(result, expected));
    assertNull(callback.exception);
  }

  @Test
  public void testDecodeInvalidScheme() throws IOException {
    fetcher = dataUrlLoader.buildLoadData(INVALID_URL_WRONG_SCHEME1, -1, -1, options).fetcher;
    CallBack callback = new CallBack();
    fetcher.loadData(Priority.HIGH, callback);
    assertNotNull(callback.exception);
  }

  @Test
  public void testDecodeMissingComma() throws IOException {
    fetcher = dataUrlLoader.buildLoadData(INVALID_URL_MISSING_COMMA, -1, -1, options).fetcher;
    CallBack callback = new CallBack();
    fetcher.loadData(Priority.HIGH, callback);
    assertNotNull(callback.exception);
  }

  @Test
  public void testDecodeWrongEncoding() throws IOException {
    fetcher = dataUrlLoader.buildLoadData(INVALID_URL_WRONG_ENCODING, -1, -1, options).fetcher;
    CallBack callback = new CallBack();
    fetcher.loadData(Priority.HIGH, callback);
    assertNotNull(callback.exception);
  }

  private class CallBack implements DataFetcher.DataCallback<Object> {

    public Object data;
    public Exception exception;

    @Override
    public void onDataReady(@Nullable Object data) {
      this.data = data;
    }

    @Override
    public void onLoadFailed(Exception e) {
      this.exception = e;
    }
  }
}
