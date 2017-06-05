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

Glide v4 uses an [annotation processor][1] to generate an API that allows applications to access all options in [``RequestBuilder``][2], [``RequestOptions``][3] and any included integration libraries in a single fluent API. 

The generated API serves two purposes:
1. Integration libraries can extend Glide's API with custom options.
2. Applications can extend Glide's API by adding methods that bundle commonly used options.

Although both of these tasks can be accomplished by hand by writing custom subclasses of [``RequestOptions``][3], doing so is challenging and produces a less fluent API. 

### Getting Started

To trigger the API generation, include a [``AppGlideModule``][4] implementation in your application:

```java
package com.example.myapp;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

@GlideModule
public final class MyAppGlideModule extends AppGlideModule {}
```

Note that [``AppGlideModule``][4] implementations must always be annotated with [``@GlideModule``][5]. If the annotation is not present, the module will not be discovered and you will see a warning in your logs with the ``Glide`` log tag that indicates that the module couldn't be found.

The API is generated in the same package as the [``AppGlideModule``][4] implementation provided by the application and is named ``GlideApp`` by default. Applications can use the API by starting all loads with ``GlideApp.with()`` instead of ``Glide.with()``:

```java
GlideApp.with(fragment)
   .load(myUrl)
   .placeholder(R.drawable.placeholder)
   .fitCenter()
   .into(imageView);
```

Note that unlike ``Glide.with()`` options like ``fitCenter()`` and ``placeholder()`` are available directly on the builder and don't need to be passed in as a separate [``RequestOptions``][3] object.

### GlideExtension

Glide's generated API can be extended by both Applications and Libraries. Extensions use annotated static methods to add new options, modifying existing options, or add additional types.

The [``GlideExtension``][6] annotation identifies a class that extends Glide's API. The annotation must be present on any classes that extend Glide's API. If the annotation is not present, annotations on methods will be ignored.

Classes annotated with [``GlideExtension``][6] are expected to be utility classes. They should have a private and empty constructor. Classes annotated with GlideExtension should also be final and contain only static methods. Annotated classes may contain static variables and may reference other classes or objects.

An application may implement as many [``GlideExtension``][6] annotated classes as they'd like. Libraries can also implement an arbitrary number of [``GlideExtension``][6] annotated classes. When a [``AppGlideModule``][4] is found, all available [``GlideExtensions``][6] will be merged to create a single API with all available extensions. Conflicts will result in compliation errors in Glide's annotation processor.

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
   
  @GlideOption
  public static void miniThumb(RequestOptions options) {
    options
      .fitCenter()
      .override(MINI_THUMB_SIZE);
  }
```

This will generate a method in a [``RequestOptions``][3] subclass that looks like this:

```java
public class GlideOptions extends RequestOptions {
  
  public GlideOptions miniThumb() {
    MyAppExtension.miniThumb(this);
  }

  ...
}
```

You can include as many additional arguments in your methods as you want, as long as the first argument is always [``RequestOptions``][9]:

```java
@GlideOption
public static void miniThumb(RequestOptions options, int size) {
  options
    .fitCenter()
    .override(size);
}
```

The additional arguments will be added as arguments to the generated method:

```java
public GlideOptions miniThumb(int size) {
  MyAppExtension.miniThumb(this);
}
```

You can then call your custom type by using the generated ``GlideApp`` class:

```java
GlideApp.with(fragment)
   .load(url)
   .miniThumb(thumbnailSize)
   .into(imageView);
```

Methods with the ``GlideOption`` annotation are expected to be static and to return void. Note that the generated methods will not be available on the standard ``Glide`` and ``RequestOptions`` classes.

#### GlideType

[``GlideType``][8] annotated static methods extend [``RequestManager``][11]. ``GlideType`` annotated methods allow you to add support for new types, including specifying default options.

For example, to add support for GIFs, you might add a ``GlideType`` method:

```java
@GlideExtension
public class MyAppExtension {
  private static final RequestOptions DECODE_TYPE_GIF = decodeTypeOf(GifDrawable.class).lock();

  @GlideType(GifDrawable.class)
  public static void asGif(RequestBuilder<GifDrawable> requestBuilder) {
    requestBuilder
      .transition(new DrawableTransitionOptions())
      .apply(DECODE_TYPE_GIF);
  }
}
```

Doing so will generate a [``RequestManager``][11] with a method that looks like this:

```java
public class GlideRequests extends RequesetManager {

  public RequestBuilder<GifDrawable> asGif() {
    RequestBuilder<GifDrawable> builder = as(GifDrawable.class);
    MyAppExtension.asGif(builder);
    return builder;
  }
  
  ...
}
```

You can then use the generated ``GlideApp`` class to call your custom method:

```java
GlideApp.with(fragment)
  .asGif()
  .load(url)
  .into(imageView);
```

Methods annotated with ``GlideType`` must take a [``RequestBuilder<T>``][2] as their first argument where the type ``<T>`` matches the class provided to the [``GlideType``][8] annotation. Methods are expected to be static and return void. Methods must be defined in a class annotated with [``GlideExtension``][6]. 


[1]: https://docs.oracle.com/javase/8/docs/api/javax/annotation/processing/Processor.html
[2]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/RequestBuilder.html
[3]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/request/RequestOptions.html
[4]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/module/AppGlideModule.html
[5]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/annotation/GlideModule.html
[6]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/annotation/GlideExtension.html
[7]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/annotation/GlideOption.html
[8]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/annotation/GlideType.html
[9]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/request/RequestOptions.html
[10]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/load/Option.html
[11]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/RequestManager.html
