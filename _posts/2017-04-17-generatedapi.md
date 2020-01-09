---
layout: page
title: "Generated API"
category: doc
date: 2017-04-17 07:28:51
order: 3
disqus: 1
---
* TOC
{:toc}

### About

Glide v4 uses an [annotation processor][1] to generate an API that allows applications to extend Glide's API and include components provided by integration libraries.

The generated API serves two purposes:
1. Integration libraries can extend Glide's API with custom options.
2. Applications can extend Glide's API by adding methods that bundle commonly used options.

Although both of these tasks can be accomplished by hand by writing custom subclasses of [``RequestOptions``][3], doing so is challenging and produces a less fluent API.

### Getting Started

#### Availability

The generated API is only available for applications for now. Limiting the generated API to applications allows us to have a single implementation of the API, instead of N implementations, one per library and the application. As a result, it's much simpler to manage imports and ensure that all call paths within a particular application have the correct options applied. This restriction may be lifted (experimentally or otherwise) in a future version. 

For now the API is only generated when a properly annotated ``AppGlideModule`` is found. There can only be one ``AppGlideModule`` per application. As a result it's not possible to generate the API for a library without precluding any application that uses the library from using the generated API. 

#### Java

To use the generated API in your application, you need to perform two steps:

1. Add a dependency on Glide's annotation processor:

   ```groovy
   repositories {
     mavenCentral()
   }

   dependencies {
     annotationProcessor 'com.github.bumptech.glide:compiler:4.11.0'
   }
   ```

   See the [download and setup page][12] for more detail.

2. Include a [``AppGlideModule``][4] implementation in your application:

   ```java
   package com.example.myapp;

   import com.bumptech.glide.annotation.GlideModule;
   import com.bumptech.glide.module.AppGlideModule;

   @GlideModule
   public final class MyAppGlideModule extends AppGlideModule {}
   ```

You're not required to implement any of the methods in ``AppGlideModule`` for the API to be generated. You can leave the class blank as long as it extends ``AppGlideModule`` and is annotated with ``@GlideModule``.

[``AppGlideModule``][4] implementations must always be annotated with [``@GlideModule``][5]. If the annotation is not present, the module will not be discovered and you will see a warning in your logs with the ``Glide`` log tag that indicates that the module couldn't be found.

**Note:** Libraries should **not** include [``AppGlideModule``][4] implementations. See the [configuration][15] page for details.

#### Kotlin

If you're using Kotlin you can:

1. Implement all of Glide's annotated classes ([``AppGlideModule``][4], [``LibraryGlideModule``][13], and [``GlideExtension``][6]) in Java as shown above.
2. Implement the annotated classes in Kotlin, but add a ``kapt`` dependency instead of an ``annotationProcessor`` dependency on Glide's annotation processor:

   ```groovy
   dependencies {
     kapt 'com.github.bumptech.glide:compiler:4.11.0'
   }
   ```
  Note that you must also include the ``kotlin-kapt`` plugin in your ``build.gradle`` file:

   ```groovy
   apply plugin: 'kotlin-kapt'
   ```
  In addition, if you have any other annotation processors, all of them must be converted from ``annotationProcessor`` to ``kapt``:

   ```groovy
   dependencies {
     kapt "android.arch.lifecycle:compiler:1.0.0"
     kapt 'com.github.bumptech.glide:compiler:4.11.0'
   }
   ```

   For more details on ``kapt``, see the [official documentation][14].


#### Android Studio

For the most part Android Studio just works with annotation processors and the generated API. However, you may need to rebuild the project the first time you add your ``AppGlideModule`` or after some types of changes. If the API won't import or seems out of date, you can re-build by:

1. Open the Build menu
2. Click Rebuild Project.

### Using the generated API
 
The API is generated in the same package as the [``AppGlideModule``][4] implementation provided by the application and is named ``GlideApp`` by default. Applications can use the API by starting all loads with ``GlideApp.with()`` instead of ``Glide.with()``:
 
```java
GlideApp.with(fragment)
   .load(myUrl)
   .placeholder(R.drawable.placeholder)
   .fitCenter()
   .into(imageView);
```
 
Unlike ``Glide.with()`` options like ``fitCenter()`` and ``placeholder()`` are available directly on the builder and don't need to be passed in as a separate [``RequestOptions``][3] object.
    
### GlideExtension

Glide's generated API can be extended by both Applications and Libraries. Extensions use annotated static methods to add new options, modifying existing options, or add additional types.

The [``GlideExtension``][6] annotation identifies a class that extends Glide's API. The annotation must be present on any classes that extend Glide's API. If the annotation is not present, annotations on methods will be ignored.

Classes annotated with [``GlideExtension``][6] are expected to be utility classes. They should have a private and empty constructor. Classes annotated with GlideExtension should also be final and contain only static methods. Annotated classes may contain static variables and may reference other classes or objects.

