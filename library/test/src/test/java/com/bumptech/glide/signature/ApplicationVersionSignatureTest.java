package com.bumptech.glide.signature;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
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
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
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
  public void canGetKeyForSignature() {
    Key key = ApplicationVersionSignature.obtain(context);
    assertNotNull(key);
  }

  @Test
  public void keyForSignatureIsTheSameAcrossCallsInTheSamePackage()
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
  public void unresolvablePackageInfo() {
    shadowOf(context.getPackageManager()).removePackage(context.getPackageName());

    Key key = ApplicationVersionSignature.obtain(context);

    assertThat(key).isNotNull();
  }

  @Test
  public void missingPackageInfo() throws NameNotFoundException {
    PackageManager packageManager = mock(PackageManager.class);
    when(packageManager.getPackageInfo(context.getPackageName(), 0)).thenReturn(null);
    Context wrappedContext =
        new ContextWrapper(context) {
          @Override
          public PackageManager getPackageManager() {
            return packageManager;
          }
        };

    Key key = ApplicationVersionSignature.obtain(wrappedContext);

    assertThat(key).isNotNull();
  }
}
