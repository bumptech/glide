package com.bumptech.glide.mocks;

import static org.mockito.Mockito.mock;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;

/**
 * Mocks for various builder patterns in Glide to make testing easier.
 *
 * <p>All methods share the same behavior. Any method on the builder that returns the builder itself
 * will default to returning the mock rather than null. Any method on the builder that returns
 * anything other than the builder will return Mockito's standard default return value.
 */
public final class MockGlideBuilders {

  private MockGlideBuilders() {}

  /** Creates a new {@link RequestBuilder} instance with a matching resource type. */
  @SuppressWarnings("unchecked")
  public static <T> RequestBuilder<T> mockRequestBuilder() {
    return (RequestBuilder<T>) mockGlideRequest(RequestBuilder.class);
  }

  /** Creates a new instance of a generated {@code GlideRequest} class for an application. */
  // The suppressions allow callers to get a typed class without warnings in their test code.
  @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
  public static <T, Y extends RequestBuilder<T>> Y mockGlideRequest(Class<?> glideRequest) {
    return (Y) mock(glideRequest, new AnswerSelf<Y>());
  }

  /** Creates a new {@link RequestOptions} instance. */
  public static RequestOptions mockRequestOptions() {
    return mockGlideOptions(RequestOptions.class);
  }

  /** Creates a new instance of a generated {@code GlideOptions} class for an application. */
  public static <T extends RequestOptions> T mockGlideOptions(Class<T> glideOptionsClass) {
    return mock(glideOptionsClass, new AnswerSelf<T>());
  }
}