An application may implement as many [``GlideExtension``][6] annotated classes as they'd like. Libraries can also implement an arbitrary number of [``GlideExtension``][6] annotated classes. When a [``AppGlideModule``][4] is found, all available [``GlideExtensions``][6] will be merged to create a single API with all available extensions. Conflicts will result in compilation errors in Glide's annotation processor.

GlideExtension annotated classes can define two types of extension methods:

1. [``GlideOption``][7] - Adds a custom option to [``RequestOptions``][3].
2. [``GlideType``][8] - Adds support for a new resource type (GIFs, SVG etc).

#### GlideOption

[``GlideOption``][7] annotated static methods extend [``RequestOptions``][3]. ``GlideOption`` is useful to:

1. Define a group of options that is used frequently throughout an application.
2. Add new options, typically in conjunction with Glide's [``Option``][10] class.

To define a group of options, you might write:

```java
@GlideExtension
public class MyAppExtension {
  // Size of mini thumb in pixels.
  private static final int MINI_THUMB_SIZE = 100;

  private MyAppExtension() { } // utility class

  @NonNull
  @GlideOption
  public static BaseRequestOptions<?> miniThumb(BaseRequestOptions<?> options) {
    return options
      .fitCenter()
      .override(MINI_THUMB_SIZE);
  }
```

This will generate a method in a [``RequestOptions``][3] subclass that looks like this:

```java
public class GlideOptions extends RequestOptions {
  
  public GlideOptions miniThumb() {
    return (GlideOptions) MyAppExtension.miniThumb(this);
  }

  ...
}
```

You can include as many additional arguments in your methods as you want, as long as the first argument is always [``RequestOptions``][9]:

```java
@GlideOption
public static BaseRequestOptions<?> miniThumb(BaseRequestOptions<?> options, int size) {
  return options
    .fitCenter()
    .override(size);
}
```

The additional arguments will be added as arguments to the generated method:

```java
public GlideOptions miniThumb(int size) {
  return (GlideOptions) MyAppExtension.miniThumb(this);
}
```

You can then call your custom method by using the generated ``GlideApp`` class:

```java
GlideApp.with(fragment)
   .load(url)
   .miniThumb(thumbnailSize)
   .into(imageView);
```

Methods with the ``GlideOption`` annotation are expected to be static and to return `BaseRequestOptions<?>`. Note that the generated methods will not be available on the standard ``Glide`` and ``RequestOptions`` classes, only the generated equivalents.

#### GlideType

[``GlideType``][8] annotated static methods extend [``RequestManager``][11]. ``GlideType`` annotated methods allow you to add support for new types, including specifying default options.

For example, to add support for GIFs, you might add a ``GlideType`` method:

```java
@GlideExtension
public class MyAppExtension {
  private static final RequestOptions DECODE_TYPE_GIF = decodeTypeOf(GifDrawable.class).lock();

  @NonNull
  @GlideType(GifDrawable.class)
  public static RequestBuilder<GifDrwable> asGif(RequestBuilder<GifDrawable> requestBuilder) {
    return requestBuilder
      .transition(new DrawableTransitionOptions())
      .apply(DECODE_TYPE_GIF);
  }
}
```

Doing so will generate a [``RequestManager``][11] with a method that looks like this:

```java
public class GlideRequests extends RequesetManager {

  public GlideRequest<GifDrawable> asGif() {
    return (GlideRequest<GifDrawable> MyAppExtension.asGif(this.as(GifDrawable.class));
  }
  
  ...
}
```

You can then use the generated ``GlideApp`` class to call your custom type:

```java
GlideApp.with(fragment)
  .asGif()
  .load(url)
  .into(imageView);
```

Methods annotated with ``GlideType`` must take a [``RequestBuilder<T>``][2] as their first argument where the type ``<T>`` matches the class provided to the [``GlideType``][8] annotation. Methods are expected to be static and return `RequestBuilder<T>`. Methods must be defined in a class annotated with [``GlideExtension``][6]. 


[1]: https://docs.oracle.com/javase/8/docs/api/javax/annotation/processing/Processor.html
[2]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/RequestBuilder.html
[3]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/request/RequestOptions.html
[4]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/module/AppGlideModule.html
[5]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/annotation/GlideModule.html
[6]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/annotation/GlideExtension.html
[7]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/annotation/GlideOption.html
[8]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/annotation/GlideType.html
[9]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/request/RequestOptions.html
[10]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/Option.html
[11]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/RequestManager.html
[12]: {{ site.baseurl }}/doc/download-setup.html
[13]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/module/LibraryGlideModule.html
[14]: https://kotlinlang.org/docs/reference/kapt.html
[15]: {{ site.baseurl }}/doc/configuration.html#avoid-appglidemodule-in-libraries
