package com.bumptech.glide.signature;

import static org.junit.Assert.assertNotNull;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.tests.KeyAssertions;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class ApplicationVersionSignatureTest {

  @After
  public void tearDown() {
    ApplicationVersionSignature.reset();
  }

  @Test
  public void testCanGetKeyForSignature() {
    Key key = ApplicationVersionSignature.obtain(RuntimeEnvironment.application);
    assertNotNull(key);
  }

  @Test
  public void testKeyForSignatureIsTheSameAcrossCallsInTheSamePackage()
      throws NoSuchAlgorithmException, UnsupportedEncodingException {
    Key first = ApplicationVersionSignature.obtain(RuntimeEnvironment.application);
    Key second = ApplicationVersionSignature.obtain(RuntimeEnvironment.application);
    KeyAssertions.assertSame(first, second);
  }
}
