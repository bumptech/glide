---
layout: page
title: "Hardware Bitmaps"
category: doc
date: 2018-02-11 09:27:59
order: 13
disqus: 1
---
* TOC
{:toc}

### What are hardware Bitmaps?
[`Bitmap.Config.HARDWARE`][3] is a new `Bitmap` format that was added in Android O. Hardware `Bitmaps` store pixel data only in graphics memory and are optimal for cases where the `Bitmap` is only drawn to the screen. 

### Why should we use hardware Bitmaps?
Only one copy of pixel data is stored for hardware `Bitmaps`. Normally there’s one copy of pixel data in application memory (the pixel byte array) and one copy in graphics memory (after the pixels are uploaded to the GPU). Hardware `Bitmaps` retain only the copy uploaded to the GPU. As a result:

 * Hardware `Bitmaps` require ½ the memory of other Bitmap configurations
 * Hardware `Bitmaps` avoid jank caused by texture uploads at draw time.

### How do we enable hardware Bitmaps?
Temporarily, set the default [`DecodeFormat`][1] to [`DecodeFormat.PREFER_ARGB_8888`][2] in your Glide requests. To do so for all requests in your application, set the `DecodeFormat` in the default options in your `GlideModule`, see [the configuration page][4].

In the long run Glide will load hardware `Bitmaps` by default and no changes will be needed to enable the format, only to disable it.

### How do we disable hardware Bitmaps?
If you need to disable hardware `Bitmaps`, you should try to do so only for requests where you need to do one of the slow or broken things below. You can disable hardware `Bitmaps` for a particular request using [`disallowHardwareConfig()`][5].

If you’re using the generated API:

```java
GlideApp.with(fragment)
  .load(url)
  .disallowHardwareConfig()
  .into(imageView);
```

Or just via `RequestOptions`:

```java
RequestOptions options = new RequestOptions().disallowHardwareConfig();
Glide.with(fragment)
  .load(url)
  .apply(options)
  .into(imageView);
```

### What’s broken when we use hardware Bitmaps?
Storing pixel data in graphics memory means that the pixel data isn’t readily accessible, which will cause exceptions in some cases. All of the known cases are listed below:
* Reading/writing pixel data in Java, including:
  * [Bitmap#getPixel](https://developer.android.com/reference/android/graphics/Bitmap.html#getPixel(int, int))
  * [Bitmap#getPixels](https://developer.android.com/reference/android/graphics/Bitmap.html#getPixels(int[], int, int, int, int, int, int))
  * [Bitmap#copyPixelsToBuffer](https://developer.android.com/reference/android/graphics/Bitmap.html#copyPixelsToBuffer(java.nio.Buffer))
  * [Bitmap#copyPixelsFromBuffer](https://developer.android.com/reference/android/graphics/Bitmap.html#copyPixelsFromBuffer(java.nio.Buffer))
* Reading/writing pixel data in native code
* Rendering hardware `Bitmaps` with a software Canvas:
```java
Canvas canvas = new Canvas(normalBitmap)
canvas.drawBitmap(hardwareBitmap, 0, 0, new Paint());
```
* Using software layer types in views that draw Bitmaps (to draw a shadow for example)
```java
ImageView imageView = …
imageView.setImageBitmap(hardwareBitmap);
imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
```

* Opening too many file descriptors.
  
    Each hardware `Bitmap` consumes a file descriptor. There’s a per process FD limit (O & earlier typically 1024, in some builds of O-MR1 & higher it’s 32k). Glide attempts to limit the number of hardware `Bitmaps` allocated to stay under the limit, but if you’re already allocating large numbers of FDs, this may be an issue. 

* Preconditions that expect `ARGB_8888 Bitmaps`
* Screenshots triggered by code that try to draw the view hierarchy with `Canvas`.

    [`PixelCopy`][6] can be used instead on O+.  

* Shared element transitions (fixed in OMR1)
An example trace will look something like this:
```
java.lang.IllegalStateException: Software rendering doesn't support hardware bitmaps
  at android.graphics.BaseCanvas.throwIfHwBitmapInSwMode(BaseCanvas.java:532)
  at android.graphics.BaseCanvas.throwIfCannotDraw(BaseCanvas.java:62)
  at android.graphics.BaseCanvas.drawBitmap(BaseCanvas.java:120)
  at android.graphics.Canvas.drawBitmap(Canvas.java:1434)
  at android.graphics.drawable.BitmapDrawable.draw(BitmapDrawable.java:529)
  at android.widget.ImageView.onDraw(ImageView.java:1367)
[snip]
  at android.view.View.draw(View.java:19089)
  at android.transition.TransitionUtils.createViewBitmap(TransitionUtils.java:168)
  at android.transition.TransitionUtils.copyViewImage(TransitionUtils.java:102)
  at android.transition.Visibility.onDisappear(Visibility.java:380)
  at android.transition.Visibility.createAnimator(Visibility.java:249)
  at android.transition.Transition.createAnimators(Transition.java:732)
  at android.transition.TransitionSet.createAnimators(TransitionSet.java:396)
[snip]
```

### What’s less efficient when we use hardware Bitmaps?
In some cases to avoid breaking users, the `Bitmap` class will perform an expensive copy of the graphics memory. In some cases where any of these methods are used, you should consider avoiding using the hardware `Bitmap` configuration to begin with depending on the frequency the slow methods are used. If you do use these methods, the framework will log a message: `“Warning attempt to read pixels from hardware bitmap, which is very slow operation”` and also trigger a [`StrictMode#noteSlowCall`][7].
* [Bitmap#copy](https://developer.android.com/reference/android/graphics/Bitmap.html#copy(android.graphics.Bitmap.Config, boolean))
* [Bitmap#createBitmap*](https://developer.android.com/reference/android/graphics/Bitmap.html#createBitmap(android.graphics.Bitmap, int, int, int, int))
* [Bitmap#writeToParcel](https://developer.android.com/reference/android/graphics/Bitmap.html#writeToParcel(android.os.Parcel, int))
* [Bitmap#extractAlpha](https://developer.android.com/reference/android/graphics/Bitmap.html#extractAlpha())
* [Bitmap#sameAs](https://developer.android.com/reference/android/graphics/Bitmap.html#sameAs(android.graphics.Bitmap))

[1]: https://bumptech.github.io/glide/javadocs/460/com/bumptech/glide/load/DecodeFormat.html
[2]: https://bumptech.github.io/glide/javadocs/460/com/bumptech/glide/load/DecodeFormat.html#PREFER_ARGB_8888
[3]: https://developer.android.com/reference/android/graphics/Bitmap.Config.html#HARDWARE
[4]: https://bumptech.github.io/glide/doc/configuration.html#default-request-options
[5]: https://bumptech.github.io/glide/javadocs/460/com/bumptech/glide/request/RequestOptions.html#disallowHardwareConfig--
[6]: https://developer.android.com/reference/android/view/PixelCopy.html
[7]: https://developer.android.com/reference/android/os/StrictMode.html#noteSlowCall(java.lang.String)
