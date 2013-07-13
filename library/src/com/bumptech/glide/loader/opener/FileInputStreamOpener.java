package com.bumptech.glide.loader.opener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 7/10/13
 * Time: 11:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileInputStreamOpener implements StreamOpener {
    private final File file;

    public FileInputStreamOpener(String path) {
        this(new File(path));
    }

    public FileInputStreamOpener(File file) {
        this.file = file;
    }

    @Override
    public InputStream openStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public void cleanup() { }
}
