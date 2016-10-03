package com.bumptech.glide.signature;

import com.bumptech.glide.tests.KeyAssertions;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MediaStoreSignatureTest {

  @Test
  public void testSignaturesDifferIfMimeTypeDiffers()
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
    MediaStoreSignature first = new MediaStoreSignature("first", 100, 1);
    MediaStoreSignature second = new MediaStoreSignature("second", 100, 1);

    KeyAssertions.assertDifferent(first, second);
  }

  @Test
  public void testSignaturesDifferIfDateModifiedDiffers()
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
    MediaStoreSignature first = new MediaStoreSignature("mimeType", 100, 1);
    MediaStoreSignature second = new MediaStoreSignature("mimeType", 999, 1);

    KeyAssertions.assertDifferent(first, second);
  }

  @Test
  public void testSignaturesDifferIfOrientationDiffers()
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
    MediaStoreSignature first = new MediaStoreSignature("mimeType", 100, 1);
    MediaStoreSignature second = new MediaStoreSignature("mimeType", 100, 9);

    KeyAssertions.assertDifferent(first, second);
  }

  @Test
  public void testSignaturesAreTheSameIfAllArgsAreTheSame()
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
    MediaStoreSignature first = new MediaStoreSignature("mimeType", 100, 1);
    MediaStoreSignature second = new MediaStoreSignature("mimeType", 100, 1);

    KeyAssertions.assertSame(first, second);
  }
}
