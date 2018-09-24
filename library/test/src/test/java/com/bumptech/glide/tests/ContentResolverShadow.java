package com.bumptech.glide.tests;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(ContentResolver.class)
public class ContentResolverShadow {
  private final Map<Uri, AssetFileDescriptor> fileDescriptorMap = new HashMap<>();
  private final Map<Uri, InputStream> inputStreamMap = new HashMap<>();

  public void registerFileDescriptor(Uri uri, AssetFileDescriptor fileDescriptor) {
    fileDescriptorMap.put(uri, fileDescriptor);
  }

  public void registerInputStream(Uri uri, InputStream inputStream) {
    inputStreamMap.put(uri, inputStream);
  }

  @Implementation
  public InputStream openInputStream(Uri uri) {
    return inputStreamMap.get(uri);
  }

  @Implementation
  public AssetFileDescriptor openAssetFileDescriptor(Uri uri, String mode) {
    return fileDescriptorMap.get(uri);
  }
}
