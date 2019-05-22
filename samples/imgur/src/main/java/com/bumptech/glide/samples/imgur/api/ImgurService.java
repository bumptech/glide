package com.bumptech.glide.samples.imgur.api;

import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;

/** Define's Imgur's API for Retrofit. */
public interface ImgurService {
  String CLIENT_ID = "36d1f6bef16370c";

  @GET("gallery/hot/viral/{page}")
  Observable<Gallery> getHotViral(@Path("page") int page);

  @GET("gallery/hot/{sort}/{page}.json")
  Observable<Gallery> getHot(@Path("sort") Sort sort, @Path("page") int page);

  @GET("gallery/{section}/{sort}/{page}.json")
  Observable<Gallery> getGallery(
      @Path("section") Section section, @Path("sort") Sort sort, @Path("page") int page);

  /** Sections that Imgur's API allows us to query from. */
  enum Section {
    hot,
    top,
    user
  }

  /** The sort order for content within a particular section. */
  enum Sort {
    viral,
    top,
    time,
    rising
  }
}
