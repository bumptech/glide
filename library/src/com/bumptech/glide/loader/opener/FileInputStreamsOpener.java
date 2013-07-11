package com.bumptech.glide.loader.opener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 7/10/13
 * Time: 11:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileInputStreamsOpener implements StreamOpener {
    private final File file;

    public FileInputStreamsOpener(String path) {
        this(new File(path));
    }

    public FileInputStreamsOpener(File file) {
        this.file = file;
    }

    @Override
    public Streams openStreams() throws IOException {
        return new Streams(new FileInputStream(file), new FileInputStream(file));
    }

    @Override
    public void cleanup() { }
}
