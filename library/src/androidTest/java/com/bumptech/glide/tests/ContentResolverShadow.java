package com.bumptech.glide.tests;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.HashMap;
import java.util.Map;

@Implements(ContentResolver.class)
public class ContentResolverShadow extends ShadowContentResolver {
  private Map<Uri, AssetFileDescriptor> fileDescriptorMap = new HashMap<>();

  public void registerFileDescriptor(Uri uri, AssetFileDescriptor fileDescriptor) {
    fileDescriptorMap.put(uri, fileDescriptor);
  }

  @Implementation
  public AssetFileDescriptor openAssetFileDescriptor(Uri uri, String mode) {
    AssetFileDescriptor fileDescriptor = fileDescriptorMap.get(uri);
    if (fileDescriptor != null) {
      return fileDescriptor;
    } else {
      return null;
    }
  }
}
