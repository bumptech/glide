package com.bumptech.glide.load;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class OptionsTest {

  @Test
  public void testEquals() {
    Option<Object> firstOption = Option.memory("firstKey");
    Object firstValue = new Object();
    Option<Object> secondOption = Option.memory("secondKey");
    Object secondValue = new Object();
    new EqualsTester()
        .addEqualityGroup(new Options(), new Options())
        .addEqualityGroup(
            new Options().set(firstOption, firstValue),
            new Options().set(firstOption, firstValue)
        )
        .addEqualityGroup(
            new Options().set(secondOption, secondValue),
            new Options().set(secondOption, secondValue)
        )
        .addEqualityGroup(
            new Options().set(firstOption, firstValue).set(secondOption, secondValue),
            new Options().set(firstOption, firstValue).set(secondOption, secondValue)
        ).testEquals();
  }

}
