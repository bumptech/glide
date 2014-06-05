package com.bumptech.glide.load.resource.bitmap;

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the &quot;License&quot;); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps an existing {@link InputStream} and &lt;em&gt;buffers&lt;/em&gt; the input.
 * Expensive interaction with the underlying input stream is minimized, since
 * most (smaller) requests can be satisfied by accessing the buffer alone. The
 * drawback is that some extra space is required to hold the buffer and that
 * copying takes place when filling that buffer, but this is usually outweighed
 * by the performance benefits.
 *
 * &lt;p/&gt;A typical application pattern for the class looks like this:&lt;p/&gt;
 *
 * &lt;pre&gt;
 * BufferedInputStream buf = new BufferedInputStream(new FileInputStream(&amp;quot;file.java&amp;quot;));
 * &lt;/pre&gt;
 *
 */
public class RecyclableBufferedInputStream extends FilterInputStream {

    public static class InvalidMarkException extends RuntimeException {
        public InvalidMarkException(String detailMessage) {
            super(detailMessage);
        }
    }

    /**
     * The buffer containing the current bytes read from the target InputStream.
     */
    protected volatile byte[] buf;

    /**
     * The total number of bytes inside the byte array {@code buf}.
     */
    protected int count;

    /**
     * The current limit, which when passed, invalidates the current mark.
     */
    protected int marklimit;

    /**
     * The currently marked position. -1 indicates no mark has been set or the
     * mark has been invalidated.
     */
    protected int markpos = -1;

    /**
     * The current position within the byte array {@code buf}.
     */
    protected int pos;

    public RecyclableBufferedInputStream(InputStream in, byte[] buffer) {
        super(in);
        if (buffer == null || buffer.length == 0) {
            throw new IllegalArgumentException("buffer is null or empty");
        }
        buf = buffer;
    }

    /**
     * Returns an estimated number of bytes that can be read or skipped without blocking for more
     * input. This method returns the number of bytes available in the buffer
     * plus those available in the source stream, but see {@link InputStream#available} for
     * important caveats.
     *
     * @return the estimated number of bytes available
     * @throws IOException if this stream is closed or an error occurs
     */
    @Override
    public synchronized int available() throws IOException {
        InputStream localIn = in; // &#39;in&#39; could be invalidated by close()
        if (buf == null || localIn == null) {
            throw streamClosed();
        }
        return count - pos + localIn.available();
    }

    private IOException streamClosed() throws IOException {
        throw new IOException("BufferedInputStream is closed");
    }

    /**
     * Closes this stream. The source stream is closed and any resources
     * associated with it are released.
     *
     * @throws IOException
     *             if an error occurs while closing this stream.
     */
    @Override
    public void close() throws IOException {
        buf = null;
        InputStream localIn = in;
        in = null;
        if (localIn != null) {
            localIn.close();
        }
    }

    private int fillbuf(InputStream localIn, byte[] localBuf)
            throws IOException {
        if (markpos == -1 || (pos - markpos >= marklimit)) {
            /* Mark position not set or exceeded readlimit */
            int result = localIn.read(localBuf);
            if (result > 0) {
                markpos = -1;
                pos = 0;
                count = result == -1 ? 0 : result;
            }
            return result;
        }
        //Added count == localBuf.length so that we do not immediately double the buffer size before reading any data
        // when marklimit > localBuf.length. Instead, we will double the buffer size only after reading the initial
        // localBuf worth of data without finding what we're looking for in the stream. This allows us to set a
        // relatively small initial buffer size and a large marklimit for safety without causing an allocation each time
        // read is called.
        if (markpos == 0 && marklimit > localBuf.length && count == localBuf.length) {
            /* Increase buffer size to accommodate the readlimit */
            int newLength = localBuf.length * 2;
            if (newLength > marklimit) {
                newLength = marklimit;
            }
            byte[] newbuf = new byte[newLength];
            System.arraycopy(localBuf, 0, newbuf, 0, localBuf.length);
            // Reassign buf, which will invalidate any local references
            // FIXME: what if buf was null?
            localBuf = buf = newbuf;
        } else if (markpos > 0) {
            System.arraycopy(localBuf, markpos, localBuf, 0, localBuf.length
                    - markpos);
        }
        /* Set the new position and mark position */
        pos -= markpos;
        count = markpos = 0;
        int bytesread = localIn.read(localBuf, pos, localBuf.length - pos);
        count = bytesread <= 0 ? pos : pos + bytesread;
        return bytesread;
    }

