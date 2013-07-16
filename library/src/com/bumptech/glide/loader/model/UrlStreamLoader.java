package com.bumptech.glide.loader.model;

import com.bumptech.glide.loader.opener.HttpInputStreamOpener;
import com.bumptech.glide.loader.opener.StreamOpener;

import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 7/16/13
 * Time: 12:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class UrlStreamLoader extends DirectModelStreamLoader<URL> {

    @Override
    protected StreamOpener getStreamOpener(URL model, int width, int height) {
        return new HttpInputStreamOpener(model);
    }

    @Override
    protected String getId(URL model) {
        return model.toString();
    }
}
