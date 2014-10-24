package com.bumptech.glide.request.target;

/**
 * A simple {@link com.bumptech.glide.request.target.Target} base class with default (usually noop) implementations
 * of non essential methods that allows the caller to specify an exact width/height. Typicaly use cases look something
 * like this:
 * <pre>
 * <code>
 * Glide.load("http://somefakeurl.com/fakeImage.jpeg")
 *      .asBitmap()
 *      .fitCenter()
 *      .into(new SimpleTarget<Bitmap>(250, 250) {
 *
 *          {@literal @Override}
 *          public void onResourceReady(Bitmap resource, GlideAnimation<Bitmap> glideAnimation) {
 *              // Do something with bitmap here.
 *          }
 *
 *      });
 * }
 * </code>
 * </pre>
 *
 * @param <Z> The type of resource that this target will receive.
 */
public abstract class SimpleTarget<Z> extends BaseTarget<Z> {
    private final int width;
    private final int height;

    /**
     * Constructor for the target that assumes you will have called
     * {@link com.bumptech.glide.GenericRequestBuilder#override(int, int)} on the request builder this target is given
     * to.
     *
     * <p>
     *     Requests that load into this target will throw an {@link java.lang.IllegalArgumentException} if
     *     {@link com.bumptech.glide.GenericRequestBuilder#override(int, int)} was not called on the request builder.
     * </p>
     */
    public SimpleTarget() {
        this(-1, -1);
    }

    /**
     * Constructor for the target that takes the desired dimensions of the decoded and/or transformed resource.
     *
     * @param width The width in pixels of the desired resource.
     * @param height The height in pixels of the desired resource.
     */
    public SimpleTarget(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Immediately calls the given callback with the sizes given in the constructor.
     *
     * @param cb {@inheritDoc}
     */
    @Override
    public final void getSize(SizeReadyCallback cb) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must both be > 0, but given width: " + width + " and"
                    + " height: " + height + ", either provide dimensions in the constructor or call override()");
        }
        cb.onSizeReady(width, height);
    }
}
