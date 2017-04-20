---
layout: page
title: "Migrating from v3 to v4"
category: doc
date: 2017-04-20 07:13:46
order: 10
disqus: 1
---
* TOC
{:toc}

## Options
One of the larger changes in Glide v4 is the way the library handles options (``centerCrop()``, ``placeholder()`` etc). In Glide v3, options were handled individually by a series of complicated multityped builders. 

### RequestBuilder

In Glide v4 there is only a single [``RequestBuilder``][5] with a single type that indicates the type of item you're attempting to load (``Bitmap``, ``Drawable``, ``GifDrawable`` etc). The ``RequestBuilder`` provides direct access to options that affect the load process itself, including the model (url, uri etc) you want to load, any [``thumbnail()``][6] requests and any [``RequestListener``s][7]. The ``RequestBuilder`` also is the place where you start the load using [``into()``][8] or [``preload()``][9]:

```java
RequestBuilder<Drawable> requestBuilder = Glide.with(fragment)
    .load(url);

requestBuilder
    .thumbnail(Glide.with(fragment)
        .override(100, 100)
        .load(url))
    .listener(requestListener)
    .into(imageView);
```

### RequestOptions

Options like ``centerCrop()``, ``placeholder()`` and others are moved into a separate object called [``RequestOptions``][10]:

```
RequestOptions options = new RequestOptions()
    .centerCrop()
    .placeholder(R.drawable.placeholder)
    .error(R.drawable.error)
    .priority(Priority.HIGH);
```

``RequestOptions`` are applied to ``RequestBuilders`` to allow you to specify a set of options once and then use them for multiple loads:

```java
RequestOptions myOptions = new RequestOptions()
    .fitCenter()
    .override(100, 100);

Glide.with(fragment)
    .load(url)
    .apply(myOptions)
    .into(drawableView);

Glide.with(fragment)
    .asBitmap()
    .apply(myOptions)
    .load(url)
    .into(bitmapView);
```
    

### Generated API

