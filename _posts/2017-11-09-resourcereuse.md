---
layout: page
title: "Resource Reuse"
category: doc
date: 2017-11-09 08:04:26
order: 11
disqus: 1
---
* TOC
{:toc}

### Resources

Resources in Glide include things like ``Bitmaps``, ``byte[]`` arrays, ``int[]`` arrays as well as a variety of POJOs. Glide attempts to re-use resources whenever possible to limit the amount of memory churn in your application. 

### Benefits
Excessive allocations of objects of any size can dramatically increase the garbage collection (GC) overhead of your application. Although Android's Dalvik runtime has a much higher GC penalty than the newer ART runtime, excessive allocations will decrease the performance of your application regardless of which device your own.

#### Dalvik
Dalvik devices (pre Lollipop) face an exceptionally large penalty for excessive allocations that are worth discussing. 

Dalvik has two basic modes for garbage collection, GC_CONCURRENT and GC_FOR_ALLOC, both of which you can see in logcat. 

* GC_CONCURRENT blocks the main thread twice for about 5ms for each collection. Since each operation is less than a single frame (16ms), GC_CONCURRENT tends not to cause your application to drop frames. 
* GC_FOR_ALLOC is a stop the world collection that can block the main thread for 125+ms. GC_FOR_ALLOC virtually always causes your application to drop multiple frames, resulting in visible stuttering, particularly while scrolling.

Unfortunately Dalvik seems to handle even modest allocations (a 16kb buffer for example) poorly. Repeated moderate allocations, or even a single large allocation (say for a Bitmap), will cause GC_FOR_ALLOC. Therefore, the more you allocate, the more stop the world garbage collections you incur, and the more frames your application drops.

By re-using moderate to large resources, Glide helps keep your app jank free by avoiding stop the world garbage collections as much as possible.


## How Glide tracks and re-uses resources
Glide takes a permissive approach to resource re-use. Glide will opportunistically re-use resources when it believes it is safe to do so, but Glide does not require calleres to recycle resources after each request. Unless a caller explicitly signals that they're done with a resource (see below), resources will not be recycled or re-used.

### Reference counting
In order to determine when a resource is in use and when it is safe to be re-used, Glide keeps a reference count for each resource. 

#### Incrementing the reference count
Each call to [``into()``][1] that loads a resource increments the reference count for that resource by one. If the same resource is loaded into two different [``Target``][2]s it will have a reference count of two after both loads complete. 

#### Decrementing the reference count
The reference count is decremented when callers signal that they are done with the resource by:

1. Calling [``clear()``][3] on the [``View``][4] or [``Target``][2] the resource was loaded in to.
2. Calling [``into()``][1] on the [``View``][4] or [``Target``][2] with a request for a new resource.

#### Releasing resources.
When the reference count reaches zero, the resource is released and returned to Glide for re-use. After the resource is returned to Glide for re-use it is no longer safe to continue using. As a result it's <strong>unsafe</strong> to:

1. Retrieve a ``Bitmap`` or ``Drawable`` loaded into an ``ImageView`` using ``getImageDrawable()`` and display it (using ``setImageDrawable()``, in an animation or ``TransitionDrawable`` or any other method).
2. Use [``SimpleTarget``][5] to load a resource into a [``View``][4] without also implementing [``onLoadCleared()``][6] and removing the resource from the [``View``][4] in that callback. 
3. Call [``recycle()``][7] on any ``Bitmap`` loaded with Glide.

It's unsafe to reference a resource after clearing the corresponding [``View``][4] or [``Target``][2] because that resource may be destroyed or re-used to display a different image, resulting in undefined behavior, graphical corruption, or crashes in applications that continue to use those resources. For example, after being released back to Glide, ``Bitmap``s may be stored in a ``BitmapPool`` and re-used to hold the bytes of a new image at some point in the future or they may have [``recycle()``][7] called on them (or both). In either case continuing to reference the ``Bitmap`` and expecting it to contain the original image is unsafe.


### Pooling

Although most of Glide's recycling logic is aimed at Bitmaps, all [``Resource``][8] implementations can implement [``recycle()``][9] and pool any re-usable data they might contain. [``ResourceDecoder``][10]s are free to return any implementation of the [``Resource``][8] API they wish, so users can customize or provide additional pooling for novel types by implementing their own [``Resource``][8]s and [``ResourceDecoder``][10]s.

For ``Bitmap``s in particular, Glide provides a [``BitmapPool``][11] interface that allows [``Resource``][8]s to obtain and re-use ``Bitmap`` objects. Glide's [``BitmapPool``][11] can be obtained from any ``Context`` using the Glide singleton:

```java
Glide.get(context).getBitmapPool();
```

Similarly users who want more control over ``Bitmap`` pooling are free to implement their own [``BitmapPool``][11], which they can then provide to Glide using a ``GlideModule``. See the [configuration page][12] for details.

## Common errors
Unfortunately pooling makes it difficult to assert that a user isn't misusing a resource or a ``Bitmap``. Glide tries to add assertions where possible, but because we don't own the underlying ``Bitmap`` we can't guarantee that callers stop using ``Bitmap``s or other resources when they tell us they have via [``clear()``][3] or a new request.

### Symptoms of resource re-use errors.
There are a couple of indicators that something might be going wrong with ``Bitmap`` or other resource pooling in Glide. A few of the most common symptoms we see are listed here, though this is not an exhaustive list.

#### Cannot draw a recycled Bitmap

