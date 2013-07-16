package com.bumptech.glide.loader.model;

import com.bumptech.glide.loader.opener.FileInputStreamOpener;
import com.bumptech.glide.loader.opener.StreamOpener;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 7/16/13
 * Time: 12:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileStreamLoader extends DirectModelStreamLoader<File> {

    @Override
    protected StreamOpener getStreamOpener(File model, int width, int height) {
        return new FileInputStreamOpener(model);
    }

    @Override
    protected String getId(File model) {
        return model.getAbsolutePath();
    }
}
