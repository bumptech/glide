package com.bumptech.glide.load.model;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

/** Tests for the {@link DataUrlLoader} class. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class DataUrlLoaderTest {

  // A valid base64-encoded PNG (a small "Google" logo).
  @SuppressWarnings("SpellCheckingInspection")
  private static final String VALID_PNG =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAALCAYAAAAeEY8BAAADFElEQVR42mNgAAK5ig+Cii"
          + "UfSmUL3mVL5r7PE8t5M1U06027eMYLMQZKQUMDE8eyxGrOJYmdDKtCmTHkFfO/iCsUfTykUPFeASH6n1Es+3Wj"
          + "SM5rKQYqANbFcTmsC2OXYpWUKXw/R67ofQEhQ+5FecnfDnYxPJNmzAp35n8Gxv/7pTT+75PQBrFh4iq5b/lk8z"
          + "+aiue+tZDKeaPBMC8qh2leFNgB/xkYGO+Eu+ncCnZRAiuWyHv3VDzngxMui0EWPgpx6n4U4Wx7J8De86aP2blr"
          + "rgaq//fwCv8/KNT//5CU0f99okn/dwse+b9fQECx9IObQvGHMrn8D66See9eiWa9s2GYE57DMCdi6Qs3N+6HIc"
          + "4T70a4mtz2t55909u0jkE85+1Tsdx30ciWSuQ+F+VPe6kskPFc4Z6XRcp9H8t2mNxVF72Gq066K//vZe//v4cD"
          + "ru//ds7V/7dx1MoXf9gtW/zRFGLO+x7x7DeVDDOBDpgZvvSut3nWXR/LyptuxgG33Axzr7rr2TKIZb1eIpL1ej"
          + "co3mGGCWe8cRJMf7FVKO1F/y1Xww4gng6Tu+Ko7X7JTvPo/52Mm//vYMqBO2AbU/H/LUwzpQreT5LOf98PEhPL"
          + "ftslkfvGjGF6aA4QL73halh7y9XgwHVnM2G4b0G+FM549Uw440U7Q+h/eCoVSH0+GYjrrjrr2V530n16w1qdFy"
          + "R+wUYr6YKNRtH/7QzpQHzsfwMDE9gBmxl6/29hcNdu+M8G9HmCWM7bQ6I5bxPBhk0NzmGYErT0mpOe0TVHnY+X"
          + "HXRMQMKrQhkg9omkvZYUSHvZJ5T+Yh3IUoHUZ/mCqc87BdOe2UB9HXzZQWvCeTuNqPO2GgmghAROgFsZ8oCWtg"
          + "BxDNABASC1olmveEQyX/sB8SKRzJcPgbQxw0S/IoaJvksZJvsqXnLQDLhoq7n7nI3GxHOWWs4M1AQ8ic9FhdNf"
          + "7ZRKeyYCjsrUly7AqDzOQC8glP7SFWjhCVhUKiTc5xBIebaAbg4AWcyf+qxNMPXZKoGU57UCqU+KQKGCTwsAbx"
          + "BBmvLaD+cAAAAASUVORK5CYII=";

  private static final String INVALID_URL_WRONG_SCHEME1 = "test";
  private static final String INVALID_URL_WRONG_SCHEME2 = "http://google.com";
  private static final String INVALID_URL_WRONG_SCHEME3 = "data:text";
  private static final String INVALID_URL_MISSING_COMMA = "data:image/png;base64=NOT_BASE64";
  private static final String INVALID_URL_WRONG_ENCODING = "data:image/png;base32,";

  @Mock private MultiModelLoaderFactory multiFactory;
  private DataUrlLoader<String, InputStream> dataUrlLoader;
  private DataFetcher<InputStream> fetcher;
  private Options options;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    DataUrlLoader.StreamFactory<String> factory = new DataUrlLoader.StreamFactory<>();
    options = new Options();
    dataUrlLoader = (DataUrlLoader<String, InputStream>) factory.build(multiFactory);
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
    byte[] expected =
        Base64.decode(VALID_PNG.substring(VALID_PNG.indexOf(',') + 1), Base64.DEFAULT);
    CallBack callback = new CallBack();
    fetcher.loadData(Priority.HIGH, callback);
    byte[] result = new byte[((ByteArrayInputStream) callback.data).available()];
    assertEquals(result.length, ((ByteArrayInputStream) callback.data).read(result));
    assertTrue(Arrays.equals(result, expected));
    assertNull(callback.exception);
  }

  @Test
  public void testDecodeInvalidScheme() {
    fetcher = dataUrlLoader.buildLoadData(INVALID_URL_WRONG_SCHEME1, -1, -1, options).fetcher;
    CallBack callback = new CallBack();
    fetcher.loadData(Priority.HIGH, callback);
    assertNotNull(callback.exception);
  }

  @Test
  public void testDecodeMissingComma() {
    fetcher = dataUrlLoader.buildLoadData(INVALID_URL_MISSING_COMMA, -1, -1, options).fetcher;
    CallBack callback = new CallBack();
    fetcher.loadData(Priority.HIGH, callback);
    assertNotNull(callback.exception);
  }

  @Test
  public void testDecodeWrongEncoding() {
    fetcher = dataUrlLoader.buildLoadData(INVALID_URL_WRONG_ENCODING, -1, -1, options).fetcher;
    CallBack callback = new CallBack();
    fetcher.loadData(Priority.HIGH, callback);
    assertNotNull(callback.exception);
  }

  private static final class CallBack implements DataFetcher.DataCallback<Object> {

    public Object data;
    public Exception exception;

    @Override
    public void onDataReady(@Nullable Object data) {
      this.data = data;
    }

    @Override
    public void onLoadFailed(@NonNull Exception e) {
      this.exception = e;
    }
  }
}
