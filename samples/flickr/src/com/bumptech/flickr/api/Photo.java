package com.bumptech.flickr.api;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/6/13
 * Time: 10:55 AM
 * To change this template use File | Settings | File Templates.
 */
public class Photo {
    public final String id;
    public final String owner;
    public final String title;
    public final String server;
    public final String farm;
    public final String secret;
    private String partialUrl = null;

    public Photo(JSONObject jsonPhoto) throws JSONException {
        this.id = jsonPhoto.getString("id");
        this.owner = jsonPhoto.getString("owner");
        this.title = jsonPhoto.optString("title", "");
        this.server = jsonPhoto.getString("server");
        this.farm = jsonPhoto.getString("farm");
        this.secret = jsonPhoto.getString("secret");
    }

    public String getPartialUrl() {
        if (partialUrl == null ) {
            partialUrl = Api.getCacheableUrl(this);
        }
        return partialUrl;
    }

}
