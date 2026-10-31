package com.bumptech.glide.load;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;

import androidx.annotation.NonNull;
import com.bumptech.glide.load.Option.CacheKeyUpdater;
import com.bumptech.glide.tests.KeyTester;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class OptionsTest {
  private static final CacheKeyUpdater<Integer> INTEGER_UPDATER =
      new CacheKeyUpdater<Integer>() {
        @Override
        public void update(
            @NonNull byte[] keyBytes, @NonNull Integer value, @NonNull MessageDigest messageDigest) {
          messageDigest.update(keyBytes);
          messageDigest.update(ByteBuffer.allocate(4).putInt(value).array());
        }
      };

  @Rule public final KeyTester keyTester = new KeyTester();

  @Test
  public void testEquals() {
    Option<Object> firstMemoryOption = Option.memory("firstKey");
    Object firstValue = new Object();
    Option<Object> secondMemoryOption = Option.memory("secondKey");
    Object secondValue = new Object();

    CacheKeyUpdater<Integer> updater =
        new CacheKeyUpdater<Integer>() {
          @Override
          public void update(
              @NonNull byte[] keyBytes,
              @NonNull Integer value,
              @NonNull MessageDigest messageDigest) {
            messageDigest.update(keyBytes);
            messageDigest.update(ByteBuffer.allocate(4).putInt(value).array());
          }
        };
    Option<Integer> firstDiskOption = Option.disk("firstDisk", updater);
    Option<Integer> secondDiskOption = Option.disk("secondDisk", updater);

    keyTester
        .addEquivalenceGroup(new Options(), new Options())
        .addEquivalenceGroup(
            new Options().set(firstMemoryOption, firstValue),
            new Options().set(firstMemoryOption, firstValue))
        .addEquivalenceGroup(
            new Options().set(secondMemoryOption, secondValue),
            new Options().set(secondMemoryOption, secondValue))
        .addEquivalenceGroup(
            new Options().set(firstMemoryOption, firstValue).set(secondMemoryOption, secondValue),
            new Options().set(firstMemoryOption, firstValue).set(secondMemoryOption, secondValue),
            new Options().set(secondMemoryOption, secondValue).set(firstMemoryOption, firstValue))
        .addEquivalenceGroup(new Options().set(firstMemoryOption, secondValue))
        .addEquivalenceGroup(new Options().set(secondMemoryOption, firstValue))
        .addEquivalenceGroup(
            new Options().set(firstDiskOption, 1), new Options().set(firstDiskOption, 1))
        .addEquivalenceGroup(
            new Options().set(secondDiskOption, 1), new Options().set(secondDiskOption, 1))
        .addEquivalenceGroup(new Options().set(firstDiskOption, 2))
        .addEquivalenceGroup(new Options().set(secondDiskOption, 2))
        .addEquivalenceGroup(
            new Options().set(firstDiskOption, 1).set(secondDiskOption, 2),
            new Options().set(secondDiskOption, 2).set(firstDiskOption, 1))
        .addEmptyDigestRegressionTest(new Options().set(firstMemoryOption, firstValue))
        .addEmptyDigestRegressionTest(
            new Options().set(firstMemoryOption, firstValue).set(secondMemoryOption, secondValue))
        .addRegressionTest(
            new Options().set(firstDiskOption, 123),
            "3c87124d1a765dc3d566f947d536ef140a4aca645c0947f702356714855b4a8e")
        .addRegressionTest(
            new Options().set(firstDiskOption, 123).set(secondDiskOption, 123),
            "6697f654686c9a925905db3840e9c99944642c2b91d6200360d77639c1754d51")
        .test();
  }

  @Test
  public void testEquals_withOptionSetToDefaultValue_isEquivalentToUnsetOption() {
    Option<Boolean> memoryOption = Option.memory("memoryKey", false);
    Option<Integer> diskOption = Option.disk("diskKey", 0, INTEGER_UPDATER);

    keyTester
        .addEquivalenceGroup(
            new Options(),
            new Options().set(memoryOption, false),
            new Options().set(diskOption, 0),
            new Options().set(memoryOption, false).set(diskOption, 0))
        .addEquivalenceGroup(
            new Options().set(memoryOption, true),
            new Options().set(memoryOption, true).set(diskOption, 0))
        .addEquivalenceGroup(
            new Options().set(diskOption, 1),
            new Options().set(diskOption, 1).set(memoryOption, false))
        .addEmptyDigestRegressionTest(new Options().set(diskOption, 0))
        .test();
  }

  @Test
  public void testPutAll_withOptionSetToDefaultValue_overridesNonDefaultValue() {
    Option<Boolean> option = Option.memory("key", false);
    Options options = new Options().set(option, true);

    options.putAll(new Options().set(option, false));

    assertThat(options.get(option)).isFalse();
    assertThat(options).isEqualTo(new Options());
  }
}
