package com.bumptech.glide.signature;

import com.bumptech.glide.tests.KeyAssertions;
import java.security.NoSuchAlgorithmException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ObjectKeyTest {

  @Test
  public void testEquals() throws NoSuchAlgorithmException {
    Object object = new Object();
    KeyAssertions.assertSame(new ObjectKey(object), new ObjectKey(object));
    KeyAssertions.assertDifferent(new ObjectKey(object), new ObjectKey(new Object()));
  }
}
