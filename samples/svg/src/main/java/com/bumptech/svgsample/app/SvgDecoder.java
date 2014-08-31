package com.bumptech.svgsample.app;

import android.annotation.SuppressLint;
import android.os.Build;
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
    @SuppressLint("NewApi")
    public Resource<SVG> decode(InputStream source, int width, int height) throws IOException {
        try {
            SVG svg = SVG.getFromInputStream(source);
            return new SimpleResource<SVG>(svg);
        } catch (SVGParseException ex) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
                throw new IOException(ex.toString());
            }
            throw new IOException("Cannot load SVG from stream", ex);
        }
    }

    @Override
    public String getId() {
        return "";
    }
}
