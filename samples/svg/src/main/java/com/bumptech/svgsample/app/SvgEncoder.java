package com.bumptech.svgsample.app;

import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.Resource;

import java.io.IOException;
import java.io.OutputStream;

public class SvgEncoder implements ResourceEncoder<Svg> {
    @Override
    public boolean encode(Resource<Svg> data, OutputStream os) {
        try {
            os.write(data.get().toBytes());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public String getId() {
        // Or if you have options (compression quality etc) add that to the key here.
        return "";
    }
}
