package com.bumptech.glide.load.model;

/**
 * An interface for lazily creating headers that allows expensive to calculate headers (oauth for
 * example) to be generated in the background during the first fetch.
 *
 * <p> Implementations should implement equals() and hashcode() </p> .
 */
public interface LazyHeaderFactory {

    String buildHeader();

}
