package com.bumptech.glide.samples.imgur.api;

/**
 * Represents Imgur's Image resource.
 *
 * <p>Populated automatically by GSON
 */
public final class Image {
  private String id;
  public String title;
  public String description;
  public String link;
  boolean is_album;

  @Override
  public String toString() {
    return "Image{"
        + "id='"
        + id
        + '\''
        + ", title='"
        + title
        + '\''
        + ", description='"
        + description
        + '\''
        + ", link='"
        + link
        + '\''
        + ", is_album='"
        + is_album
        + '\''
        + '}';
  }
}
