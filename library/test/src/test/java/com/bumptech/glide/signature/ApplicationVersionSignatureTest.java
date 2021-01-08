package com.bumptech.glide.signature;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.tests.KeyTester;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class ApplicationVersionSignatureTest {
  @Rule public final KeyTester keyTester = new KeyTester();
  private Context context;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
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

  @Test
  public void testUnresolvablePackageInfo() throws NameNotFoundException {
    Context context = mock(Context.class, Answers.RETURNS_DEEP_STUBS.get());
    String packageName = "my.package";
    when(context.getPackageName()).thenReturn(packageName);
    when(context.getPackageManager().getPackageInfo(packageName, 0))
        .thenThrow(new NameNotFoundException("test"));

    Key key = ApplicationVersionSignature.obtain(context);

    assertNotNull(key);
  }

  @Test
  public void testMissingPackageInfo() throws NameNotFoundException {
    Context context = mock(Context.class, Answers.RETURNS_DEEP_STUBS.get());
    String packageName = "my.package";
    when(context.getPackageName()).thenReturn(packageName);
    when(context.getPackageManager().getPackageInfo(packageName, 0)).thenReturn(null);

    Key key = ApplicationVersionSignature.obtain(context);

    assertNotNull(key);
  }
}
