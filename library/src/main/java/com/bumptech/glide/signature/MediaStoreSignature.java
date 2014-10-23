package com.bumptech.glide.signature;

import com.bumptech.glide.load.Key;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

/**
 * A unique signature based on metadata data from the media store that detects common changes to media store files like
 * edits, rotations, and temporary file replacement.
 */
public class MediaStoreSignature implements Key {
    private final String mimeType;
    private final long dateModified;
    private final int orientation;

    /**
     * Constructor for {@link com.bumptech.glide.signature.MediaStoreSignature}.
     *
     * @param mimeType The mime type of the media store media. Ok to default to empty string "". See
     *      {@link android.provider.MediaStore.Images.ImageColumns#MIME_TYPE} or
     *      {@link android.provider.MediaStore.Video.VideoColumns#MIME_TYPE}.
     * @param dateModified The date modified time of the media store media. Ok to default to 0. See
     *      {@link android.provider.MediaStore.Images.ImageColumns#DATE_MODIFIED} or
     *      {@link android.provider.MediaStore.Video.VideoColumns#DATE_MODIFIED}.
     * @param orientation The orientation of the media store media. Ok to default to 0. See
     *      {@link android.provider.MediaStore.Images.ImageColumns#ORIENTATION}.
     */
    public MediaStoreSignature(String mimeType, long dateModified, int orientation) {
        this.mimeType = mimeType;
        this.dateModified = dateModified;
        this.orientation = orientation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MediaStoreSignature that = (MediaStoreSignature) o;

        if (dateModified != that.dateModified) {
            return false;
        }
        if (orientation != that.orientation) {
            return false;
        }
        if (mimeType != null ? !mimeType.equals(that.mimeType) : that.mimeType != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = mimeType != null ? mimeType.hashCode() : 0;
        result = 31 * result + (int) (dateModified ^ (dateModified >>> 32));
        result = 31 * result + orientation;
        return result;
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) throws UnsupportedEncodingException {
        byte[] data = ByteBuffer.allocate(12)
                .putLong(dateModified)
                .putInt(orientation)
                .array();
        messageDigest.update(data);
        messageDigest.update(mimeType.getBytes(STRING_CHARSET_NAME));
    }
}