    /**
     * Sets a mark position in this stream. The parameter {@code readlimit}
     * indicates how many bytes can be read before a mark is invalidated.
     * Calling {@code reset()} will reposition the stream back to the marked
     * position if {@code readlimit} has not been surpassed. The underlying
     * buffer may be increased in size to allow {@code readlimit} number of
     * bytes to be supported.
     *
     * @param readlimit
     *            the number of bytes that can be read before the mark is
     *            invalidated.
     * @see #reset()
     */
    @Override
    public synchronized void mark(int readlimit) {
        //This is stupid, but BitmapFactory.decodeStream calls mark(1024)
        //which is too small for a substantial portion of images. This
        //change (using Math.max) ensures that we don't overwrite readlimit
        //with a smaller value
        marklimit = Math.max(marklimit, readlimit);
        markpos = pos;
    }

    public synchronized void clearMark() {
        markpos = -1;
        marklimit = 0;
    }

    /**
     * Indicates whether {@code BufferedInputStream} supports the {@code mark()}
     * and {@code reset()} methods.
     *
     * @return {@code true} for BufferedInputStreams.
     * @see #mark(int)
     * @see #reset()
     */
    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * Reads a single byte from this stream and returns it as an integer in the
     * range from 0 to 255. Returns -1 if the end of the source string has been
     * reached. If the internal buffer does not contain any available bytes then
     * it is filled from the source stream and the first byte is returned.
     *
     * @return the byte read or -1 if the end of the source stream has been
     *         reached.
     * @throws IOException
     *             if this stream is closed or another IOException occurs.
     */
    @Override
    public synchronized int read() throws IOException {
        // Use local refs since buf and in may be invalidated by an
        // unsynchronized close()
        byte[] localBuf = buf;
        InputStream localIn = in;
        if (localBuf == null || localIn == null) {
            throw streamClosed();
        }

        /* Are there buffered bytes available? */
        if (pos >= count && fillbuf(localIn, localBuf) == -1) {
            return -1; /* no, fill buffer */
        }
        // localBuf may have been invalidated by fillbuf
        if (localBuf != buf) {
            localBuf = buf;
            if (localBuf == null) {
                throw streamClosed();
            }
        }

        /* Did filling the buffer fail with -1 (EOF)? */
        if (count - pos > 0) {
            return localBuf[pos++] & 0xFF;
        }
        return -1;
    }

