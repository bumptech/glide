package com.bumptech.glide.integration.cronet;

import java.nio.ByteBuffer;

/**
 * Parses a {@link java.nio.ByteBuffer} to a particular data type.
 *
 * @param <T> The type of data to parse the buffer to.
 */
interface ByteBufferParser<T> {
  /** Returns the required type of data parsed from the given {@link ByteBuffer}. */
  T parse(ByteBuffer byteBuffer);
  /** Returns the {@link Class} of the data that will be parsed from {@link ByteBuffer}s. */
  Class<T> getDataClass();
}
