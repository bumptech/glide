package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.util.Synthetic;
import com.bumptech.glide.util.Util;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Keys {@link android.graphics.Bitmap Bitmaps} using both {@link
 * android.graphics.Bitmap#getAllocationByteCount()} and the {@link android.graphics.Bitmap.Config}
 * returned from {@link android.graphics.Bitmap#getConfig()}.
 *
 * <p>Using both the config and the byte size allows us to safely re-use a greater variety of {@link
 * android.graphics.Bitmap Bitmaps}, which increases the hit rate of the pool and therefore the
 * performance of applications. This class works around #301 by only allowing re-use of {@link
 * android.graphics.Bitmap Bitmaps} with a matching number of bytes per pixel.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
public class SizeConfigStrategy implements LruPoolStrategy {
  private static final int MAX_SIZE_MULTIPLE = 8;

  private static final Bitmap.Config[] ARGB_8888_IN_CONFIGS;

  static {
    Bitmap.Config[] result =
        new Bitmap.Config[] {
          Bitmap.Config.ARGB_8888,
          // The value returned by Bitmaps with the hidden Bitmap config.
          null,
        };
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      result = Arrays.copyOf(result, result.length + 1);
      result[result.length - 1] = Config.RGBA_F16;
    }
    ARGB_8888_IN_CONFIGS = result;
  }

  private static final Bitmap.Config[] RGBA_F16_IN_CONFIGS = ARGB_8888_IN_CONFIGS;

  // We probably could allow ARGB_4444 and RGB_565 to decode into each other, but ARGB_4444 is
  // deprecated and we'd rather be safe.
  private static final Bitmap.Config[] RGB_565_IN_CONFIGS =
      new Bitmap.Config[] {Bitmap.Config.RGB_565};
  private static final Bitmap.Config[] ARGB_4444_IN_CONFIGS =
      new Bitmap.Config[] {Bitmap.Config.ARGB_4444};
  private static final Bitmap.Config[] ALPHA_8_IN_CONFIGS =
      new Bitmap.Config[] {Bitmap.Config.ALPHA_8};

  private final KeyPool keyPool = new KeyPool();
  private final GroupedLinkedMap<Key, Bitmap> groupedMap = new GroupedLinkedMap<>();
  private final Map<Bitmap.Config, NavigableMap<Integer, Integer>> sortedSizes = new HashMap<>();

  @Override
  public void put(Bitmap bitmap) {
    int size = Util.getBitmapByteSize(bitmap);
    Key key = keyPool.get(size, bitmap.getConfig());

    groupedMap.put(key, bitmap);

    NavigableMap<Integer, Integer> sizes = getSizesForConfig(bitmap.getConfig());
    Integer current = sizes.get(key.size);
    sizes.put(key.size, current == null ? 1 : current + 1);
  }

  @Override
  @Nullable
  public Bitmap get(int width, int height, Bitmap.Config config) {
    int size = Util.getBitmapByteSize(width, height, config);
    Key bestKey = findBestKey(size, config);

    Bitmap result = groupedMap.get(bestKey);
    if (result != null) {
      // Decrement must be called before reconfigure.
      decrementBitmapOfSize(bestKey.size, result);
      result.reconfigure(width, height, config);
    }
    return result;
  }

  private Key findBestKey(int size, Bitmap.Config config) {
    Key result = keyPool.get(size, config);
    for (Bitmap.Config possibleConfig : getInConfigs(config)) {
      NavigableMap<Integer, Integer> sizesForPossibleConfig = getSizesForConfig(possibleConfig);
      Integer possibleSize = sizesForPossibleConfig.ceilingKey(size);
      if (possibleSize != null && possibleSize <= size * MAX_SIZE_MULTIPLE) {
        if (possibleSize != size
            || (possibleConfig == null ? config != null : !possibleConfig.equals(config))) {
          keyPool.offer(result);
          result = keyPool.get(possibleSize, possibleConfig);
        }
        break;
      }
    }
    return result;
  }

  @Override
  @Nullable
  public Bitmap removeLast() {
    Bitmap removed = groupedMap.removeLast();
    if (removed != null) {
      int removedSize = Util.getBitmapByteSize(removed);
      decrementBitmapOfSize(removedSize, removed);
    }
    return removed;
  }

  private void decrementBitmapOfSize(Integer size, Bitmap removed) {
    Bitmap.Config config = removed.getConfig();
    NavigableMap<Integer, Integer> sizes = getSizesForConfig(config);
    Integer current = sizes.get(size);
    if (current == null) {
      throw new NullPointerException(
          "Tried to decrement empty size"
              + ", size: "
              + size
              + ", removed: "
              + logBitmap(removed)
              + ", this: "
              + this);
    }

    if (current == 1) {
      sizes.remove(size);
    } else {
      sizes.put(size, current - 1);
    }
  }

  private NavigableMap<Integer, Integer> getSizesForConfig(Bitmap.Config config) {
    NavigableMap<Integer, Integer> sizes = sortedSizes.get(config);
    if (sizes == null) {
      sizes = new TreeMap<>();
      sortedSizes.put(config, sizes);
    }
    return sizes;
  }

  @Override
  public String logBitmap(Bitmap bitmap) {
    int size = Util.getBitmapByteSize(bitmap);
    return getBitmapString(size, bitmap.getConfig());
  }

  @Override
  public String logBitmap(int width, int height, Bitmap.Config config) {
    int size = Util.getBitmapByteSize(width, height, config);
    return getBitmapString(size, config);
  }

  @Override
  public int getSize(Bitmap bitmap) {
    return Util.getBitmapByteSize(bitmap);
  }

  @Override
  public String toString() {
    StringBuilder sb =
        new StringBuilder()
            .append("SizeConfigStrategy{groupedMap=")
            .append(groupedMap)
            .append(", sortedSizes=(");
    for (Map.Entry<Bitmap.Config, NavigableMap<Integer, Integer>> entry : sortedSizes.entrySet()) {
      sb.append(entry.getKey()).append('[').append(entry.getValue()).append("], ");
    }
    if (!sortedSizes.isEmpty()) {
      sb.replace(sb.length() - 2, sb.length(), "");
    }
    return sb.append(")}").toString();
  }

  @VisibleForTesting
  static class KeyPool extends BaseKeyPool<Key> {

    public Key get(int size, Bitmap.Config config) {
      Key result = get();
      result.init(size, config);
      return result;
    }

    @Override
    protected Key create() {
      return new Key(this);
    }
  }

  @VisibleForTesting
  static final class Key implements Poolable {
    private final KeyPool pool;

    @Synthetic int size;
    private Bitmap.Config config;

    public Key(KeyPool pool) {
      this.pool = pool;
    }

    @VisibleForTesting
    Key(KeyPool pool, int size, Bitmap.Config config) {
      this(pool);
      init(size, config);
    }

    public void init(int size, Bitmap.Config config) {
      this.size = size;
      this.config = config;
    }

    @Override
    public void offer() {
      pool.offer(this);
    }

    @Override
    public String toString() {
      return getBitmapString(size, config);
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Key) {
        Key other = (Key) o;
        return size == other.size && Util.bothNullOrEqual(config, other.config);
      }
      return false;
    }

    @Override
    public int hashCode() {
      int result = size;
      result = 31 * result + (config != null ? config.hashCode() : 0);
      return result;
    }
  }

  @Synthetic
  static String getBitmapString(int size, Bitmap.Config config) {
    return "[" + size + "](" + config + ")";
  }

  private static Bitmap.Config[] getInConfigs(Bitmap.Config requested) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (Bitmap.Config.RGBA_F16.equals(requested)) { // NOPMD - Avoid short circuiting sdk checks.
        return RGBA_F16_IN_CONFIGS;
      }
    }

    switch (requested) {
      case ARGB_8888:
        return ARGB_8888_IN_CONFIGS;
      case RGB_565:
        return RGB_565_IN_CONFIGS;
      case ARGB_4444:
        return ARGB_4444_IN_CONFIGS;
      case ALPHA_8:
        return ALPHA_8_IN_CONFIGS;
      default:
        return new Bitmap.Config[] {requested};
    }
  }
}
