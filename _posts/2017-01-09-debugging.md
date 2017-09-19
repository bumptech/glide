---
layout: page
title: "Debugging"
category: doc
date: 2017-01-09 07:14:59
order: 11 
disqus: 1
---
* TOC
{:toc}

### Local Logs
If you have access to the device, you can look for a few log lines using ``adb logcat`` or your IDE. You can enable logging for any tag mentioned here using:

```
adb shell setprop log.tag.<tag_name> <VERBOSE|DEBUG>
```

VERBOSE logs tend to be more verbose but contain more useful information. Depending on the tag, you can try both VERBOSE and DEBUG to see which provides the best level of information.

#### Request errors
The highest level and easiest to understand logs are logged with the ``Glide`` tag:

```
adb shell setprop log.tag.Glide DEBUG
```

The Glide tag will log both successful and failed requests and differing levels of detail depending on the log level. VERBOSE should be used to log successful requests. DEBUG can be used to log detailed error messages.

You can also control the verbosity of the Glide log tag programmatically using [``setLogLevel(int)``][1]. ``setLogLevel`` allows you to enable more verbose logs in developer builds but not release builds, for example.

#### Unexpected cache misses
For details on how Glide's caching works, see [the Caching page][13].

The [``Engine``][9] log tag provides details on how a request will be fulfilled and includes the full in memory cache key used to store the corresponding resource. If you're trying to debug why images you have in memory in one place aren't being used in another place, the ``Engine`` tag lets you compare the cache keys directly to see the differences.

For each started request, the ``Engine`` tag will log that the request will be completed from cache, active resources, an existing load, or a new load. Cache means that the resource wasn't in use, but was available in the in memory cache. Active resources means that the resource was actively being used by another ``Target``, typically in a ``View``. An existing load means that the resource wasn't available in memory, but another ``Target`` had previously requested the same resource and the load is already in progress. Finally a new load means that the resource was neither in memory nor already being loaded so our request triggered a new load.

#### Missing images and local logs
In some cases you may see that an image never loads and that no logs with either the ``Glide`` tag or the ``Engine`` tag are ever logged for your request. There are a few possible causes.

##### Failing to start the request.
Verify that you're calling [``into()``][2] or [``submit()``][3] for your request. If you don't call either method, you're never asking Glide to start your load.

##### Missing Size
If you verify that you are in fact calling [``into()``][2] or [``submit()``][3] and you're still not seeing logs, the most likely explanation is that Glide is unable to determine the size of the ``View`` or ``Target`` you're attempting to load your resource into.

###### Custom Targets
If you're using a custom ``Target``, make sure you've either implemented [``getSize``][4] and are calling the given callback with a non-zero width and height or are subclassing a ``Target`` like [``ViewTarget``][5] that implements the method for you.

###### Views
If you're just loading a resource into a ``View``, the most likely explanation is that your view is either not going through layout or is being given a 0 width or height. Views may not go through layout if their visibility is set to ``View.GONE`` or if they are never attached. Views may receive invalid or 0 widths and heights if they and/or their parents have certain combinations of ``wrap_content`` and ``match_parent`` for their widths and heights. You can experiment by giving your views fixed non-zero dimensions or passing in an specific size to Glide to use for the request with the [``override(int, int)`` API][6].

#### RequestListener and custom logs
If you'd like to programmatically keep track of errors and successful loads, track the overall cache hit ratio of images in your application, or have more control over local logs, you can use the [``RequestListener``][7] interface. ``RequestListener`` can be added to an individual load using [``RequestBuilder#listener()``][8]. Sample usage looks like this:

```java
Glide.with(fragment)
   .load(url)
   .listener(new RequestListener() {
       @Override
       boolean onLoadFailed(@Nullable GlideException e, Object model,
           Target<R> target, boolean isFirstResource) {
         // Log errors here.
       }

       @Override
       boolean onResourceReady(R resource, Object model, Target<R> target,
           DataSource dataSource, boolean isFirstResource) {
         // Log successes here or use DataSource to keep track of cache hits and misses.
       }
    })
    .into(imageView);
```

To save object allocations, you can re-use the same ``RequestListener`` for multiple loads.

### Out of memory errors
Almost all OOM errors are due to issues with the hosting application and not with Glide.

There are two common causes of OOMs in applications:

1. Excessively large allocations
2. Memory leaks (memory that is allocated but never released)

#### Excessively large allocations.
If opening a single page or loading a single image causes an OOM, your applications is probably loading an unnecessarily large image.

The amount of memory required to display an image in a Bitmap is width * height * bytes per pixel. The number of bytes per pixel depends on the ``Bitmap.Config`` used to display the image, but typically four bytes per pixel are required for ``ARGB_8888`` Bitmaps. As a result, even a 080p image requires 8mb of ram. The larger the image, the more ram required, so a 12 megapixel image requires a fairly massive 48mb.

Glide will downsample images automatically based on the size provided by the ``Target``, ``ImageView`` or ``override()`` request option provided. If you're seeing excessively large allocations in Glide, usually that means that the size of your ``Target`` or ``override()`` is too large or you're using ``Target.SIZE_ORIGINAL`` in conjunction with a large image.

To fix excessively large allocations, avoid ``Target.SIZE_ORIGINAL`` and ensure that the size of your ``ImageViews`` or that you provide to Glide via ``override()`` are reasonable.

#### Memory leaks.
If repeating the same set of steps in your application over and over again gradually increases your applications' memory usage and eventually leads to an OOM, you probably have a memory leak.

The [Android documentation][10] has a lot of good information on tracking and debugging memory usage. To investigate memory leaks, you're almost certainly going to want to [capture a heap dump][11] and look for Fragments, Activities or other objects that are retained after they're no longer used.

To fix memory leaks, remove references to the destroyed ``Fragment`` or ``Activity`` at the appropriate point in the lifecycle to avoid retaining excessive objects. Use the heap dump to help find other ways your application retains memory and remove unnecessary references as you find them. It's often helpful to start by listing the shortest paths excluding weak references to all Bitmap objects (using [MAT][12] or another memory analyzer) and then looking for reference chains that seem suspicious. You can also check to make sure that you have no more than once instance of each ``Activity`` and only the expected number of instances of each ``Fragment`` by searching for them in your memory analyzer.

[1]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/GlideBuilder.html#setLogLevel-int-
[2]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/RequestBuilder.html#into-android.widget.ImageView-
[3]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/RequestBuilder.html#submit-int-int-
[4]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/request/target/Target.html#getSize-com.bumptech.glide.request.target.SizeReadyCallback-
[5]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/request/target/ViewTarget.html
[6]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/request/RequestOptions.html#override-int-int-
[7]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/request/RequestListener.html
[8]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/RequestBuilder.html#listener-com.bumptech.glide.request.RequestListener-
[9]: https://github.com/bumptech/glide/blob/6b137c2b1d4b2ab187ea2aa56834dea039daa090/library/src/main/java/com/bumptech/glide/load/engine/Engine.java#L33
[10]: https://developer.android.com/studio/profile/investigate-ram.html
[11]: https://developer.android.com/studio/profile/investigate-ram.html#HeapDump
[12]: http://www.eclipse.org/mat/
[13]: {{ site.baseurl }}/doc/caching.html
