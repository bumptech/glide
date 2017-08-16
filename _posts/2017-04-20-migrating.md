---
layout: page
title: "Migrating from v3 to v4"
category: doc
date: 2017-04-20 07:13:46
order: 12
disqus: 1
---
* TOC
{:toc}

## Options
One of the larger changes in Glide v4 is the way the library handles options (``centerCrop()``, ``placeholder()`` etc). In Glide v3, options were handled individually by a series of complicated multityped builders. In Glide v4 these have been replaced by a single builder with a single type and a series of options objects that can be provided to the builder. Glide's [generated API][11] simplifies this further by merging options from the options objects and from any included integration libraries with the builder to create a single fluent API.

### RequestBuilder

Includes methods like:
```java
listener()
thumbnail()
load()
into()
```

In Glide v4 there is only a single [``RequestBuilder``][5] with a single type that indicates the type of item you're attempting to load (``Bitmap``, ``Drawable``, ``GifDrawable`` etc). The ``RequestBuilder`` provides direct access to options that affect the load process itself, including the model (url, uri etc) you want to load, any [``thumbnail()``][6] requests and any [``RequestListener``s][7]. The ``RequestBuilder`` also is the place where you start the load using [``into()``][8] or [``preload()``][9]:

```java
RequestBuilder<Drawable> requestBuilder = Glide.with(fragment)
    .load(url);

requestBuilder
    .thumbnail(Glide.with(fragment)
        .load(thumbnailUrl))
    .listener(requestListener)
    .load(url)
    .into(imageView);
```

### RequestOptions

Includes methods like:
```java
centerCrop()
placeholder()
error()
priority()
diskCacheStrategy()
```

Most options have moved into a separate object called [``RequestOptions``][10]:

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
    

### TransitionOptions

Includes methods like:
```java
crossFade()
animate()
```

Options that control transitions from placeholders to images and/or between thumbnails and the full image have been moved into [``TransitionOptions``][13].

To apply transitions (formerly animations), use one of the transition options that matches the type of resource you're requesting:

* [``GenericTransitionOptions``][14]
* [``DrawableTransitionOptions``][15]
* [``BitmapTransitionOptions``][16]

To remove any default transition, use [``TransitionOptions.dontTransition()``][17].

Transitions are applied to a request using [``RequestBuilder``][5]:

```java
Glide.with(fragment)
    .load(url)
    .transition(withCrossFade(R.anim.fade_in, 300));
```

#### Default transition

Unlike Glide v3, Glide v4 does **NOT** apply a cross fade or any other transition by default to requests. Transitions must be applied manually.

### Generated API

To make it even easier to use Glide v4, Glide now also offers a generated API for Applications. Applications can access the generated API by including an appropriately annotated [``AppGlideModule``][2] implementation. See the [Generated API][11] page for details on how this works.

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

## Types and Targets

### Picking Resource Types

Glide allows you to specify what type of resource you want to load. If you specify a super type, Glide will attempt to load any available subtypes. For example, if you request a Drawable, Glide may load either a BitmapDrawable or a GifDrawable. If you request a GifDrawable, Glide will either load a GifDrawable or error if the image isn't a GIF (even if it happens to be a perfectly valid image).

Drawables are requested by default:

```java
Glide.with(fragment).load(url)
```
  
To request a Bitmap:

```java
Glide.with(fragment).asBitmap()
```

To obtain a filepath (best for local images):

```java
Glide.with(fragment).asFile()
```

To download a remote file into cache and obtain a file path:

```java
Glide.with(fragment).downloadOnly()
// or if you have the url already:
Glide.with(fragment).download(url);
```

### Drawables

``GlideDrawable`` in Glide v3 has been removed in favor of the standard Android [``Drawable``][18]. ``GlideBitmapDrawable`` has been removed in favor of [``BitmapDrawable``][19].

If you want to know if a Drawable is animated, you can check if it is an instance of [``Animatable``][20]:

```java
boolean isAnimated = drawable instanceof Animatable
```

### Targets

The signature of ``onResourceReady`` has changed. For example, for ``Drawables``:

```java
onResourceReady(GlideDrawable drawable, GlideAnimation<? super GlideDrawable> anim) 
```

is now:

```java
onResourceReady(Drawable drawable, Transition<? super Drawable> transition);
```

