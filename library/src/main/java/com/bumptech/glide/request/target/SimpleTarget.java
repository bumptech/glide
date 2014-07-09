package com.bumptech.glide.request.target;

/**
 * A simple {@link com.bumptech.glide.request.target.Target} base class with default (usually noop) implementations
 * of non essential methods that allows the caller to specify an exact width/height. Typicaly use cases look something
 * like this:
 * <pre>
 * {@code
 * Glide.load("http://somefakeurl.com/fakeImage.jpeg")
 *      .asBitmap()
 *      .into(new SimpleTarget<Bitmap>(250, 250) {
 *
 *          public void onResourceReady(Bitmap resource, GlideAnimation<Bitmap> glideAnimation) {
 *              // Do something with bitmap here.
 *          }
 *
 *      });
 * }
 * </pre>
 */
public abstract class SimpleTarget<Z> extends BaseTarget<Z> {
    private final int width;
    private final int height;

    /**
     * Constructor for the target that takes the desired dimensions of the decoded and/or transformed resource.
     *
     * @param width The desired width of the resource.
     * @param height The desired height of the resource.
     */
    public SimpleTarget(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must both be > 0, but given width: " + width + " and"
                    + " height: " + height);
        }
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
        cb.onSizeReady(width, height);
    }
}
