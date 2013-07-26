package com.bumptech.glide.loader.model;

import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.loader.stream.LocalUriLoader;
import com.bumptech.glide.loader.stream.StreamLoader;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 7/25/13
 * Time: 11:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class DrawableLoader implements ModelLoader<Integer> {
    private final Context context;

    public DrawableLoader(Context context) {
        this.context = context;
    }

    @Override
    public StreamLoader getStreamOpener(Integer model, int width, int height) {
        Uri uri = Uri.parse("android.resource://" + context.getPackageName() + "/" + model.toString());
        return new LocalUriLoader(context, uri);
    }

    @Override
    public String getId(Integer model) {
        return model.toString();
    }

    @Override
    public void clear() { }
}