    /**
     * Reads at most {@code byteCount} bytes from this stream and stores them in
     * byte array {@code buffer} starting at offset {@code offset}. Returns the
     * number of bytes actually read or -1 if no bytes were read and the end of
     * the stream was encountered. If all the buffered bytes have been used, a
     * mark has not been set and the requested number of bytes is larger than
     * the receiver&#39;s buffer size, this implementation bypasses the buffer and
     * simply places the results directly into {@code buffer}.
     *
     * @param buffer
     *            the byte array in which to store the bytes read.
     * @return the number of bytes actually read or -1 if end of stream.
     * @throws IndexOutOfBoundsException
     *             if {@code offset &lt; 0} or {@code byteCount &lt; 0}, or if
     *             {@code offset + byteCount} is greater than the size of
     *             {@code buffer}.
     * @throws IOException
     *             if the stream is already closed or another IOException
     *             occurs.
     */
    @Override
    public synchronized int read(byte[] buffer, int offset, int byteCount) throws IOException {
        // Use local ref since buf may be invalidated by an unsynchronized
        // close()
        byte[] localBuf = buf;
        if (localBuf == null) {
            throw streamClosed();
        }
        //Arrays.checkOffsetAndCount(buffer.length, offset, byteCount);
        if (byteCount == 0) {
            return 0;
        }
        InputStream localIn = in;
        if (localIn == null) {
            throw streamClosed();
        }

        int required;
        if (pos < count) {
            /* There are bytes available in the buffer. */
            int copylength = count - pos >= byteCount ? byteCount : count - pos;
            System.arraycopy(localBuf, pos, buffer, offset, copylength);
            pos += copylength;
            if (copylength == byteCount || localIn.available() == 0) {
                return copylength;
            }
            offset += copylength;
            required = byteCount - copylength;
        } else {
            required = byteCount;
        }

        while (true) {
            int read;
            /*
             * If we&#39;re not marked and the required size is greater than the
             * buffer, simply read the bytes directly bypassing the buffer.
             */
            if (markpos == -1 && required >= localBuf.length) {
                read = localIn.read(buffer, offset, required);
                if (read == -1) {
                    return required == byteCount ? -1 : byteCount - required;
                }
            } else {
                if (fillbuf(localIn, localBuf) == -1) {
                    return required == byteCount ? -1 : byteCount - required;
                }
                // localBuf may have been invalidated by fillbuf
                if (localBuf != buf) {
                    localBuf = buf;
                    if (localBuf == null) {
                        throw streamClosed();
                    }
                }

                read = count - pos >= required ? required : count - pos;
                System.arraycopy(localBuf, pos, buffer, offset, read);
                pos += read;
            }
            required -= read;
            if (required == 0) {
                return byteCount;
            }
            if (localIn.available() == 0) {
                return byteCount - required;
            }
            offset += read;
        }
    }

    /**
     * Resets this stream to the last marked location.
     *
     * @throws IOException
     *             if this stream is closed, no mark has been set or the mark is
     *             no longer valid because more than {@code readlimit} bytes
     *             have been read since setting the mark.
     * @see #mark(int)
     */
    @Override
    public synchronized void reset() throws IOException {
        if (buf == null) {
            throw new IOException("Stream is closed");
        }
        if (-1 == markpos) {
            throw new InvalidMarkException("Mark has been invalidated");
        }
        pos = markpos;
    }

    /**
     * Skips {@code byteCount} bytes in this stream. Subsequent calls to
     * {@code read} will not return these bytes unless {@code reset} is
     * used.
     *
     * @param byteCount
     *            the number of bytes to skip. {@code skip} does nothing and
     *            returns 0 if {@code byteCount} is less than zero.
     * @return the number of bytes actually skipped.
     * @throws IOException
     *             if this stream is closed or another IOException occurs.
     */
    @Override
    public synchronized long skip(long byteCount) throws IOException {
        // Use local refs since buf and in may be invalidated by an
        // unsynchronized close()
        byte[] localBuf = buf;
        InputStream localIn = in;
        if (localBuf == null) {
            throw streamClosed();
        }
        if (byteCount < 1) {
            return 0;
        }
        if (localIn == null) {
            throw streamClosed();
        }

        if (count - pos >= byteCount) {
            pos += byteCount;
            return byteCount;
        }
        long read = count - pos;
        pos = count;

        if (markpos != -1) {
            if (byteCount <= marklimit) {
                if (fillbuf(localIn, localBuf) == -1) {
                    return read;
                }
                if (count - pos >= byteCount - read) {
                    pos += byteCount - read;
                    return byteCount;
                }
                // Couldn&#39;t get all the bytes, skip what we read
                read += (count - pos);
                pos = count;
                return read;
            }
        }
        return read + localIn.skip(byteCount - read);
    }
}
