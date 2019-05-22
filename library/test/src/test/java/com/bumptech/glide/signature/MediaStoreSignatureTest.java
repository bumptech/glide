package com.bumptech.glide.signature;

import com.bumptech.glide.tests.KeyTester;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MediaStoreSignatureTest {
  @Rule public final KeyTester keyTester = new KeyTester();

  @Test
  public void equalsHashCodeAndDigest() {
    keyTester
        .addEquivalenceGroup(
            new MediaStoreSignature("first", 100, 1), new MediaStoreSignature("first", 100, 1))
        .addEquivalenceGroup(new MediaStoreSignature("second", 100, 1))
        .addEquivalenceGroup(new MediaStoreSignature("first", 200, 1))
        .addEquivalenceGroup(new MediaStoreSignature("first", 100, 2))
        .addRegressionTest(
            new MediaStoreSignature("first", 100, 1),
            "04959925006b21081000fd10835cc376343c0e922df0bd7346897ede6f958adf")
        .test();
  }
}
