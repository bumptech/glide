package com.bumptech.glide.samples.flickr.api;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A POJO representing a JSON object returned from Flickr's api representing a single image.
 */
public class Photo {
    private final String id;
    private final String owner;
    private final String title;
    private final String server;
    private final String farm;
    private final String secret;
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
        if (partialUrl == null) {
            partialUrl = Api.getCacheableUrl(this);
        }
        return partialUrl;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getServer() {
        return server;
    }

    public String getFarm() {
        return farm;
    }

    public String getSecret() {
        return secret;
    }

    @Override
    public String toString() {
        return getPartialUrl();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Photo photo = (Photo) o;

        if (!farm.equals(photo.farm)) {
            return false;
        }
        if (!id.equals(photo.id)) {
            return false;
        }
        if (!owner.equals(photo.owner)) {
            return false;
        }
        if (!secret.equals(photo.secret)) {
            return false;
        }
        if (!server.equals(photo.server)) {
            return false;
        }
        if (!title.equals(photo.title)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + owner.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + server.hashCode();
        result = 31 * result + farm.hashCode();
        result = 31 * result + secret.hashCode();
        return result;
    }
}
