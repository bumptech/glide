package com.bumptech.glide.load.model;

import android.os.ParcelFileDescriptor;
import com.bumptech.glide.load.Encoder;

import java.io.InputStream;
import java.io.OutputStream;

public class ImageVideoWrapperEncoder implements Encoder<ImageVideoWrapper> {
    private final Encoder<InputStream> streamEncoder;
    private final Encoder<ParcelFileDescriptor> fileDescriptorEncoder;
    private String id;

    public ImageVideoWrapperEncoder(Encoder<InputStream> streamEncoder,
            Encoder<ParcelFileDescriptor> fileDescriptorEncoder) {
        this.streamEncoder = streamEncoder;
        this.fileDescriptorEncoder = fileDescriptorEncoder;
    }

    @Override
    public boolean encode(ImageVideoWrapper data, OutputStream os) {
        if (data.getStream() != null) {
            return streamEncoder.encode(data.getStream(), os);
        } else {
            return fileDescriptorEncoder.encode(data.getFileDescriptor(), os);
        }
    }

    @Override
    public String getId() {
        if (id == null) {
            id = streamEncoder.getId() + fileDescriptorEncoder.getId();
        }
        return id;
    }
}