Similarly the signature of ``onLoadFailed`` has also changed:
```java
onLoadFailed(Exception e, Drawable errorDrawable)
```

is now:

```java
onLoadFailed(Drawable errorDrawable)
```

If you need more information about the errors that caused the load to fail, you can use [``RequestListener``][21].


#### Cancellation

``Glide.clear(Target)`` has moved into [``RequestManager``][22]:

```java
Glide.with(fragment).clear(target)
```

Although it's not required, it's most performant to use the ``RequestManager`` that started the load to also clear the load. Glide v4 keeps track of requests per Activity and Fragment so clearing needs to remove the request at the appropriate level.

## Configuration
In Glide v3, configuration is performed via one or more [``GlideModules``][1]. In Glide v4, configuration is done via a similar but slightly more sophisticated system.

For details on the new system, see the [Configuration][4] page.

### Applications

Applications that have a single [``GlideModule``][1] can convert their ``GlideModule`` into a [``AppGlideModule``][2].

In Glide v3, you might have a ``GlideModule`` like this:

```java
public class GiphyGlideModule implements GlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    builder.setMemoryCache(new LruResourceCache(10 * 1024 * 1024));
  }

  @Override
  public void registerComponents(Context context, Glide glide) {
    glide.register(Api.GifResult.class, InputStream.class, new GiphyModelLoader.Factory());
  }
}
```

In Glide v4, you would convert it into a ``AppGlideModule`` that looks like this:

```java
@GlideModule
public class GiphyGlideModule extends AppGlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    builder.setMemoryCache(new LruResourceCache(10 * 1024 * 1024));
  }

  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    registry.append(Api.GifResult.class, InputStream.class, new GiphyModelLoader.Factory());
  }
}
```

Note that the ``@GlideModule`` annotation is required.

If your application has multiple ``GlideModule``s, convert one of them to a ``AppGlideModule`` and the others to [``LibraryGlideModule``s][3]. ``LibraryGlideModule``s will not be discovered unless a ``AppGlideModule`` is present, so you cannot use only ``LibraryGlideModule``s. 

### Libraries

Libraries that have one or more ``GlideModule``s should use [``LibraryGlideModule``][3] instead of [``AppGlideModule``][2]. Libraries should not use [``AppGlideModule``s][2] because there can only be one per Application, so including it in a library would not only prevent users of the library from setting their own options, but it would also cause conflicts if multiple libraries included a ``AppGlideModule``. 

For example, the Volley ``GlideModule`` in v3:

```java
public class VolleyGlideModule implements GlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    // Do nothing.
  }

  @Override
  public void registerComponents(Context context, Glide glide) {
    glide.register(GlideUrl.class, InputStream.class, new VolleyUrlLoader.Factory(context));
  }
}
```

Can be converted to a ``LibraryGlideModule`` in v4:

```java
@GlideModule
public class VolleyLibraryGlideModule extends LibraryGlideModule {
  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    registry.replace(GlideUrl.class, InputStream.class, new VolleyUrlLoader.Factory(context));
  }
}
```

### Manifest parsing

To ease the migration, manifest parsing and the older [``GlideModule``][1] interface are deprecated, but still supported in v4. ``AppGlideModule``s, ``LibraryGlideModule``s and the deprecated ``GlideModule``s can all coexist in an application.

However, to avoid the performance overhead of checking metadata (and associated bugs), you can disable manifest parsing once your migration is complete by overriding a method in your ``AppGlideModule``:

```java
@GlideModule
public class GiphyGlideModule extends AppGlideModule {
  @Override
  public boolean isManifestParsingEnabled() {
    return false;
  }

  ...
}
```

### ``using()``, ModelLoader, StreamModelLoader.

#### ModelLoader

The [``ModelLoader``][26] API exists in Glide v4 and serves the same purpose that it did in Glide v3, but a few of the specifics have changed.

First specific subtypes of ``ModelLoader``, like ``StreamModelLoader`` are now unnecessary and users can implement ``ModelLoader`` directly. For example, a ``StreamModelLoader<File>`` would now be implemented and referred to as a ``ModelLoader<File, InputStream>``.

Second, instead of returning a ``DataFetcher`` directly, ``ModelLoader``s now return [``LoadData``][27]. ``LoadData`` is a very simple wrapper that contains a disk cache key and a ``DataFetcher``.

Third, ``ModelLoaders`` have a ``handles()`` method, so that you can register more than one ModelLoader with the same type parameters.

