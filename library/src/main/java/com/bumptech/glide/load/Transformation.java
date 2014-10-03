package com.bumptech.glide.load;

import com.bumptech.glide.load.engine.Resource;

/**
 * A class for performing an arbitrary transformation on a resource.
 *
 * @param <T> The type of the resource being transformed.
 */
public interface Transformation<T> {

    /**
     * Transforms the given resource and returns the transformed resource.
     *
     * <p>
     *     Note - If the original resource object is not returned, the original resource will be recycled and it's
     *     internal resources may be reused. This means it is not safe to rely on the original resource or any internal
     *     state of the original resource in any new resource that is created. Usually this shouldn't occur, but if
     *     absolutely necessary either the original resource object can be returned with modified internal state, or
     *     the data in the original resource can be copied into the transformed resource.
     * </p>
     *
     * @param resource The resource to transform.
     * @param outWidth The width of the view or target the resource will be displayed in.
     * @param outHeight The height of the view or target the resource will be displayed in.
     * @return The transformed resource.
     */
    Resource<T> transform(Resource<T> resource, int outWidth, int outHeight);

    /**
     * A method to get a unique identifier for this particular transformation that can be used as part of a cache key.
     * The fully qualified class name for this class is appropriate if written out, but getClass().getName() is not
     * because the name may be changed by proguard.
     *
     * <p>
     *     If this transformation does not affect the data that will be stored in cache, returning an empty string here
     *     is acceptable.
     * </p>
     *
     * @return A string that uniquely identifies this transformation.
     */
    String getId();
}
