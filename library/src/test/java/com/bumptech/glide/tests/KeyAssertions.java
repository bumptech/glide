package com.bumptech.glide.tests;

import static com.google.common.truth.Truth.assertThat;

import com.bumptech.glide.load.Key;
import com.google.common.testing.EqualsTester;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class KeyAssertions {

  public static void assertSame(Key first, Key second) throws NoSuchAlgorithmException {
    assertSameOrdered(first, second);
    assertSameOrdered(second, first);
  }

  private static void assertSameOrdered(Key first, Key second) throws NoSuchAlgorithmException {
    new EqualsTester()
        .addEqualityGroup(first, second)
        .testEquals();

    assertThat(getDigest(first)).isEqualTo(getDigest(second));
  }

  public static void assertDifferent(Key first, Key second) throws NoSuchAlgorithmException {
    assertDifferent(first, second, true);
    assertDifferent(second, first, true);
  }

  public static void assertDifferent(Key first, Key second, boolean checkDiskCacheKey)
      throws NoSuchAlgorithmException {
    new EqualsTester()
        .addEqualityGroup(first)
        .addEqualityGroup(second)
        .testEquals();

    if (checkDiskCacheKey) {
      MessageDigest firstDigest = MessageDigest.getInstance("SHA-1");
      first.updateDiskCacheKey(firstDigest);
      MessageDigest secondDigest = MessageDigest.getInstance("SHA-1");
      second.updateDiskCacheKey(secondDigest);

      assertThat(getDigest(first)).isNotEqualTo(getDigest(second));
    }
  }

  private static byte[] getDigest(Key key) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    key.updateDiskCacheKey(md);
    return md.digest();
  }
}