Converting a ``ModelLoader`` from the v3 API to the v4 API is almost always straight forward. If you just return a ``DataFetcher`` in your v3 ``ModelLoader``:

```java
public final class MyModelLoader implements StreamModelLoader<File> {

  @Override
  public DataFetcher<InputStream> getResourceFetcher(File model, int width, int height) {
    return new MyDataFetcher(model);
  }
}
```

Then all you need to do in your v4 equivalent is wrap the data fetcher:

```java
public final class MyModelLoader implements ModelLoader<File, InputStream> {

  @Override
  public LoadData<InputStream> buildLoadData(File model, int width, int height,
      Options options) {
    return new LoadData<>(model, new MyDataFetcher(model));
  }

  @Override
  public void handles(File model) {
    return true;
  }
}
```

Note that the model is passed in to the ``LoadData`` to act as part of the cache key, in addition to the ``DataFetcher``. This pattern provides more control over the disk cache key in some specialized circumstances. Most implementations can just pass their model directly into ``LoadData`` as is done above.

If you'd only like to use your ModelLoader for some models you can use the ``handles()`` method to inspect the model before you try to load it. If you return ``false`` from ``handles()`` your ``ModelLoader`` will not be to load the given model, even if the types of your ``ModelLoader`` (``File`` and ``InputStream`` in this example) match.

For example, if you're writing encrypted images to disk in a specific folder, you could use the ``handles()`` method to implement a ``ModelLoader`` that decrypted images from that specific folder but wasn't used when loading ``File``s from other folders:

```java
public final class MyModelLoader implements ModelLoader<File, InputStream> {
  private static final String ENCRYPTED_PATH = "/my/encrypted/folder";

  @Override
  public LoadData<InputStream> buildLoadData(File model, int width, int height,
      Options options) {
    return new LoadData<>(model, new MyDataFetcher(model));
  }

  @Override
  public void handles(File model) {
    return model.getAbsolutePath().startsWith(ENCRYPTED_PATH);
  }
}
```


#### ``using()``

The [``using()``][23] API was removed in Glide 4 to encourage users to [register][24] their components once with a [``AppGlideModule``][2] to avoid object re-use. Rather than creating a new ``ModelLoader`` each time you load an image, you register it once in an [``AppGlideModule``][2] and let Glide inspect your model (the object you pass to [``load()``][25]) to figure out when to use your registered ``ModelLoader``.

To make sure you only use your ``ModelLoader`` for certain models, implement ``handles()`` as shown above to inspect each model and return true only if your ``ModelLoader`` should be used.

[1]: {{ site.url }}/glide/javadocs/360/com/bumptech/glide/module/GlideModule.html
[2]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/module/AppGlideModule.html
[3]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/module/LibraryGlideModule.html
[4]: configuration.html
[5]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/RequestBuilder.html
[6]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/RequestBuilder.html#thumbnail-com.bumptech.glide.RequestBuilder-
[7]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/RequestBuilder.html#listener-com.bumptech.glide.request.RequestListener-
[8]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/RequestBuilder.html#into-Y-
[9]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/RequestBuilder.html#preload-int-int-
[10]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/request/RequestOptions.html
[11]: generatedapi.html
[12]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/annotation/GlideExtension.html
[13]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/load/resource/bitmap/BitmapTransitionOptions.html
[14]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/GenericTransitionOptions.html
[15]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/load/resource/drawable/DrawableTransitionOptions.html
[16]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/load/resource/bitmap/BitmapTransitionOptions.html
[17]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/TransitionOptions.html#dontTransition--
[18]: https://developer.android.com/reference/android/graphics/drawable/Drawable.html
[19]: https://developer.android.com/reference/android/graphics/drawable/BitmapDrawable.html
[20]: https://developer.android.com/reference/android/graphics/drawable/Animatable.html
[21]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/request/RequestListener.html
[22]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/RequestManager.html
[23]: {{ site.url }}/glide/javadocs/380/com/bumptech/glide/RequestManager.html#using(com.bumptech.glide.load.model.stream.StreamByteArrayLoader)
[24]: {{ site.url }}/glide/doc/generatedapi.html
[25]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/RequestBuilder.html#load-java.lang.Object-
[26]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/load/model/ModelLoader.html
[27]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/load/model/ModelLoader.LoadData.html
