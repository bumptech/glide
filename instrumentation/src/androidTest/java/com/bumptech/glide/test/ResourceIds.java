package com.bumptech.glide.test;

import android.content.Context;
import android.content.res.Resources;
import androidx.test.core.app.ApplicationProvider;

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
    int canonical_large = getResourceId("raw", "canonical_large");
    int canonical_png = getResourceId("raw", "canonical_png");
    int canonical_transparent_png = getResourceId("raw", "canonical_transparent_png");
    int interlaced_transparent_gif = getResourceId("raw", "interlaced_transparent_gif");
    int transparent_gif = getResourceId("raw", "transparent_gif");
    int opaque_gif = getResourceId("raw", "opaque_gif");
    int opaque_interlaced_gif = getResourceId("raw", "opaque_interlaced_gif");
    int webkit_logo_p3 = getResourceId("raw", "webkit_logo_p3");
    int video = getResourceId("raw", "video");
    int animated_webp = getResourceId("raw", "dl_world_anim_webp");
    int animated_avif = getResourceId("raw", "dl_world_anim_avif");
  }

  public interface drawable {
    int bitmap_alias = getResourceId("drawable", "bitmap_alias");
    int googlelogo_color_120x44dp = getResourceId("drawable", "googlelogo_color_120x44dp");
    int shape_drawable = getResourceId("drawable", "shape_drawable");
    int state_list_drawable = getResourceId("drawable", "state_list_drawable");
    int vector_drawable = getResourceId("drawable", "vector_drawable");
  }

  private static int getResourceId(String type, String resourceName) {
    Context context = ApplicationProvider.getApplicationContext();
    Resources res = context.getResources();
    return res.getIdentifier(resourceName, type, context.getPackageName());
  }
}
