---
layout: page
title: "Generated API"
category: doc
date: 2017-04-17 07:28:51
order: 3
disqus: 1
---

### About

Glide v4 uses an [annotation processor][1] to generate an API that allows applications to access all options in [``RequestBuilder``][2], [``RequestOptions``][3] and any included integration libraries in a single fluent API. 

The generated API serves two purposes:
1. Integration libraries can extend Glide's API with custom options.
2. Applications can extend Glide's API by adding methods that bundle commonly used options.

Although both of these tasks can be accomplished by hand by writing custom subclasses of [``RequestOptions``][3], doing so is challenging and produces a less fluent API.

### Getting Started

To trigger the API generation, include a [``RootGlideModule``][4] implementation in your application:

```java
package com.example.myapp;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.RootGlideModule;

@GlideModule
public final class MyAppGlideModule extends RootGlideModule {}
```

Note that [``RootGlideModule``][4] implementations must always be annotated with [``@GlideModule``][5]. 

The API is generated in the same package as the [``RootGlideModule``][4] implementatino provided by the application and is named ``GlideApp`` by default. Applications can use the API by starting all loads with ``GlideApp.with()`` instead of ``Glide.with()``:

```java
GlideApp.with(fragment)
   .placeholder(placeholder)
   .fitCenter()
   .load(myUrl)
   .into(imageView);
```

[1]: https://docs.oracle.com/javase/8/docs/api/javax/annotation/processing/Processor.html
[2]: http://sjudd.github.io/glide/javadocs/400/com/bumptech/glide/RequestBuilder.html
[3]: http://sjudd.github.io/glide/javadocs/400/com/bumptech/glide/request/RequestOptions.html
[4]: http://sjudd.github.io/glide/javadocs/400/com/bumptech/glide/module/RootGlideModule.html
[5]: http://sjudd.github.io/glide/javadocs/400/com/bumptech/glide/annotation/GlideModule.html
