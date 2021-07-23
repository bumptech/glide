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

For now, you need to set an explicit `RequestOption` to enable hardware Bitmaps:

```java
new RequestOption().set(Downsampler.ALLOW_HARDWARE_CONFIG, true);
```

Sadly we're finding a number of cases where using Hardware Bitmaps leads to crashes on some devices. I'd only recommend default enabling hardware Bitmaps on Android 9 (API 28) or higher. 

In the long run we hope that Glide will load hardware `Bitmaps` by default and no changes will be needed to enable the format, only to disable it.

### How do we disable hardware Bitmaps?
If you need to disable hardware `Bitmaps`, you should try to do so only for requests where you need to do one of the slow or broken things below. You can disable hardware `Bitmaps` for a particular request using [`disallowHardwareConfig()`][5]:

```java
Glide.with(fragment)
  .load(url)
  .disallowHardwareConfig()
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

* Low FD Limits on some devices on Android O

```
Cause: null pointer dereference
    x0   0000007c09c1e4c0  x1   000000000000060a  x2   0000000070071de8  x3   0000000000000000
    x4   0000000000000000  x5   00000000ffffffff  x6   00000000ffffffff  x7   0000007bca0a7100
    x8   0000007bca2bf6c0  x9   0000007c19510000  x10  0000007c19510000  x11  0000007bcaf25300
    x12  0000000000000000  x13  000000000000004e  x14  0000000000000000  x15  0000007c09c02c90
    x16  0000007c183e0cb0  x17  0000007c1837d740  x18  0000000000000000  x19  0000000000000000
    x20  0000007c09c1e4c0  x21  00000000ffffffff  x22  0000000000000000  x23  0000000000000000
    x24  0000000000000002  x25  0000000000000000  x26  0000007bca0a85b0  x27  0000000000000000
    x28  0000000000000000  x29  0000007bca0a8470  x30  0000007c19340a60
    sp   0000007bca0a8410  pc   0000007c19340a80  pstate 0000000060000000

backtrace:
    #00 pc 0000000000177a80  /system/lib64/libandroid_runtime.so (_ZN7android6bitmap12createBitmapEP7_JNIEnvPNS_6BitmapEiP11_jbyteArrayP8_jobjecti+96)
    #01 pc 000000000017da60  /system/lib64/libandroid_runtime.so (_ZL8doDecodeP7_JNIEnvP18SkStreamRewindableP8_jobjectS4_+4640)
    #02 pc 0000000000bc6c30  /system/framework/arm64/boot-framework.oat (offset 0x692000) (android.graphics.BitmapFactory.nativeDecodeStream [DEDUPED]+256)
    #03 pc 0000000000bc6520  /system/framework/arm64/boot-framework.oat (offset 0x692000) (android.graphics.BitmapFactory.decodeStream+272)
    #04 pc 000000000052e638  /system/lib64/libart.so (art_quick_invoke_static_stub+600)
    #05 pc 00000000000d86e4  /system/lib64/libart.so (_ZN3art9ArtMethod6InvokeEPNS_6ThreadEPjjPNS_6JValueEPKc+260)
    #06 pc 0000000000291710  /system/lib64/libart.so
