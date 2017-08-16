---
layout: page
title: "Transitions"
category: doc
date: 2017-02-13 19:40:27
order: 8
disqus: 1
---
* TOC
{:toc}

### About
[``Transitions``][1] in Glide allow you to define how Glide should transition from a placeholder to a newly loaded image or from a thumbnail to a full size image. Transitions act within the context of a single request, not across multiple requests. As a result, [``Transitions``][1] do **NOT** allow you to define an animation (like a cross fade) from one request to another request. 


### Default transition
Unlike Glide v3, Glide v4 does **NOT** apply a cross fade or any other transition by default. Transitions must be applied manually per request.

### Standard behavior
Glide provides a number of transitions that users can manually apply per request. Glide's built in transitions behave in a consistent manner and will avoid running in certain circumstances depending on where images are loaded from.

Images can be loaded from one of four places in Glide:

1. Glide's in memory cache
2. Glide's disk cache
3. A source File or Uri available locally on the device
4. A source Url or Uri available only remotely.

Glide's built in transitions do not run if data is loaded from Glide's in memory cache. However, Glide's built in transitions do run if data is loaded from Glide's disk cache, a local source File or Uri or a remote source Url or Uri. 

To change this behavior and write your own custom transition, see the [custom transitions][20] section below.

### Specifying Transitions
For an overview and code sample, see the [Options documentation][18].

[``TransitionOptions``][12] are used to specify the transitions for a particular request. [``TransitionOptions``][12] are set for a request using the [``transition()``][13] method in [``RequestBuilder``][14]. Type specific transitions can be specified using [``BitmapTransitionOptions``][15] or [``DrawableTransitionOptions``][16]. For types other than ``Bitmaps`` and ``Drawables`` [``GenericTransitionOptions``][17] can be used. 

### Performance Tips
Animations in Android can be expensive, particularly if a large number are started at once. Cross fades and other animations involving changes in alpha can be especially expensive. In addition, animations often take substantially longer to run than images take to decode. Gratuitous use of animations in lists and grids can make image loading feel slow and janky. To maximize performance, consider avoiding animations when using Glide to load images into ListViews, GridViews, or RecyclerViews, especially when you expect images to be cached or fast to load most of the time. Instead consider pre-loading so that images are in memory when users scroll to them. 

### Common Errors

#### Cross fading with placeholders and transparent images
Glide's default cross fade animation leverages [``TransitionDrawable``][8]. [``TransitionDrawable``][8] offers two animation modes, controlled by [``setCrossFadeEnabled()``][9]. When cross fades are disabled, the image that is transitioned to is faded in on top of the image that was already showing. When cross fades are enabled, the image that is being transitioned from is animated from opaque to transparent and the image that is being transitioned to is animated from transparent to opaque. 

In Glide, we default to disabling cross fades because it typically provides a much nicer looking animation. An actual cross fade where the alpha of both images is changing at once often produces a white flash in the middle of the animation where both images are partially opaque. 

Unfortunately although disabling cross fades is typically a better default, it can also lead to problems when the image that is being loaded contains transparent pixels. When the placeholder is larger than the image that is being loaded or the image is partially transparent, disabling cross fades results in the placeholder being visible behind the image after the animation finishes. If you are loading transparent images with placeholders, you can enable cross fades by adjusting the options in [``DrawableCrossFadeFactory``][10] and passing the result into [``transition()``][11].

#### Cross fading across requests.
[``Transitions``][1] do not allow you to cross fade between two different images that are loaded with different requests. Glide by default will cancel any existing requests when you start a new load into an existing View or Target (See [Targets documentation][19] for more details). As a result, if you want to load two different images and cross fade between them, you cannot do so with Glide directly. Strategies like waiting for the first load to finish, grabbing a Bitmap or Drawable out of the View, starting a second load, and then manually animating between the Drawale or Bitmap and the new image are unsafe and may result in crashes or graphical corruption. 

Instead, the easiest way to cross fade across two different images loaded in two separate requests is to use [``ViewSwitcher``][2] containing two [``ImageViews``][3]. Load the first image into the result of [``getNextView()``][4]. Then load the second image into the next result of [``getNextView()``][4] and use a [``RequestListener``][5] to call [``showNext()``][6] when the second image load finishes. For better control, you can also follow the strategy outlined in the [developer documentation][7]. As with the [``ViewSwitcher``][2], only start the cross fade after the second image load finishes.

### Custom Transitions
To define a custom transition:

1. Implement [``TransitionFactory``][21].
2. Apply your custom ``TransitionFactory`` to loads with [``DrawableTransitionOptions#with``][22].


To change the default behavior of your transition so that you can control whether or not it's applied when your image is loaded from the memory cache, disk cache or from source, 
you can inspect the [``DataSource``][23] passed in to the [``build()``][24] method in your [``TransitionFactory``][21],  

For an example, see [``DrawableCrossFadeFactory``][25].

[1]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/request/transition/Transition.html
[2]: https://developer.android.com/reference/android/widget/ViewSwitcher.html
[3]: https://developer.android.com/reference/android/widget/ImageView.html
[4]: https://developer.android.com/reference/android/widget/ViewSwitcher.html#getNextView()
[5]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/request/RequestListener.html
[6]: https://developer.android.com/reference/android/widget/ViewAnimator.html#showNext()
[7]: https://developer.android.com/training/animation/crossfade.html
[8]: https://developer.android.com/reference/android/graphics/drawable/TransitionDrawable.html
[9]: https://developer.android.com/reference/android/graphics/drawable/TransitionDrawable.html#setCrossFadeEnabled(boolean)
[10]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/request/transition/DrawableCrossFadeFactory.html
[11]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/TransitionOptions.html#transition-com.bumptech.glide.request.transition.TransitionFactory-
[12]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/TransitionOptions.html
[13]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/RequestBuilder.html#transition-com.bumptech.glide.TransitionOptions-
[14]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/RequestBuilder.html
[15]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/load/resource/bitmap/BitmapTransitionOptions.html
[16]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/load/resource/drawable/DrawableTransitionOptions.html
[17]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/GenericTransitionOptions.html
[18]: /glide/doc/options.html#transitionoptions
[19]: /glide/doc/targets.html#targets-and-automatic-cancellation
[20]: {{ site.url }}/glide/transitions#custom-transitions
[21]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/request/transition/TransitionFactory.html
[22]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/load/resource/drawable/DrawableTransitionOptions.html#with-com.bumptech.glide.request.transition.TransitionFactory-
[23]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/load/DataSource.html
[24]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/request/transition/TransitionFactory.html#build-com.bumptech.glide.load.DataSource-boolean-
[25]: https://github.com/bumptech/glide/blob/8f22bd9b82349bf748e335b4a31e70c9383fb15a/library/src/main/java/com/bumptech/glide/request/transition/DrawableCrossFadeFactory.java#L35 
