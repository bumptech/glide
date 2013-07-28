package com.bumptech.glide.loader.model;

import android.content.Context;
import com.android.volley.RequestQueue;

import java.net.URL;

/**
 * A simple model loader for urls
 */
public class UrlLoader extends VolleyModelLoader<URL> {

    public UrlLoader(Context context) {
        super(context);
    }

    @SuppressWarnings("unused")
    public UrlLoader(RequestQueue requestQueue) {
        super(requestQueue);
    }

    @Override
    protected String getUrl(URL model, int width, int height) {
        return model.toString();
    }

    //this may need to be overridden if multiple urls can be used to retrieve the same image
    @Override
    public String getId(URL model) {
        return model.toString();
    }
}
