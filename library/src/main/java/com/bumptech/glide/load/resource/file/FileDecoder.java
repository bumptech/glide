package com.bumptech.glide.load.resource.file;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;

import java.io.File;
import java.util.Map;

/**
 * A simple {@link com.bumptech.glide.load.ResourceDecoder} that creates resource for a given {@link
 * java.io.File}.
 */
public class FileDecoder implements ResourceDecoder<File, File> {

  @Override
  public boolean handles(File source) {
    return true;
  }

  @Override
  public Resource<File> decode(File source, int width, int height, Map<String, Object> options) {
    return new FileResource(source);
  }
}
