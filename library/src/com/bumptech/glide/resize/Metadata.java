package com.bumptech.glide.resize;

public class Metadata {
    public static final Metadata DEFAULT = new Metadata(Priority.NORMAL);

    public final Priority priority;

    public Metadata(Priority priority) {
        this.priority = priority;
    }
}
