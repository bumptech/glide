package com.bumptech.glide.signature;

import static org.junit.Assert.assertNotNull;

import android.content.Context;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.tests.KeyTester;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class ApplicationVersionSignatureTest {
  @Rule public final KeyTester keyTester = new KeyTester();
  private Context context;

  @Before
  public void setUp() {
    context = RuntimeEnvironment.application;
  }

  @After
  public void tearDown() {
    ApplicationVersionSignature.reset();
  }

  @Test
  public void testCanGetKeyForSignature() {
    Key key = ApplicationVersionSignature.obtain(context);
    assertNotNull(key);
  }

  @Test
  public void testKeyForSignatureIsTheSameAcrossCallsInTheSamePackage()
      throws NoSuchAlgorithmException, UnsupportedEncodingException {
    keyTester
        .addEquivalenceGroup(
            ApplicationVersionSignature.obtain(context),
            ApplicationVersionSignature.obtain(context))
        .addEquivalenceGroup(new ObjectKey("test"))
        .addRegressionTest(
            ApplicationVersionSignature.obtain(context),
            "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9")
        .test();
  }
}
