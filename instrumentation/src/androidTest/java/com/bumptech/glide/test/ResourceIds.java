package com.bumptech.glide.test;

import android.content.Context;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;

/**
 * Internally in google we don't appear to be able to reference resource ids directly, this class is
 * a hack around that until we figure out what's going wrong.
 */
public final class ResourceIds {
  private ResourceIds() {
    // Utility class.
  }

  public interface raw {
    int dl_world_anim = getResourceId("raw", "dl_world_anim");
    int canonical = getResourceId("raw", "canonical");
  }

  public interface drawable {
    int bitmap_alias = getResourceId("drawable", "bitmap_alias");
    int googlelogo_color_120x44dp= getResourceId("drawable", "googlelogo_color_120x44dp");
    int shape_drawable = getResourceId("drawable", "shape_drawable");
    int state_list_drawable = getResourceId("drawable", "state_list_drawable");
    int vector_drawable = getResourceId("drawable", "vector_drawable");
  }

  private static int getResourceId(String type, String resourceName) {
    Context context = InstrumentationRegistry.getTargetContext();
    Resources res = context.getResources();
    return res.getIdentifier(resourceName, type, context.getPackageName());
  }
}
