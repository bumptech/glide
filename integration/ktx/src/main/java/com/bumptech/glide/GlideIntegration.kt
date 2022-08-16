/**
 * Functions that give us access to some of Glide's non-public internals to make the flows API a bit
 * better.
 */
package com.bumptech.glide

import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

internal fun RequestBuilder<*>.requestManager() = this.requestManager

internal fun <ResourceT, TargetAndRequestListenerT> RequestBuilder<ResourceT>.intoDirect(
  targetAndRequestListener: TargetAndRequestListenerT,
) where TargetAndRequestListenerT : Target<ResourceT>,
        TargetAndRequestListenerT : RequestListener<ResourceT>  {
  this.into(targetAndRequestListener, targetAndRequestListener) {
    it.run()
  }
}