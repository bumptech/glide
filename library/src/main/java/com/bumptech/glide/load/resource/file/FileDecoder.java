package com.bumptech.glide.load.resource.file;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;

import java.io.File;
import java.io.IOException;

public class FileDecoder implements ResourceDecoder<File, File> {

    @Override
    public Resource<File> decode(File source, int width, int height) throws IOException {
        return new FileResource(source);
    }

    @Override
    public String getId() {
        return "";
    }
}
