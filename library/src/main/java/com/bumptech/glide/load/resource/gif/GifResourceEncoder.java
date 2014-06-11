package com.bumptech.glide.load.resource.gif;

import android.util.Log;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.ResourceEncoder;

import java.io.IOException;
import java.io.OutputStream;

public class GifResourceEncoder implements ResourceEncoder<GifData> {
    private static final String TAG = "GifEncoder";
    @Override
    public boolean encode(Resource<GifData> resource, OutputStream os) {
        boolean result = true;
        try {
            os.write(resource.get().getData());
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Failed to encode gif", e);
            }
            result = false;
        }
        return result;
    }

    @Override
    public String getId() {
        return "GifEncoder.com.bumptech.glide.load.resource.gif";
    }
}
