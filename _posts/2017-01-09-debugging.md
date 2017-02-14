---
layout: page
title: "Debugging"
category: doc
date: 2017-01-09 07:14:59
order: 8
disqus: 1
---
### Local Logs

If you have access to the device, you can look for a few log lines using ``adb logcat`` or your IDE. You can enable logging for any tag mentioned here using ``adb shell setprop log.tag.<tag_name> <VERBOSE|DEBUG>``. VERBOSE logs tend to be more verbose but contain more useful information. Depending on the tag, you can try both VERBOSE and DEBUG to see which provides the best level of information.

#### Request errors

The highest level and easiest to understand logs are logged with the ``Glide`` tag. The Glide tag will log both successful and failed requests and differing levels of detail depending on the log level. VERBOSE should be used to log successful requests. DEBUG can be used to log detailed error messages.

You can also control the verbosity of the Glide log tag programmatically using [``setLogLevel(int)``][1]. ``setLogLevel`` allows you to enable more verbose logs in developer builds but not release builds, for example.

#### Unexpected cache misses

The ``Engine`` tag provides details on how a request will be fulfilled and includes the full in memory cache key used to store the corresponding resource. If you're trying to debug why images you have in memory in one place aren't being used in another place, the ``Engine`` tag lets you compare the cache keys directly to see the differences.

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


[1]: https://github.com/bumptech/glide/blob/6ddb5f0598b1a5a5a51647fb968e998d6cabbd3d/library/src/main/java/com/bumptech/glide/GlideBuilder.java#L248
[2]: https://github.com/bumptech/glide/blob/6ddb5f0598b1a5a5a51647fb968e998d6cabbd3d/library/src/main/java/com/bumptech/glide/RequestBuilder.java#L345
[3]: https://github.com/bumptech/glide/blob/6ddb5f0598b1a5a5a51647fb968e998d6cabbd3d/library/src/main/java/com/bumptech/glide/RequestBuilder.java#L456
[4]: https://github.com/bumptech/glide/blob/master/library/src/main/java/com/bumptech/glide/request/target/Target.java#L84
[5]: https://github.com/bumptech/glide/blob/master/library/src/main/java/com/bumptech/glide/request/target/ViewTarget.java
[6]: https://github.com/bumptech/glide/blob/6ddb5f0598b1a5a5a51647fb968e998d6cabbd3d/library/src/main/java/com/bumptech/glide/request/BaseRequestOptions.java#L333
[7]: https://github.com/bumptech/glide/blob/6ddb5f0598b1a5a5a51647fb968e998d6cabbd3d/library/src/main/java/com/bumptech/glide/request/RequestListener.java
[8]: https://github.com/bumptech/glide/blob/6ddb5f0598b1a5a5a51647fb968e998d6cabbd3d/library/src/main/java/com/bumptech/glide/RequestBuilder.java#L117
