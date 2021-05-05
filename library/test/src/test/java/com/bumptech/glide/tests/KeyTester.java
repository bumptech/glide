package com.bumptech.glide.tests;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;
import static org.junit.Assert.fail;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.Key;
import com.google.common.base.Equivalence;
import com.google.common.testing.EquivalenceTester;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public final class KeyTester implements TestRule {
  private static final String EMPTY_DIGEST_STRING =
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
  private final List<KeyAndHash> regressionTests = new ArrayList<>();
  private final Sha256 sha256 = new Sha256();
  private final EquivalenceTester<Key> tester = EquivalenceTester.of(new KeyEquivalence(sha256));
  private boolean isUsedWithoutCallingTest;
  private boolean isUsedAsRule;

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        isUsedAsRule = true;
        base.evaluate();
        if (isUsedWithoutCallingTest) {
          fail("You used KeyTester but failed to call test()!");
        }
      }
    };
  }

  private void assertUsedAsRule() {
    if (!isUsedAsRule) {
      fail("You must use KeyTester as an @Rule");
    }
  }

  @CheckResult
  public KeyTester addEquivalenceGroup(Key first, Key... rest) {
    assertUsedAsRule();
    isUsedWithoutCallingTest = true;
    tester.addEquivalenceGroup(first, rest);
    return this;
  }

  @CheckResult
  public KeyTester addRegressionTest(Key key, String expectedDigest) {
    assertUsedAsRule();
    if (EMPTY_DIGEST_STRING.equals(expectedDigest)) {
      throw new IllegalArgumentException(
          "Expected digest is empty, if this is intended use "
              + "addEmptyDigestRegressionTest instead");
    }
    return addRegressionTestInternal(key, expectedDigest);
  }

  @CheckResult
  public KeyTester addEmptyDigestRegressionTest(Key key) {
    assertUsedAsRule();
    return addRegressionTestInternal(key, EMPTY_DIGEST_STRING);
  }

  private KeyTester addRegressionTestInternal(Key key, String expectedDigest) {
    isUsedWithoutCallingTest = true;
    regressionTests.add(new KeyAndHash(key, expectedDigest));
    return this;
  }

  public void test() {
    assertUsedAsRule();
    isUsedWithoutCallingTest = false;
    tester.test();

    assertThat(regressionTests).isNotEmpty();
    int i = 1;
    for (KeyAndHash keyAndHash : regressionTests) {
      assert_()
          .withMessage(
              "Unexpected digest for regression test [" + i + "]: with key: " + keyAndHash.key)
          .that(sha256.getStringDigest(keyAndHash.key))
          .isEqualTo(keyAndHash.hash);
      i++;
    }
  }

  private static final class Sha256 {

    private final MessageDigest digest;

    Sha256() {
      try {
        digest = MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
    }

    private byte[] getDigest(Key key) {
      try {
        key.updateDiskCacheKey(digest);
        return digest.digest();
      } finally {
        digest.reset();
      }
    }

    String getStringDigest(Key key) {
      return com.bumptech.glide.util.Util.sha256BytesToHex(getDigest(key));
    }
  }

  /** Tests equals, hashcode and digest methods of {@link Key}s. */
  private static final class KeyEquivalence extends Equivalence<Key> {

    private final Sha256 sha256;

    KeyEquivalence(Sha256 sha256) {
      this.sha256 = sha256;
    }

    @Override
    protected boolean doEquivalent(@NonNull Key a, @NonNull Key b) {
      byte[] aDigest = sha256.getDigest(a);
      byte[] bDigest = sha256.getDigest(b);
      Object object = new Object();
      Object sentinel = null;
      return a.equals(b)
          && b.equals(a)
          && !a.equals(sentinel)
          && !b.equals(sentinel)
          && !a.equals(object)
          && !b.equals(object)
          && Arrays.equals(aDigest, bDigest);
    }

    @Override
    protected int doHash(@NonNull Key key) {
      return key.hashCode();
    }
  }

  private static final class KeyAndHash {
    private final Key key;
    private final String hash;

    KeyAndHash(Key key, String hash) {
      this.key = key;
      this.hash = hash;
    }
  }
}
