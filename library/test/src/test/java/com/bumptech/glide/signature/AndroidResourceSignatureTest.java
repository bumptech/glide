package com.bumptech.glide.signature;

import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.tests.KeyTester;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AndroidResourceSignatureTest {
  @Rule public final KeyTester keyTester = new KeyTester();
  private Context context;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
  }

  @Test
  public void testCanGetKeyForSignature() {
    Key key = AndroidResourceSignature.obtain(context);
    assertNotNull(key);
  }

  @Test
  public void testKeyForSignatureIsTheSameAcrossCallsInTheSamePackage() {
    keyTester
        .addEquivalenceGroup(
            AndroidResourceSignature.obtain(context), AndroidResourceSignature.obtain(context))
        .addEquivalenceGroup(new ObjectKey("test"))
        .addRegressionTest(
            ApplicationVersionSignature.obtain(context),
            "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9")
        .test();
  }

  @Test
  public void testKeyForSignatureDiffersByNightMode() {
    RuntimeEnvironment.setQualifiers("notnight");
    keyTester
        .addEquivalenceGroup(
            AndroidResourceSignature.obtain(context), AndroidResourceSignature.obtain(context))
        .addRegressionTest(
            AndroidResourceSignature.obtain(context),
            "265d958bdae1bea56e45cc31f4db672c22893b66fef85617bbc78742bd912207");
    RuntimeEnvironment.setQualifiers("night");
    keyTester
        .addEquivalenceGroup(
            AndroidResourceSignature.obtain(context), AndroidResourceSignature.obtain(context))
        .addRegressionTest(
            AndroidResourceSignature.obtain(context),
            "96c9b8d5bb071ccd67df50cd9a0059640ebd02db78d08f07611ec145ce44a638");

    keyTester.test();
  }

  @Test
  public void testMissingPackageInfo() throws NameNotFoundException {
    // Make getPackageInfo throw NameNotFoundException.
    Shadows.shadowOf(context.getPackageManager()).removePackage(context.getPackageName());
    Key key = AndroidResourceSignature.obtain(context);

    assertNotNull(key);
  }
}