```

* NPE on O/OMR1 in RenderThread

This might be caused by a race between the render of the first frame and the first hardware bitmap decode. If the hardware bitmap decode wins, we NPE. Unfortunately attempts to force Glide to wait to use hardware bitmaps until after a frame has been rendered haven't eliminated this issue. Either the implementation in Glide is missing some number of cases or there's some other issue on O that produces a similar stack trace.

```
pid: 16455, tid: 16504, name: RenderThread  >>> com.google.android.apps.photos <<<
signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x4
Cause: null pointer dereference
backtrace:
    #00 pc 003ec6dc  /vendor/lib/egl/libGLES_mali.so (cobj_instance_retain+8)
    #01 pc 003b0b28  /vendor/lib/egl/libGLES_mali.so (egl_get_egl_image_template_gles+156)
    #02 pc 00389a08  /vendor/lib/egl/libGLES_mali.so (gles_texture_egl_image_target_texture_2d_oes+216)
    #03 pc 00035a05  /system/lib/libhwui.so (android::uirenderer::debug::GlesErrorCheckWrapper::glEGLImageTargetTexture2DOES_(unsigned int, void*)+12)
    #04 pc 0004cb77  /system/lib/libhwui.so (android::uirenderer::renderthread::OpenGLPipeline::uploadBitmapToGraphicBuffer(android::uirenderer::Caches&, SkBitmap&, android::GraphicBuffer&, int, int)+114)
    #05 pc 0004cf49  /system/lib/libhwui.so (android::uirenderer::renderthread::OpenGLPipeline::allocateHardwareBitmap(android::uirenderer::renderthread::RenderThread&, SkBitmap&)+496)
    #06 pc 0005284f  /system/lib/libhwui.so (android::uirenderer::renderthread::RenderThread::allocateHardwareBitmap(SkBitmap&)+30)
    #07 pc 0005131d  /system/lib/libhwui.so (android::uirenderer::renderthread::Bridge_allocateHardwareBitmap(android::uirenderer::renderthread::allocateHardwareBitmapArgs*)+20)
    #08 pc 00051533  /system/lib/libhwui.so (android::uirenderer::renderthread::MethodInvokeRenderTask::run()+10)
    #09 pc 000516b3  /system/lib/libhwui.so (android::uirenderer::renderthread::SignalingRenderTask::run()+10)
    #10 pc 00052403  /system/lib/libhwui.so (android::uirenderer::renderthread::RenderThread::threadLoop()+178)
    #11 pc 0000d1e9  /system/lib/libutils.so (android::Thread::_threadLoop(void*)+144)
    #12 pc 00071681  /system/lib/libandroid_runtime.so (android::AndroidRuntime::javaThreadShell(void*)+80)
    #13 pc 00049107  /system/lib/libc.so (__pthread_start(void*)+22)
    #14 pc 0001b055  /system/lib/libc.so (__start_thread+32)
```

Another trace that might be from the same cause:

```
pid: 1610, tid: 1683, name: RenderThread  >>> com.google.android.apps.photos <<<
signal 6 (SIGABRT), code -6 (SI_TKILL), fault addr --------
Abort message: 'glTexSubImage2D error! GL_INVALID_OPERATION (0x502)'
    r0 00000000  r1 00000693  r2 00000006  r3 00000008
    r4 0000064a  r5 00000693  r6 93b752b4  r7 0000010c
    r8 00000000  r9 ae9708b4  sl ae970880  fp ae9708b4
    ip ae970854  sp 93b752a0  lr b043f87d  pc b04393ce  cpsr 20000030

backtrace:
    #00 pc 0001a3ce  /system/lib/libc.so (abort+63)
    #01 pc 0000655d  /system/lib/liblog.so (__android_log_assert+156)
    #02 pc 00031851  /system/lib/libhwui.so (android::uirenderer::debug::GlesErrorCheckWrapper::assertNoErrors(char const*)+192)
    #03 pc 0002f271  /system/lib/libhwui.so (wrap_glTexSubImage2D(unsigned int, int, int, int, int, int, unsigned int, unsigned int, void const*)+52)
    #04 pc 000547c7  /system/lib/libhwui.so (android::uirenderer::renderthread::OpenGLPipeline::allocateHardwareBitmap(android::uirenderer::renderthread::RenderThread&, SkBitmap&)+634)
    #05 pc 0005b0bf  /system/lib/libhwui.so (android::uirenderer::renderthread::RenderThread::allocateHardwareBitmap(SkBitmap&)+30)
    #06 pc 00059a89  /system/lib/libhwui.so (android::uirenderer::renderthread::Bridge_allocateHardwareBitmap(android::uirenderer::renderthread::allocateHardwareBitmapArgs*)+20)
    #07 pc 00059c9b  /system/lib/libhwui.so (android::uirenderer::renderthread::MethodInvokeRenderTask::run()+10)
    #08 pc 00059e1b  /system/lib/libhwui.so (android::uirenderer::renderthread::SignalingRenderTask::run()+10)
    #09 pc 0005ac73  /system/lib/libhwui.so (android::uirenderer::renderthread::RenderThread::threadLoop()+178)
    #10 pc 0000d1c9  /system/lib/libutils.so (android::Thread::_threadLoop(void*)+144)
    #11 pc 0006eef9  /system/lib/libandroid_runtime.so (android::AndroidRuntime::javaThreadShell(void*)+80)
    #12 pc 000475bf  /system/lib/libc.so (__pthread_start(void*)+22)
    #13 pc 0001af35  /system/lib/libc.so (__start_thread+32)
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
