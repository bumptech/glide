package com.bumptech.glide.integration.compose

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas

/**
 * Transition between a given request's optional placeholder and the resource.
 */
public interface Transition {
  /**
   * Start the transition, calling [invalidate] each time the transition requires that the image
   * be re-drawn.
   */
  public suspend fun transition(invalidate: () -> Unit)

  /**
   * Stop the transition
   */
  public suspend fun stop()

  /**
   * Optionally draw the current placeholder. If you want the placeholder to be shown during the
   * transition, you must implement this method. If you only want to show the placeholder until
   * the first resource finishes loading, then you can simply return null
   *
   * The canvas is prepared so that the image will be drawn in the appropriate location based on
   * the parameters you provided (e.g. `ContentScale`). The canvas is saved before this method is
   * called and restored afterward so you do not need to do so manually unless it's required for
   * your transition.
   *
   * This method will only be called if a placeholder is set. If this method is called, it will
   * always be called before [drawCurrent].
   */
  public val drawPlaceholder: (DrawScope.(Drawable) -> Unit)?

  /**
   * Draw the current image. If you do not draw the current image, it will not be shown.
   *
   * The canvas is prepared so that the image will be drawn in the appropriate location based on
   * the parameters you provided (e.g. `ContentScale`). The canvas is saved before this method is
   * called and restored afterward so you do not need to do so manually unless it's required for
   * your transition.
   *
   * This method is always called. If [drawPlaceholder] is also called, it will be called before
   * this method.
   */
  public val drawCurrent: DrawScope.(Drawable) -> Unit

  /**
   * Creates a new instance of this [Transition]. Must implement equals/hashcode. The simplest way
   * of ensuring equals and hashcode are implemented correctly for custom transitions is to use an
   * Object.
   */
  public interface Factory {
    /**
     * Create a new stateful [Transition] instance.
     *
     * May be called multiple times for a single Composable.
     */
    public fun build(): Transition
  }
}

internal object DoNotTransition: Transition {
  object Factory : Transition.Factory {
    override fun build() = DoNotTransition
  }
  override suspend fun transition(invalidate: () -> Unit) {}
  override suspend fun stop() {}
  override val drawPlaceholder: DrawScope.(Drawable?) -> Unit = {}
  override val drawCurrent: DrawScope.(Drawable) -> Unit = {
    it.draw(drawContext.canvas.nativeCanvas)
  }
}

/**
 * Fades out the placeholder while fading in the resource(s).
 */
public class CrossFade private constructor(
  private val animationSpec: AnimationSpec<Int>
): Transition {

  /**
   * A factory for [CrossFade]. If you do not want to modify the [animationSpec], use [DEFAULT].
   */
  public class Factory(
    private val animationSpec: AnimationSpec<Int> = tween(250)
  ): Transition.Factory {

    public companion object {
      /**
       * A default [CrossFade] with a 250ms duration
       */
      public val DEFAULT: Factory = Factory()
    }

    override fun build(): Transition = CrossFade(animationSpec)

    override fun equals(other: Any?): Boolean {
      if (other is Factory) {
        return animationSpec == other.animationSpec
      }
      return false
    }

    override fun hashCode(): Int {
      return animationSpec.hashCode()
    }
  }

  private companion object {
    const val OPAQUE_ALPHA = 255
  }

  private val animatable: Animatable<Int, AnimationVector1D> =
    Animatable(0, Int.VectorConverter, OPAQUE_ALPHA)
  override suspend fun transition(invalidate: () -> Unit) {
    try {
      animatable.animateTo(OPAQUE_ALPHA, animationSpec)
      invalidate()
    } finally {
      animatable.snapTo(OPAQUE_ALPHA)
      invalidate()
    }
  }

  override suspend fun stop() {
    animatable.stop()
  }

  override val drawPlaceholder: DrawScope.(Drawable?) -> Unit = {
    if (it != null) {
      val nativeCanvas = drawContext.canvas.nativeCanvas
      nativeCanvas.saveLayerAlpha(
        0f, 0f, drawContext.size.width, drawContext.size.height, OPAQUE_ALPHA - animatable.value
      )
      it .draw(nativeCanvas)
      nativeCanvas.restore()
    }
  }

  override val drawCurrent: DrawScope.(Drawable) -> Unit = {
    val nativeCanvas = drawContext.canvas.nativeCanvas
    nativeCanvas.saveLayerAlpha(
      0f, 0f, drawContext.size.width, drawContext.size.height, animatable.value)
    it.draw(nativeCanvas)
    nativeCanvas.restore()
  }
}
