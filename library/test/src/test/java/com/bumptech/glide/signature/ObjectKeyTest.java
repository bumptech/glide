package com.bumptech.glide.signature;

import com.bumptech.glide.tests.KeyTester;
import java.security.NoSuchAlgorithmException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ObjectKeyTest {
  @Rule public final KeyTester keyTester = new KeyTester();

  @Test
  public void testEqualsHashCodeAndDigest() throws NoSuchAlgorithmException {
    Object object = new Object();
    keyTester
        .addEquivalenceGroup(new ObjectKey(object), new ObjectKey(object))
        .addEquivalenceGroup(new ObjectKey(new Object()))
        .addEquivalenceGroup(new ObjectKey("test"), new ObjectKey("test"))
        .addRegressionTest(
            new ObjectKey("test"),
            "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08")
        .test();
  }
}
