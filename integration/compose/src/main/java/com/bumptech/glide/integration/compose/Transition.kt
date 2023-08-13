package com.bumptech.glide.integration.compose

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter

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
  public val drawPlaceholder: DrawPainter?

  /**
   * Draw the current image. If you do not draw the current image, it will not be shown.
   *
   * This method is used before and after the transition finishes, so you need to ensure that you
   * draw something even if your transition is not currently running, if you want the image to
   * be displayed.
   *
   * The canvas is prepared so that the image will be drawn in the appropriate location based on
   * the parameters you provided (e.g. `ContentScale`). The canvas is saved before this method is
   * called and restored afterward so you do not need to do so manually unless it's required for
   * your transition.
   *
   * This method is always called if there is something to draw. If [drawPlaceholder] is also
   * called, [drawPlaceholder] will be called before this method.
   */
  public val drawCurrent: DrawPainter

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

public typealias DrawPainter = DrawScope.(Painter, Size, Float, ColorFilter?) -> Unit

internal object DoNotTransition : Transition {
  object Factory : Transition.Factory {
    override fun build() = DoNotTransition
  }

  override suspend fun transition(invalidate: () -> Unit) {}
  override suspend fun stop() {}
  override val drawPlaceholder: DrawPainter = { _, _, _, _ -> }
  override val drawCurrent: DrawPainter = { painter, size, alpha, colorFilter ->
    with(painter) {
      draw(size, alpha, colorFilter)
    }
  }
}

/**
 * A factory for [CrossFade]. If you do not want to modify the [animationSpec], use the companion
 * object (ie `transition = CrossFade`)
 */
public class CrossFade(
  private val animationSpec: AnimationSpec<Float>
) : Transition.Factory {

  override fun build(): Transition = CrossFadeImpl(animationSpec)

  /**
   * A default [CrossFade] with a 250ms duration
   */
  public companion object : Transition.Factory {
    override fun build(): Transition =
      CrossFadeImpl(animationSpec = tween(250))
  }

  override fun equals(other: Any?): Boolean {
    if (other is CrossFade) {
      return animationSpec == other.animationSpec
    }
    return false
  }

  override fun hashCode(): Int {
    return animationSpec.hashCode()
  }
}

/**
 * Fades out the placeholder while fading in the resource(s).
 */
internal class CrossFadeImpl(
  private val animationSpec: AnimationSpec<Float>
) : Transition {

  private companion object {
    const val OPAQUE_ALPHA = 1f
  }

  private val animatable: Animatable<Float, AnimationVector1D> =
    Animatable(0f, Float.VectorConverter, OPAQUE_ALPHA)

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

  override val drawPlaceholder: DrawPainter = { painter, size, alpha, colorFilter ->
    with(painter) {
      draw(size, (OPAQUE_ALPHA - animatable.value) * alpha, colorFilter)
    }
  }

  override val drawCurrent: DrawPainter = { painter, size, alpha, colorFilter ->
    with(painter) {
      draw(size, animatable.value * alpha, colorFilter)
    }
  }
}