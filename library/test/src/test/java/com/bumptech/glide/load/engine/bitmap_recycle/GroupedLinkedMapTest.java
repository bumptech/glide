package com.bumptech.glide.load.engine.bitmap_recycle;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class GroupedLinkedMapTest {

  private GroupedLinkedMap<Key, Object> map;

  @Before
  public void setUp() {
    map = new GroupedLinkedMap<>();
  }

  @Test
  public void testReturnsNullForGetWithNoBitmap() {
    Key key = new Key("key", /* width= */ 1, /* height= */ 1);
    assertNull(map.get(key));
  }

  @Test
  public void testCanAddAndRemoveABitmap() {
    Key key = new Key("key", 1, 1);
    Object expected = new Object();

    map.put(key, expected);

    assertThat(map.get(key)).isEqualTo(expected);
  }

  @Test
  public void testCanAddAndRemoveMoreThanOneBitmapForAGivenKey() {
    Key key = new Key("key", 1, 1);
    Integer value = 20;

    int numToAdd = 10;

    for (int i = 0; i < numToAdd; i++) {
      map.put(key, value);
    }

    for (int i = 0; i < numToAdd; i++) {
      assertThat(map.get(key)).isEqualTo(value);
    }
  }

  @Test
  public void testLeastRecentlyRetrievedKeyIsLeastRecentlyUsed() {
    Key firstKey = new Key("key", 1, 1);
    Integer firstValue = 10;
    map.put(firstKey, firstValue);
    map.put(firstKey, firstValue);

    Key secondKey = new Key("key", 2, 2);
    Integer secondValue = 20;
    map.put(secondKey, secondValue);

    map.get(firstKey);

    assertThat(map.removeLast()).isEqualTo(secondValue);
  }

  @Test
  public void testAddingAnEntryDoesNotMakeItMostRecentlyUsed() {
    Key firstKey = new Key("key", 1, 1);
    Integer firstValue = 10;

    map.put(firstKey, firstValue);
    map.put(firstKey, firstValue);

    map.get(firstKey);

    Integer secondValue = 20;
    map.put(new Key("key", 2, 2), secondValue);

    assertThat(map.removeLast()).isEqualTo(secondValue);
  }

  private static final class Key implements Poolable {

    private final String key;
    private final int width;
    private final int height;

    Key(String key, int width, int height) {
      this.key = key;
      this.width = width;
      this.height = height;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Key) {
        Key other = (Key) o;
        return key.equals(other.key) && width == other.width && height == other.height;
      }
      return false;
    }

    @Override
    public int hashCode() {
      int result = key != null ? key.hashCode() : 0;
      result = 31 * result + width;
      result = 31 * result + height;
      return result;
    }

    @Override
    public void offer() {
      // Do nothing.
    }
  }
}
