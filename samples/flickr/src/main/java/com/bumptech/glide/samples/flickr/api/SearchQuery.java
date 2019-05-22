package com.bumptech.glide.samples.flickr.api;

import android.os.Parcel;

/** Wraps a search query string. */
public final class SearchQuery implements Query {
  public static final Creator<SearchQuery> CREATOR =
      new Creator<SearchQuery>() {
        @Override
        public SearchQuery createFromParcel(Parcel source) {
          return new SearchQuery(source);
        }

        @Override
        public SearchQuery[] newArray(int size) {
          return new SearchQuery[size];
        }
      };

  private final String queryString;

  public SearchQuery(String queryString) {
    this.queryString = queryString;
  }

  private SearchQuery(Parcel in) {
    queryString = in.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(queryString);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public String getDescription() {
    return queryString;
  }

  @Override
  public String getUrl() {
    return Api.getSearchUrl(queryString);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof SearchQuery) {
      SearchQuery other = (SearchQuery) o;
      return queryString.equals(other.queryString);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return queryString.hashCode();
  }
}
