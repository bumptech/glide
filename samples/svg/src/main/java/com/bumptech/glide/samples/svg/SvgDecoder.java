package com.bumptech.glide.samples.svg;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Decodes an SVG internal representation from an {@link InputStream}.
 */
public class SvgDecoder implements ResourceDecoder<InputStream, SVG> {

  @Override
  public boolean handles(InputStream source, Options options) throws IOException {
    // TODO: Can we tell?
    return true;
  }

  public Resource<SVG> decode(InputStream source, int width, int height, Options options)
      throws IOException {
    try {
      SVG svg = SVG.getFromInputStream(source);
      return new SimpleResource<SVG>(svg);
    } catch (SVGParseException ex) {
      throw new IOException("Cannot load SVG from stream", ex);
    }
  }
}
