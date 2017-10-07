package com.bumptech.glide.load.resource.drawable;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.Nullable;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import java.io.IOException;
import java.util.List;

/**
 * Decodes {@link Drawable}s given resource {@link Uri}s in the form
 * android.resource://<package_name>/<type>/<name>.
 */
public class ResourceDrawableDecoder implements ResourceDecoder<Uri, Drawable> {
  private static final int EXPECTED_PATH_SEGMENTS = 2;
  private static final int TYPE_PATH_SEGMENT_INDEX = 0;
  private static final int NAME_PATH_SEGMENT_INDEX = 1;

  private final Context context;

  public ResourceDrawableDecoder(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public boolean handles(Uri source, Options options) throws IOException {
    return source.getScheme().equals(ContentResolver.SCHEME_ANDROID_RESOURCE);
  }

  @Nullable
  @Override
  public Resource<Drawable> decode(Uri source, int width, int height, Options options)
      throws IOException {
    // Parsing is based on the logic in ResourceLoader/the android framework that constructs
    // resource Uris.
    List<String> segments = source.getPathSegments();
    if (segments.size() != EXPECTED_PATH_SEGMENTS) {
      throw new IOException("Unexpected path segments for: " + source + " segments: " + segments);
    }
    String packageName = source.getAuthority();
    String typeName = segments.get(TYPE_PATH_SEGMENT_INDEX);
    String resourceName = segments.get(NAME_PATH_SEGMENT_INDEX);
    int id = context.getResources().getIdentifier(resourceName, typeName, packageName);
    Drawable drawable = DrawableDecoderCompat.getDrawable(context, id, null /*theme*/);
    if (drawable == null) {
      throw new IOException("ContextCompat#getDrawable returned null for: " + source);
    }
    return new DrawableResource<Drawable>(drawable) {
      @SuppressWarnings("unchecked")
      @Override
      public Class<Drawable> getResourceClass() {
        return (Class<Drawable>) drawable.getClass();
      }

      @Override
      public int getSize() {
        return 1;
      }

      @Override
      public void recycle() {
        // Do nothing.
      }
    };
  }
}
