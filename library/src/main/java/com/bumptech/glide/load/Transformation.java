package com.bumptech.glide.load;

import com.bumptech.glide.Resource;

/**
 * A class for performing an arbitrary transformation on a bitmap
 * @param <T> The type of the resource being transformed.
 */
public interface Transformation<T> {

    /**
     * A noop Transformation that simply returns the given bitmap
     */
    public static Transformation NONE = new Transformation() {
        @Override
        public Resource transform(Resource resource, int outWidth, int outHeight) {
            return resource;
        }

        @Override
        public String getId() {
            return "NONE.com.bumptech.glide.load.Transformation";
        }
    };


    /**
     * Transform the given bitmap. It is also acceptable to simply return the given bitmap if no transformation is
     * required.
     *
     * @param resource The resource to transform
     * @param outWidth The width of the view or target the bitmap will be displayed in
     * @param outHeight The height of the view or target the bitmap will be displayed in
     * @return The transformed bitmap
     */
    public abstract Resource<T> transform(Resource<T> resource, int outWidth, int outHeight);

    /**
     * A method to get a unique identifier for this particular transformation that can be used as part of a cache key.
     * The fully qualified class name for this class is appropriate if written out, but getClass().getName() is not
     * because the name may be changed by proguard.
     *
     * @return A string that uniquely identifies this transformation from other transformations
     */
    public String getId();
}
