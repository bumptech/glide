package com.bumptech.glide.mocks;

import static org.mockito.Mockito.RETURNS_DEFAULTS;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Useful default when mocking {@link com.bumptech.glide.request.RequestOptions} or {@link
 * com.bumptech.glide.RequestBuilder}.
 *
 * @param <T> The type of the options and/or builder.
 */
final class AnswerSelf<T> implements Answer<T> {

  @SuppressWarnings("unchecked")
  @Override
  public T answer(InvocationOnMock invocation) throws Throwable {
    Object mock = invocation.getMock();
    if (invocation.getMethod().getReturnType().isInstance(mock)) {
      return (T) mock;
    } else {
      return (T) RETURNS_DEFAULTS.answer(invocation);
    }
  }
}
