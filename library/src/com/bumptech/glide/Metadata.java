package com.bumptech.glide;

import com.bumptech.glide.load.DecodeFormat;

public class Metadata {
    public static final Metadata DEFAULT = new Metadata(Priority.NORMAL, DecodeFormat.PREFER_RGB_565);

    private final Priority priority;
    private final DecodeFormat decodeFormat;

    public Metadata(Priority priority, DecodeFormat decodeFormat) {
        if (priority == null) {
            throw new NullPointerException("priority must not be null");
        }
        if (decodeFormat == null) {
            throw new NullPointerException("decodeFormat must not be null");
        }
        this.priority = priority;
        this.decodeFormat = decodeFormat;
    }

    public Priority getPriority() {
        return priority;
    }

    public DecodeFormat getDecodeFormat() {
        return decodeFormat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Metadata metadata = (Metadata) o;

        if (decodeFormat != metadata.decodeFormat) {
            return false;
        }
        if (priority != metadata.priority) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = priority.hashCode();
        result = 31 * result + decodeFormat.hashCode();
        return result;
    }
}
