package com.bumptech.glide.samples.imgur.api;

import java.util.List;

/**
 * Represents Imgur's Gallery resource.
 *
 * <p>Populated automatically by GSON.
 */
final class Gallery {
  public List<Image> data;

  @Override
  public String toString() {
    return "Gallery{" + "data=" + data + '}';
  }
}
