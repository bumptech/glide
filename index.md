---
layout: default
title: "Fast and efficient image loading for Android"
---

### About Glide

Glide is a fast and efficient image loading library for Android focused on smooth scrolling. Glide offers an easy to use API, a performant and extensible resource decoding pipeline and automatic resource pooling.

![tesw](https://github.com/bumptech/glide/blob/master/static/glide_logo.png?raw=true)

Glide supports fetching, decoding, and displaying video stills, images, and animated GIFs. Glide includes a flexible api that allows developers to plug in to almost any network stack. By default Glide uses a custom HttpUrlConnection based stack, but also includes utility libraries plug in to Google's Volley project or Square's OkHttp library instead.

Glide's primary focus is on making scrolling any kind of a list of images as smooth and fast as possible, but Glide is also effective for almost any case where you need to fetch, resize, and display a remote image.


#### API

Glide uses a simple fluent API that allows users to make most requests in a  single line:

```java
Glide.with(fragment)
    .load(url)
    .into(imageView);
```

#### Performance

Glide takes in to account two key aspects of image loading performance on Android:

* The speed at which images can be decoded.
* The amount of jank incurred while decoding images.

For users to have a great experience with an app, images must not only appear quickly, but they must also do so without causing lots of jank and stuttering from main thread I/O or excessive garbage collections.

Glide takes a number of steps to ensure image loading is both as fast and as smooth as possible on Android:

* Smart and automatic downsampling and caching minimize storage overhead and decode times.
* Aggressive re-use of resources like byte arrays and Bitmaps minimizes expensive garbage collections and heap fragmentation.
* Deep lifecycle integration ensures that only requests for active Fragments and Activities are prioritized and that Applications release resources when neccessary to avoid being killed when backgrounded.

### Getting Started

Start by vising the [Download and Setup][1] page to learn how to integrate Glide in to your app. Then take a look at the [Getting Started][2] page to learn the basics. For more help and examples, continue on through the rest of the Documentation section, or take a look at one of our many [sample apps][3].

### Requirements

Glide v4 requires Android [Ice Cream Sandwich][4] (API level 14) or higher.

[1]: doc/download-setup.html
[2]: doc/getting-started.html
[3]: ref/samples.html
[4]: https://developer.android.com/about/versions/android-4.0-highlights.html