Glide's ``BitmapPool`` has a fixed size. When ``Bitmap``s are evicted from the pool without being re-used, Glide will call [``recycle()``][7]. If an application inadvertently continues to hold on to the ``Bitmap`` even after indicating to Glide that it is safe to recycle it, the application may then attempt to draw the ``Bitmap``, resulting in a crash in ``onDraw()``.

#### Can't call reconfigure() on a recycled bitmap

Resources are returned to Glide's ``BitmapPool`` when they're not in use any more. This is handled internally based on the lifecycle of a ``Request`` (who controls [``Resource``][8]s). If something calls [``recycle()``][7] on those Bitmaps, but they're still in the pool, Glide cannot re-use them and your app crashes with the above message. One key point here is that the crash will likely happen in the future at another point in your app, and not where the offending code was executed!

#### Views flicker between images or the same image shows up in multiple views

If a ``Bitmap`` is returned to the ``BitmapPool`` multiple times, or is returned to the pool but still held on to by a [``View``][4], another image may be decoded into the ``Bitmap``. If this happens, the contents of the ``Bitmap`` are replaced with the new image. ``View``s may still attempt to draw the ``Bitmap`` during this process, which will result either in artifacts or in the original ``View`` showing a new image.

### Causes of re-use errors.
A few common causes of re-use errors are listed below. As with symptoms, it's difficult to be exhaustive, but these are some things you should definitely consider when trying to debug a re-use error in your application.

#### Attempting to load two different resources into the same Target.
There is no safe way to load multiple resources into a single Target in Glide. Users can use the [``thumbnail()``][13] API to load a series of resources into a [``Target``][2], but it is only safe to reference each earlier resource until the next call to [``onResourceReady()``][14].

Typically a better answer is to actually use a second [``View``][4] and load the second image into the second ``View``. [``ViewSwitcher``][19] can work well to allow you to cross fade between two different images loaded in separate requests. You can just add the ``ViewSwitcher`` to your layout with two ``ImageView`` children and use [``into(ImageView)``][20] twice, once on each child, to load the two images.

Users absolutely must load multiple resources into the same [``View``][4] can do so by using two separate [``Target``][2]s. To make sure that the loads don't cancel each other, users either need to avoid the [``ViewTarget``][15] subclasses, or use a custom [``ViewTarget``][15] subclass and override [``setRequest()``][16] and [``getRequest()``][17] so that they do not use the [``View``][4]'s tag to store the [``Request``][18]. This is advanced usage and not typically recommended. 

#### Loading a resource into a Target, clearing or reusing the Target, and continuing to reference the resource.

The easiest way to avoid this error is to make sure that all references to a resource are nulled out when [``onLoadCleared()``][6] is called. It is generally safe to load a ``Bitmap`` and then de-reference the ``Target`` and never call [``into()``][1] or [``clear()``][3] on the ``Target`` again. However, it is not safe to load a ``Bitmap``, clear the ``Target``, and then continue to reference the ``Bitmap`` later. Similarly it's unsafe to load a resource into a ``View`` and then obtain the resource from the View (via ``getImageDrawable()`` or any other means) and continue to reference it elsewhere.

#### Recycling the original Bitmap in a ``Transformation<Bitmap>``.
As the JavaDoc says in [``Transformation``][21], the original ``Bitmap`` passed in to [``transform()``][22] will be automatically recycled if the ``Bitmap`` returned by the ``Transformation`` is not the same instance as the one passed in to [``transform()``][22]. This is an important difference from other loader libraries, for example Picasso. [``BitmapTransformation``][23] provides the boilerplate to handle Glide's ``Resource`` creation, but the recycling is done internally, so both ``Transformation`` and ``BitmapTransformation`` must not recycle the passed-in ``Bitmap`` or ``Resource``.

It's also worth noting the any intermediate ``Bitmap``s that any custom ``BitmapTransformation`` obtains from the ``BitmapPool``, but does not return from [``transform()``][22] must be either put back to the ``BitmapPool`` or have [``recycle()``][7], but never both. You should never [``recycle()``][7] a ``Bitmap`` obtained from Glide.

[1]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/RequestBuilder.html#into-Y-
[2]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/request/target/Target.html
[3]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/RequestManager.html#clear-com.bumptech.glide.request.target.Target-
[4]: http://d.android.com/reference/android/view/View.html?is-external=true
[5]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/request/target/SimpleTarget.html
[6]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/request/target/Target.html#onLoadCleared-android.graphics.drawable.Drawable-
[7]: https://developer.android.com/reference/android/graphics/Bitmap.html#recycle()
[8]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/load/engine/Resource.html
[9]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/load/engine/Resource.html#recycle--
[10]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/load/ResourceDecoder.html
[11]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/load/engine/bitmap_recycle/BitmapPool.html
[12]: {{ site.baseurl }}/doc/configuration.html#bitmap-pool
[13]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/RequestBuilder.html#thumbnail-com.bumptech.glide.RequestBuilder-
[14]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/request/target/Target.html#onResourceReady-R-com.bumptech.glide.request.transition.Transition-
[15]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/request/target/ViewTarget.html
[16]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/request/target/Target.html#setRequest-com.bumptech.glide.request.Request-
[17]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/request/target/Target.html#getRequest--
[18]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/request/Request.html
[19]: https://developer.android.com/reference/android/widget/ViewSwitcher.html
[20]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/RequestBuilder.html#into-android.widget.ImageView-
[21]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/load/Transformation.html
[22]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/load/resource/bitmap/BitmapTransformation.html#transform-com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool-android.graphics.Bitmap-int-int-
[23]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/load/resource/bitmap/BitmapTransformation.html
