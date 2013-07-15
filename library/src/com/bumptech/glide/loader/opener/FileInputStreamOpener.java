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
    private final String path;

    public FileInputStreamOpener(String path) {
        this.path = path;
        this.file = null;
    }

    public FileInputStreamOpener(File file) {
        this.file = file;
        this.path = null;
    }

    @Override
    public InputStream openStream() throws IOException {
        return new FileInputStream(file != null ? file : new File(path));
    }

    @Override
    public void cleanup() { }
}