To make it even easier to use Glide v4, Glide now also offers a generated API for Applications. Applications can access the generated API by including an appropriately annotated [``RootGlideModule``][[2] implementation. See the [Generated API][11] page for details on how this works.

The generated API adds a ``GlideApp`` class, that provides access to ``RequestBuilder`` and ``RequestOptions`` subclasses. The ``RequestOptions`` subclass contains all methods in ``RequestOptions`` and any methods defined in [``GlideExtensions``][12]. The ``RequestBuilder`` subclass provides access to all methods in the generated ``RequestOptions`` subclass without having to use ``apply``:

A request without the generated API might look like this:

```java
Glide.with(fragment)
    .load(url)
    .apply(centerCropTransform()
        .placeholder(R.drawable.placeholder)
        .error(R.drawable.error)
        .priority(Priority.HIGH))
    .into(imageView);
```

With the generated API, the ``RequestOptions`` calls can be inlined:

```java
GlideApp.with(fragment)
    .load(url)
    .centerCrop()
    .placeholder(R.drawable.placeholder)
    .error(R.drawable.error)
    .priority(Priority.HIGH)
    .into(imageView);
```

You can still use the generated ``RequestOptions`` subclass to apply the same set of options to multiple loads, but generated ``RequestBuilder`` subclass may be more convenient in most cases.

## Configuration
In Glide v3, configuration is performed via one or more [``GlideModules``][1]. In Glide v4, configuration is done via a similar but slightly more sophisticated system.

For details on the new system, see the [Configuration][4] page.

### Applications

Applications that have a single [``GlideModule``][1] can convert their ``GlideModule`` into a [``RootGlideModule``][2].

In Glide v3, you might have a ``GlideModule`` like this:

```java
public class GiphyGlideModule implements GlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    builder.setMemoryCache(new LruResourceCache(10 * 1024 * 1024));
  }

  @Override
  public void registerComponents(Context context, Registry registry) {
    registry.append(Api.GifResult.class, InputStream.class, new GiphyModelLoader.Factory());
  }
}
```

In Glide v4, you would convert it into a ``RootGlideModule`` that looks like this:

```java
@GlideModule
public class GiphyGlideModule extends RootGlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    builder.setMemoryCache(new LruResourceCache(10 * 1024 * 1024));
  }

  @Override
  public void registerComponents(Context context, Registry registry) {
    registry.append(Api.GifResult.class, InputStream.class, new GiphyModelLoader.Factory());
  }
}
```

Note that the ``@GlideModule`` annotation is required.

If your application has multiple ``GlideModule``s, convert one of them to a ``RootGlideModule`` and the others to [``ChildGlideModule``s][3]. ``ChildGlideModule``s will not be discovered unless a ``RootGlideModule`` is present, so you cannot use only ``ChildGlideModule``s. 

### Libraries

Libraries that have one or more ``GlideModule``s should use [``ChildGlideModule``][3] instead of [``RootGlideModule``][2]. Libraries should not use [``RootGlideModule``s][2] because there can only be one per Application, so including it in a library would not only prevent users of the library from setting their own options, but it would also cause conflicts if multiple libraries included a ``RootGlideModule``. 

For example, the Volley ``GlideModule`` in v3:

```java
public class VolleyGlideModule implements GlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    // Do nothing.
  }

  @Override
  public void registerComponents(Context context, Registry registry) {
    registry.replace(GlideUrl.class, InputStream.class, new VolleyUrlLoader.Factory(context));
  }
}
```

Can be converted to a ``ChildGlideModule`` in v4:

```java
@GlideModule
public class VolleyChildGlideModule extends ChildGlideModule {
  @Override
  public void registerComponents(Context context, Registry registry) {
    registry.replace(GlideUrl.class, InputStream.class, new VolleyUrlLoader.Factory(context));
  }
}
```

### Manifest parsing

To ease the migration, manifest parsing and the older [``GlideModule``][1] interface are deprecated, but still supported in v4. ``RootGlideModule``s, ``ChildGlideModule``s and the deprecated ``GlideModule``s can all coexist in an application.

However, to avoid the performance overhead of checking metadata (and associated bugs), you can disable manifest parsing once your migration is complete by overriding a method in your ``RootGlideModule``:

```java
@GlideModule
public class GiphyGlideModule extends RootGlideModule {
  @Override
  public boolean isManifestParsingEnabled() {
    return false;
  }

  ...
}
```





[1]: http://sjudd.github.io/glide/javadocs/360/com/bumptech/glide/module/GlideModule.html
[2]: http://sjudd.github.io/glide/javadocs/400/com/bumptech/glide/module/RootGlideModule.html
[3]: http://sjudd.github.io/glide/javadocs/400/com/bumptech/glide/module/ChildGlideModule.html
[4]: configuration.html
[5]: http://sjudd.github.io/glide/javadocs/400/com/bumptech/glide/RequestBuilder.html
[6]: http://sjudd.github.io/glide/javadocs/400/com/bumptech/glide/RequestBuilder.html#thumbnail-com.bumptech.glide.RequestBuilder-
[7]: http://sjudd.github.io/glide/javadocs/400/com/bumptech/glide/RequestBuilder.html#listener-com.bumptech.glide.request.RequestListener-
[8]: http://sjudd.github.io/glide/javadocs/400/com/bumptech/glide/RequestBuilder.html#into-Y-
[9]: http://sjudd.github.io/glide/javadocs/400/com/bumptech/glide/RequestBuilder.html#preload-int-int-
[10]: http://sjudd.github.io/glide/javadocs/400/com/bumptech/glide/request/RequestOptions.html
[11]: generatedapi.html
[12]: http://sjudd.github.io/glide/javadocs/400/com/bumptech/glide/annotation/GlideExtension.html
