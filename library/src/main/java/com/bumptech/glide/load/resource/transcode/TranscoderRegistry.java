package com.bumptech.glide.load.resource.transcode;

import com.bumptech.glide.util.Synthetic;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that allows {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder}s to be
 * registered and retrieved by the classes they convert between.
 */
public class TranscoderRegistry {
  private final List<Entry<?, ?>> transcoders = new ArrayList<>();

  /**
   * Registers the given {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder} using
   * the given classes so it can later be retrieved using the given classes.
   *
   * @param decodedClass    The class of the resource that the transcoder transcodes from.
   * @param transcodedClass The class of the resource that the transcoder transcodes to.
   * @param transcoder      The transcoder.
   * @param <Z>             The type of the resource that the transcoder transcodes from.
   * @param <R>             The type of the resource that the transcoder transcodes to.
   */
  public synchronized <Z, R> void register(Class<Z> decodedClass, Class<R> transcodedClass,
      ResourceTranscoder<Z, R> transcoder) {
    transcoders.add(new Entry<>(decodedClass, transcodedClass, transcoder));
  }

  /**
   * Returns the currently registered
   * {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder} for the given classes.
   *
   * @param resourceClass   The class of the resource that the transcoder transcodes from.
   * @param transcodedClass The class of the resource that the transcoder transcodes to.
   * @param <Z>             The type of the resource that the transcoder transcodes from.
   * @param <R>             The type of the resource that the transcoder transcodes to.
   */
  @SuppressWarnings("unchecked")
  public synchronized <Z, R> ResourceTranscoder<Z, R> get(Class<Z> resourceClass,
      Class<R> transcodedClass) {
    // For example, there may be a transcoder that can convert a GifDrawable to a Drawable, which
    // will be caught above. However, if there is no registered transcoder, we can still just use
    // the UnitTranscoder to return the Drawable because the transcode class (Drawable) is
    // assignable from the resource class (GifDrawable).
    if (transcodedClass.isAssignableFrom(resourceClass)) {
      return (ResourceTranscoder<Z, R>) UnitTranscoder.get();
    }
    for (Entry<?, ?> entry : transcoders) {
      if (entry.handles(resourceClass, transcodedClass)) {
        return (ResourceTranscoder<Z, R>) entry.transcoder;
      }
    }

    throw new IllegalArgumentException(
        "No transcoder registered to transcode from " + resourceClass + " to " + transcodedClass);
  }

  public synchronized <Z, R> List<Class<R>> getTranscodeClasses(Class<Z> resourceClass,
      Class<R> transcodeClass) {
    List<Class<R>> transcodeClasses = new ArrayList<>();
    // GifDrawable -> Drawable is just the UnitTranscoder, as is GifDrawable -> GifDrawable.
    if (transcodeClass.isAssignableFrom(resourceClass)) {
      transcodeClasses.add(transcodeClass);
      return transcodeClasses;
    }

    for (Entry<?, ?> entry : transcoders) {
      if (entry.handles(resourceClass, transcodeClass)) {
        transcodeClasses.add(transcodeClass);
      }
    }

    return transcodeClasses;
  }

  private static final class Entry<Z, R> {
    private final Class<Z> fromClass;
    private final Class<R> toClass;
    @Synthetic final ResourceTranscoder<Z, R> transcoder;

    Entry(Class<Z> fromClass, Class<R> toClass, ResourceTranscoder<Z, R> transcoder) {
      this.fromClass = fromClass;
      this.toClass = toClass;
      this.transcoder = transcoder;
    }

    /**
     * If we convert from a specific Drawable, we must get that specific Drawable class or a
     * subclass of that Drawable. In contrast, if we we convert <em>to</em> a specific Drawable,
     * we can fulfill requests for a more generic parent class (like Drawable). As a result, we
     * check fromClass and toClass in different orders.
     */
    public boolean handles(Class<?> fromClass, Class<?> toClass) {
      return this.fromClass.isAssignableFrom(fromClass) && toClass.isAssignableFrom(this.toClass);
    }
  }
}
