package com.bumptech.glide.load.engine.bitmap_recycle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bumptech.glide.load.engine.bitmap_recycle.SizeStrategy.Key;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SizeStrategyKeyTest {

  private SizeStrategy.KeyPool keyPool;

  @Before
  public void setUp() {
    keyPool = mock(SizeStrategy.KeyPool.class);
  }

  @Test
  public void testEquality() {
    Key first = new Key(keyPool);
    first.init(100);
    Key second = new Key(keyPool);
    second.init(100);
    Key third = new Key(keyPool);
    third.init(50);

    new EqualsTester().addEqualityGroup(first, second).addEqualityGroup(third).testEquals();
  }

  @Test
  public void testReturnsSelfToPoolOnOffer() {
    Key key = new Key(keyPool);
    key.offer();

    verify(keyPool).offer(eq(key));
  }

  @Test
  public void testInitSetsSize() {
    Key key = new Key(keyPool);
    key.init(100);

    Key other = new Key(keyPool);
    other.init(200);

    assertNotEquals(key, other);

    key.init(200);

    assertEquals(key, other);
  }
}
