package com.bumptech.glide.load;

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
@Config(manifest = Config.NONE, sdk = 18)
public class OptionsTest {
  @Rule public final KeyTester keyTester = new KeyTester();

  @Test
  public void testEquals() {
    Option<Object> firstOption = Option.memory("firstKey");
    Object firstValue = new Object();
    Option<Object> secondOption = Option.memory("secondKey");
    Object secondValue = new Object();

    CacheKeyUpdater<Integer> updater = new CacheKeyUpdater<Integer>() {
      @Override
      public void update(byte[] keyBytes, Integer value, MessageDigest messageDigest) {
        messageDigest.update(keyBytes);
        messageDigest.update(ByteBuffer.allocate(4).putInt(value).array());

      }
    };
    Option<Integer> firstDiskOption = Option.disk("firstDisk", updater);
    Option<Integer> secondDiskOption = Option.disk("secondDisk", updater);

    keyTester
        .addEquivalenceGroup(new Options(), new Options())
        .addEquivalenceGroup(
            new Options().set(firstOption, firstValue),
            new Options().set(firstOption, firstValue))
        .addEquivalenceGroup(
            new Options().set(secondOption, secondValue),
            new Options().set(secondOption, secondValue))
        .addEquivalenceGroup(
            new Options().set(firstOption, firstValue).set(secondOption, secondValue),
            new Options().set(firstOption, firstValue).set(secondOption, secondValue),
            new Options().set(secondOption, secondValue).set(firstOption, firstValue))
        .addEquivalenceGroup(
            new Options().set(firstOption, secondValue))
        .addEquivalenceGroup(
            new Options().set(secondOption, firstValue))
        .addEquivalenceGroup(
            new Options().set(firstDiskOption, 1),
            new Options().set(firstDiskOption, 1))
        .addEquivalenceGroup(
            new Options().set(secondDiskOption, 1),
            new Options().set(secondDiskOption, 1))
        .addEquivalenceGroup(
            new Options().set(firstDiskOption, 2))
        .addEquivalenceGroup(
            new Options().set(secondDiskOption, 2))
        .addEquivalenceGroup(
            new Options().set(firstDiskOption, 1).set(secondDiskOption, 2),
            new Options().set(secondDiskOption, 2).set(firstDiskOption, 1))
        .addRegressionTest(
            new Options().set(firstOption, firstValue),
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        .addRegressionTest(
            new Options().set(firstOption, firstValue).set(secondOption, secondValue),
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        .addRegressionTest(
            new Options().set(firstDiskOption, 123),
            "3c87124d1a765dc3d566f947d536ef140a4aca645c0947f702356714855b4a8e")
        .addRegressionTest(
            new Options().set(firstDiskOption, 123).set(secondDiskOption, 123),
            "6697f654686c9a925905db3840e9c99944642c2b91d6200360d77639c1754d51")
        .test();
  }

}
