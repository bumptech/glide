---
layout: page
title: "Writing a custom ModelLoader"
category: tut
date: 2017-12-05 19:54:12
disqus: 1
---
* TOC
{:toc}

Although Glide provides out of the box support for most common types of models (URLs, Uris, file paths etc), you may occasionally run into a type that Glide doesn't support. You may also run in to cases where you want to customize or tweak Glide's default behavior. You may even want to integrate a new way of fetching images or a new networking library beyond those available in Glide's [integration libraries][3].

Fortunately Glide is extensible. To add support for a new type of model, you'll need to follow three steps:

1. Implement a [``ModelLoader``][1]
2. Implement a [``DataFetcher``][2] that can be returned by your [``ModelLoader``][1]
3. Register your new [``ModelLoader``][3] with Glide using an [``AppGlideModule``][4] (or [``LibraryGlideModule``][5] if you're working on a library rather than an application).

So that we have something to follow along with, let's implement a custom ``ModelLoader`` that takes Base64 encoded image Strings and decodes them with Glide. Note that if you actually want to do this in your application, it would be better to retrieve the Base64 encoded Strings in your ``ModelLoader`` so that you can avoid the CPU and memory overhead of loading them into memory if Glide has previously cached your image. 

For our purposes though, loading a Base64 image should provide a simple example, even if it might be a bit inefficient in the real world.

## Writing the ModelLoader.

The first step is to implement the [``ModelLoader``][1] interface. Before we do so, we need to make two decisions:

1. What type of Model should we handle?
2. What type of Data should we produce for that Model?

In this case we'd like to handle base 64 encoded Strings, so that means ``String`` is probably a reasonable choice for our Model type. Later on we'll need something more specific than just any random String, but for now, String is sufficient to start our implementation.

Next we need to decide what type of data we should try to obtain from our String. By default, Glide provides image decoders for two types of data:

1. [``InputStream``][7]
2. [``ByteBuffer``][8]

Glide also provides default support for [``ParcelFileDescriptor``][9] for decoding videos.

Since we're decoding an Image, we probably want [``InputStream``][7] or [``ByteBuffer``][8]. Since we already have all of the data in memory and the methods in [``Base64``][10], which we'll be using to do the actual decoding, return ``byte[]``, [``ByteBuffer``][8] is probably the best choice for our data.

### An empty implementation

Now that we know our ``Model`` and ``Data`` types, we can create a class that accepts the right types and returns default values:

```java
package judds.github.com.base64modelloaderexample;

import android.support.annotation.Nullable;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Loads an {@link InputStream} from a Base 64 encoded String.
 */
public final class Base64ModelLoader implements ModelLoader<String, ByteBuffer> {

  @Nullable
  @Override
  public LoadData<ByteBuffer> buildLoadData(String model, int width, int height, Options options) {
    return null;
  }

  @Override
  public boolean handles(String model) {
    return false;
  }
}
```

Of course this ``ModelLoader`` won't do much, but it's a start.

### Implementing ``handles()``

The next step is to implement the [``handles()``][11] method. As we mentioned earlier, there are a number of different types of Models that a String might be, including:

1. URLs
2. Uris
3. File paths

The ``handles()`` method allows the ``ModelLoader`` to efficiently check each model and avoid loading unsupported types.

To make our jobs easier, let's assume that our base64 encoded Strings will actually be passed to us as [data URIs][12]. The goal of the handles method is to identify any Strings passed in to Glide that match the data uri format and return ``true`` for only those strings.


Fortunately the data URI format seems pretty straight forward, so we can just check for the ``data:`` prefix:
```java
  @Override
  public boolean handles(String model) {
    return model.startsWith("data:");
  }
```

Depending on how robust we want our implementation to be, we might also want to check for the embedded image type or the format of the data, so that we don't try to load the bytes for an html page for example. We'll skip that here for now for simplicities sake.

### Implementing ``buildLoadData``

Now that we're able to identify data URIs, the next step is to provide an object that can decode the actual ``ByteBuffer`` if it's not already in cache for our given model, dimensions, and options. To do so we need to implement the [``buildLoadData``][13] method. 

To start, our method just returns ``null``, which is perfectly valid, although not very useful:

```java
  @Nullable
  @Override
  public LoadData<ByteBuffer> buildLoadData(String model, int width, int height, Options options) {
    return null;
  }
```

To make our method more useful, let's start by returning a new [``LoadData<ByteBuffer>``][14] object. To do so, we're going to need two things:

1. A [``Key``][15] that will be used as part of our disk cache keys (the model's ``equals()`` and ``hashCode()`` methods are used for the in memory cache key).
2. A [``DataFetcher``][2] that can obtain a ``ByteBuffer`` for our particular model.

#### Picking the ``Key``

The [``Key``][15] for the disk cache is straight forward in this case because our model type is a ``String``. If you have a model type that can be serialized using [``toString()``][16], you can just pass your model into a new [``ObjectKey``][17]:

```java
Key diskCacheKey = new ObjectKey(model);
```

Otherwise, you'd want to implement the [``Key``][15] interface here too, making sure that the ``equals()``, ``hashCode()`` and [``updateDiskCacheKey``][18] methods were all filled out and uniquely and consistently identified your particular model.

Since we're literally working with Strings here, [``ObjectKey``][17] will work just fine.

#### Picking the DataFetcher

Since we're adding support for a new Model, we're actually going to want a custom DataFetcher. In some cases [``ModelLoader``][1]s may actually just do a bit of parsing in [``buildLoadData``][13] and delegate to another [``ModelLoader``][1], but we're not so lucky here.

For now, let's just pass in ``null`` here, even though it's not a valid value, and move on to our actual ``DataFetcher`` implementation:

```java
  @Nullable
  @Override
  public LoadData<ByteBuffer> buildLoadData(String model, int width, int height, Options options) {
    return new LoadData<>(new ObjectKey(model), /*fetcher=*/ null);
  }
```

## Writing the ``DataFetcher``

Like [``ModelLoader``][1], the [``DataFetcher``][2] interface is generic and requires us to specify the type of data we expect it to return. Fortunately we already decided that we wanted to support loading ``ByteBuffer``s, so there's no difficult decision to make. 

As a result, we can quickly stub out an implementation:

```java
public class Base64DataFetcher implements DataFetcher<ByteBuffer> {

  @Override
  public void loadData(Priority priority, DataCallback<? super ByteBuffer> callback) {}

  @Override
  public void cleanup() {}

  @Override
  public void cancel() {}

  @NonNull
  @Override
  public Class<ByteBuffer> getDataClass() {
    return null;
  }

  @NonNull
  @Override
  public DataSource getDataSource() {
    return null;
  }
}
```

Although there are a number of methods here, most of them are actually pretty easy to implement.

#### getDataClass
``getDataClass()`` is trivial, we're loading ``ByteBuffer``:

```java
  @NonNull
  @Override
  public Class<ByteBuffer> getDataClass() {
    return ByteBuffer.class;
  }
```

#### getDataSource
``getDataSource()`` is almost as trivial, but it has some implications. Glide's default caching strategy is different for local images than it is for remote images. Glide assumes that it's easy and cheap to retrieve local images, so we default to caching images after they've been downsampled and transformed. In contrast, Glide assumes that it's difficult and expensive to retrieve remote images, so we default to caching the original data we retrieved.

For base64 ``String``s, the best choice for your app might depend on how you retrieve the ``String``s. If they're loaded from a local database, ``DataSource.LOCAL`` makes the most sense. If you're retrieving them via HTTP every time, ``DataSource.REMOTE`` is a better choice.

Let's assume the ``String``s are obtained locally for now:

```java
  @NonNull
  @Override
  public DataSource getDataSource() {
    return DataSource.LOCAL;
  }
```

#### cancel
For networking libraries or long running loads where cancellation is possible, it's a good idea to implement the ``cancel()`` method. Doing so will help speed up other queued loads and will save some amount of CPU, memory, or other resources.

In our case, [``Base64``][10] doesn't offer a cancellation API, so we can leave it blank:

```java
  @Override
  public void cancel() {
    // Intentionally empty.
  }

```

#### cleanup
``cleanup()`` is an interesting one. If you're loading an [``InputStream``][7] or opening any kind of I/O resources, you absolutely must close and clean up the ``InputStream`` or resource in the ``cleanup()`` method.

However, in our case we're just decoding an in memory model into in memory data. As a result, there's nothing to clean up, so our method can also be empty:

```java
  @Override
  public void cleanup() {
    // Intentionally empty only because we're not opening an InputStream or another I/O resource!
  }
```

Danger! Make sure that if you open an I/O resource or an ``InputStream`` you must close it here! We can only get away with leaving this method blank because we're not doing so! :)

#### loadData

Now for the fun part! ``loadData()`` is the method where Glide expects you to do your heavy lifting. You can queue an asynchronous task, start a network request, load some data from disk or whatever you'd like. ``loadData()`` is always called on one of Glide's background threads. A given ``DataFetcher`` will only be used on a single background thread at a time, so it doesn't need to be thread safe. However, multiple ``DataFetcher``s may be run in parallel, so any shared resources accessed by ``DataFetcher``s should be thread safe. 

``loadData()`` provides two arguments:

1. [``Priority``][19], which can be used to prioritized requests if you're using a networking library or other queueing system.
2. [``DataCallback``][20] which should be called with either your decoded data, or an error message if your load fails for any reason.

We can either queue an async task and call the given [``DataCallback``][20] asynchronously, or we can do some work inside the ``loadData()`` method and call the [``DataCallback``][20] directly.

In our case we don't have any network or other queue to call, so we can just do our work in line.

Note that one important thing is missing here. We don't have a reference to our model! This is because each [``DataFetcher``][2] is basically a closure that can be used to obtain data for a particular model. As a result, Glide expects you to pass the model into the constructor of the [``DataFetcher``][2]:

```java
  private final String model;

  Base64DataFetcher(String model) {
    this.model = model;
  }
```

As it turns out, our ``loadData()`` method is now actually rather simple. We just need to parse out the base64 section of the data uri:

```java
  private String getBase64SectionOfModel() {
    // See https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/Data_URIs.
    int startOfBase64Section = model.indexOf(',');
    return model.substring(startOfBase64Section + 1);
  }
```

Then we need to decode the ``byte[]``s of the base64 section:

```java
  byte[] data = Base64.decode(base64Section, Base64.DEFAULT);
```

And convert it to a ``ByteBuffer``:

```java
  ByteBuffer byteBuffer = ByteBuffer.wrap(data);
```

Then we just need to call the callback with our decoded ``ByteBuffer``:

```java
  callback.onDataReady(byteBuffer);
```

With everything together, here's the complete ``loadData()`` implementation:

```java
  @Override
  public void loadData(Priority priority, DataCallback<? super ByteBuffer> callback) {
    String base64Section = getBase64SectionOfModel();
    byte[] data = Base64.decode(base64Section, Base64.DEFAULT);
    ByteBuffer byteBuffer = ByteBuffer.wrap(data);
    callback.onDataReady(byteBuffer);
  }

  private String getBase64SectionOfModel() {
    // See https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/Data_URIs.
    int startOfBase64Section = model.indexOf(',');
    return model.substring(startOfBase64Section + 1);
  }
```

#### The full DataFetcher

Now that we've got all the methods in ``DataFetcher`` implemented, let's take one more look at it all together:

```java
package judds.github.com.base64modelloaderexample;

import android.support.annotation.NonNull;
import android.util.Base64;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import java.nio.ByteBuffer;

public class Base64DataFetcher implements DataFetcher<ByteBuffer> {

  private final String model;

  Base64DataFetcher(String model) {
    this.model = model;
  }

  @Override
  public void loadData(Priority priority, DataCallback<? super ByteBuffer> callback) {
    String base64Section = getBase64SectionOfModel();
    byte[] data = Base64.decode(base64Section, Base64.DEFAULT);
    ByteBuffer byteBuffer = ByteBuffer.wrap(data);
    callback.onDataReady(byteBuffer);
  }

  private String getBase64SectionOfModel() {
    // See https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/Data_URIs.
    int startOfBase64Section = model.indexOf(',');
    return model.substring(startOfBase64Section + 1);
  }

  @Override
  public void cleanup() {
    // Intentionally empty only because we're not opening an InputStream or another I/O resource!
  }

  @Override
  public void cancel() {
    // Intentionally empty.
  }

  @NonNull
  @Override
  public Class<ByteBuffer> getDataClass() {
    return ByteBuffer.class;
  }

  @NonNull
  @Override
  public DataSource getDataSource() {
    return DataSource.LOCAL;
  }
}
```

## Finishing off the ModelLoader. 

Back when we were working on our [``ModelLoader``][1], we left the [``buildLoadData``][13] method a little incomplete and returned ``null`` instead of a valid ``DataFetcher``:

```java
  @Nullable
  @Override
  public LoadData<ByteBuffer> buildLoadData(String model, int width, int height, Options options) {
    return new LoadData<>(new ObjectKey(model), /*fetcher=*/ null);
  }
```

Now that we have a ``DataFetcher`` implementation, we can fill that part in:

```java
  @Override
  public LoadData<ByteBuffer> buildLoadData(String model, int width, int height, Options options) {
    return new LoadData<>(new ObjectKey(model), new Base64DataFetcher(model));
  }
```

We can drop ``@Nullable`` since we're never actually going to return ``null`` in our implementation. If we were delegating to a wrapped ``ModelLoader``, we'd want to check that ``ModelLoader``s return value and be sure to return ``null`` if it returned ``null``. In some cases we may actually discover while attempting to parse our data that we can't actually load it, in which case we can also return ``null``.


## Registering our ModelLoader with Glide.

We're almost done, but there's one last step. Our ``ModelLoader`` implementation is complete, but totally unused. To finish off our project, we need to tell Glide about our ``ModelLoader`` so that Glide knows to use it. 


### Adding the AppGlideModule
To do so, we're going to follow the steps on [the configuration page][21] for our application and add an [``AppGlideModule``][22] if you haven't already done so:

```java
package judds.github.com.base64modelloaderexample;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

@GlideModule
public class MyAppGlideModule extends AppGlideModule { }
```

Don't forget to add a dependency on Glide's annotation processor to your build.gradle file as well:

```groovy
annotationProcessor 'com.github.bumptech.glide:compiler:4.7.1'
```

Next we want to get at Glide's [``Registry``][23], so we'll implement the [``registerComponents``][24] method in our ``AppGlideModule``:

```java
package judds.github.com.base64modelloaderexample;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

@GlideModule
public class MyAppGlideModule extends AppGlideModule { 
  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    // TODO: implement this.
  }
}
```

### Picking our Registry method
To tell Glide about our ``ModelLoader``, we need to add it to the [``Registry``][23] using one of the available methods for ``ModelLoader``s. 

``ModelLoaders`` are stored in a list in the order they are registered. When you start a new load, Glide looks at all the registered ``ModelLoader``s for the model type you provide in the order they were registered and attempts them in order. 

As a result, if you're adding support for a new Model type you typically want to [``prepend()``][25] your ``ModelLoader`` so that Glide attempts it before the default ``ModelLoader``s.  In our case, we're doing exactly that, adding support for a new type of model, so we want [``prepend()``][25]. 

However, that there's one more wrinkle here. [``prepend()``][25] takes a [``ModelLoaderFactory``][26], not a [``ModelLoader``][1]. Doing so allows you to delegate to other ``ModelLoader``s, even when they're registered dynamically, but it also adds an interface you have to implement when defining new loaders.

### Implementing ModelLoaderFactory

Fortunately the [``ModelLoaderFactory``][26] interface is quite simple, so we can add it easily:

```java
package judds.github.com.base64modelloaderexample;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import java.nio.ByteBuffer;

public class Base64ModelLoaderFactory implements ModelLoaderFactory<String, ByteBuffer> {

  @Override
  public ModelLoader<String, ByteBuffer> build(MultiModelLoaderFactory unused) {
    return new Base64ModelLoader();
  }

  @Override
  public void teardown() { 
    // Do nothing.
  }
}
```

The types for [``ModelLoaderFactory``][26] match those used in our ``ModelLoader`` exactly.

### Registering our ModelLoader

Finally we just need to update our ``AppGlideModule`` to use our new Factory:

```java
@GlideModule
public class MyAppGlideModule extends AppGlideModule {
  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    registry.prepend(String.class, ByteBuffer.class, new Base64ModelLoaderFactory());
  }
}
```

And that's it! 

Now we can just take any data uri with a base64 encoded image and load it with Glide and it just works:

```java
String dataUri = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYA..."
Glide.with(fragment)
  .load(dataUri)
  .into(imageView);
```

## Complete sample.

A complete sample project using the code we've written is available here: [https://github.com/sjudd/Base64ModelLoaderExample](https://github.com/sjudd/Base64ModelLoaderExample/commit/ae004dc4b325ee39814f197cc196d7371fbccdf1).

The commits are in the same order we wrote the code in the sample:

1. [An empty project with a blank Activity](https://github.com/sjudd/Base64ModelLoaderExample/commit/d9ee7eb9285ed1a7279cc085b3abd0f1369f92dd)
2. [A ModelLoader that just implements handles](https://github.com/sjudd/Base64ModelLoaderExample/commit/83ae04155b79056487299f65f70e172c11ff53ae)
3. [A data fetcher implementation](https://github.com/sjudd/Base64ModelLoaderExample/commit/70a4facda7504c375ca9150ea2a6789077bbd7e1)
4. [A ModelLoader with a complete buildLoadData implementation](https://github.com/sjudd/Base64ModelLoaderExample/commit/8c641cb1be3afbf1ff0d8bcba7b37b1778f06dc4)
5. [An AppGlideModule and registered ModelLoader](https://github.com/sjudd/Base64ModelLoaderExample/commit/d4e1cd9dcc011bb6d2910301c5783290fbe3bb89)
6. [An example data Uri loaded into a View](https://github.com/sjudd/Base64ModelLoaderExample/commit/ae004dc4b325ee39814f197cc196d7371fbccdf1)

## Caveats
As it turns out, Glide already supports Data URIs, so no need to do anything if you want load base64 strings as data URIs. This code is for example purposes only.

If you did want to implement support for data Uris, you'd probably want to do some more error checking on ``handles()`` or in the ``DataFetcher`` to handle truncated strings where your indexes might exceed the bounds of the uri. 

## Advanced use cases
There are a couple of more advanced use cases that don't fit into our tutorial above. We'll address them here individually.

### Delegating to another ModelLoader
One thing we mentioned earlier, but didn't discuss in detail is that Glide allows you to delegate to an existing ModelLoader in a custom ModelLoader. It's not uncommon to have a custom model type that Glide doesn't understand, but be able to relatively easily extract a model type that Glide does understand, like a URL, Uri, or file path, from the custom type.

With delegation, you can add support for your custom model by extracting the model that Glide does understand and delegating.

For example, in Glide's [Giphy sample app][28], we obtain a JSON object from Giphy's API that contains a set of URLs:

```java
/**
 * A POJO mirroring an individual GIF image returned from Giphy's API.
 */
public static final class GifResult {
  public String id;
  GifUrlSet images;

  @Override
  public String toString() {
    return "GifResult{" + "id='" + id + '\'' + ", images=" + images
        + '}';
  }
}
```

Although we could extract the urls in our View logic and do something like this:

```java
Glide.with(fragment)
  .load(gifResult.images.fixed_width)
  .into(imageView);
```

It's cleaner if we can just pass in the ``GifResult`` directly:

```java
Glide.with(fragment)
  .load(gifResult)
  .into(imageView);
```

If we had to re-write all of our URL handling logic to do that, it wouldn't be worth the effort. If we can delegate though, we end up with a fairly simple ``ModelLoader`` implementation:

```java
public final class GiphyModelLoader extends BaseGlideUrlLoader<Api.GifResult> {
  private final ModelLoader<GlideUrl, InputStream> urlLoader;

  private GiphyModelLoader(ModelLoader<GlideUrl, InputStream> urlLoader) {
    this.urlLoader = urlLoader;
  }

  @Override
  public boolean handles(@NonNull Api.GifResult model) {
    return true;
  }

  @Override
  public LoadData<InputStream> buildLoadData(
      @NonNull Api.GifResult model, int width, int height, @NonNull Options options) {
    return urlLoader.buildLoadData(model.images.fixed_width, width, height, options);
  }
}
```

The ``ModelLoader<GlideUrl, InputStream>`` required by our ``ModelLoader``'s constructor is provided by our ``ModelLoaderFactory`` which can look up the currently registered ``ModelLoader``s for a given model and data type (``GlideUrl`` and ``InputStream`` in this case):

```java
/**
 * The default factory for {@link com.bumptech.glide.samples.giphy.GiphyModelLoader}s.
 */
public static final class Factory implements ModelLoaderFactory<GifResult, InputStream> {
  @Override
  public ModelLoader<Api.GifResult, InputStream> build(MultiModelLoaderFactory multiFactory) {
    return new GiphyModelLoader(multiFactory.build(GlideUrl.class, InputStream.class));
  }

  @Override public void teardown() {}
}
```

Glide's ``ModelLoader``s are built lazily and torn down if new ``ModelLoader``s are registered so that you never end up using a stale ``ModelLoader`` when you use this delegation pattern. As a result, our ``GiphyModelLoader`` is totally decoupled from the networking library we actually use to load the url.

The [``MultiModelLoaderFactory``][29] can be used to obtain any registered ``ModelLoader``. If multiple ``ModelLoader``s are registered for a given type, the ``MultiModelLoaderFactory`` will return a wrapping ``ModelLoader`` that will attempt each ``ModelLoader`` that returns ``true`` from [``handles()``][11] for a given model in order until one succeeds. 

### Handling custom sizes in ModelLoaders

Even with delegation, the Giphy example above might seem like a fair amount of work for a slightly nicer API. However, there are additional benefits to having your own ``ModelLoader``, especially for APIs like Giphy's where you have multiple URLs you can choose from. 

Although we implemented [``buildLoadData()``][13] previously for our base 64 ``ModelLoader``, we never discussed the arguments provided other than the model. ``buildLoadData()`` also passes in a width and a height that can be used to select the most appropriately sized image, which can save bandwidth, memory, CPU, disk space etc by only retrieving, caching, and decoding the smallest image necessary.

The width and height passed in to ``buildLoadData()`` are either those provided by the [``Target``][30] or, if specified, the [``override()``][31] dimensions for the request. If you're loading into an ``ImageView`` the width and height provided to ``buildLoadData()`` are the width and height of the ``ImageView`` (again unless ``override()`` is used). If you use ``Target.SIZE_ORIGINAL``, the width and height will be the constant ``Target.SIZE_ORIGINAL``.

The actual [``GiphyModelLoader``][32] has a simple example of using the dimensions provided to ``buildLoadData()`` to pick the best available url:

```java
@Override
protected String getUrl(Api.GifResult model, int width, int height, Options options) {
  Api.GifImage fixedHeight = model.images.fixed_height;
  int fixedHeightDifference = getDifference(fixedHeight, width, height);
  Api.GifImage fixedWidth = model.images.fixed_width;
  int fixedWidthDifference = getDifference(fixedWidth, width, height);
  if (fixedHeightDifference < fixedWidthDifference && !TextUtils.isEmpty(fixedHeight.url)) {
    return fixedHeight.url;
  } else if (!TextUtils.isEmpty(fixedWidth.url)) {
    return fixedWidth.url;
  } else if (!TextUtils.isEmpty(model.images.original.url)) {
    return model.images.original.url;
  } else {
    return null;
  }
}
```

In Glide's [Flickr sample app][33], we see a similar pattern, although somewhat more robust because of the large variety of available thumbnail sizes. 

If you have access to an API that either allows you to specify a specific size to request or that offers a variety of thumbnail sizes, using a custom ``ModelLoader`` can significantly improve the performance of your application.


### BaseGlideUrlLoader

To save some of the boiler plate required to write a custom ``ModelLoader`` that just delegates to the default networking library, Glide includes the [``BaseGlideUrlLoader``][34] abstract class. A couple of our previous examples, including the [``GiphyModelLoader``][32] and the [``FlickrModelLoader``][35] make use of this class.

``BaseGlideUrlLoader`` provides some basic caching to minimize ``String`` allocations and two convenience methods:

1. [``getUrl()``][37] which returns a ``String`` url for a given model
2. [``getHeaders()``][38] which can be optionally implemented to returns a set of HTTP [``Headers``][36] for a given model and dimensions if you need to add an authentication or other type of header.


#### getUrl

If you read the earlier section about handling custom sizes, you might have noticed that the method in [``GiphyModelLoader``][32] we referenced isn't actually [``buildLoadData()``][13]. It's actually just the ``getUrl()`` convenience method:

```java
@Override
protected String getUrl(Api.GifResult model, int width, int height, Options options) {
  Api.GifImage fixedHeight = model.images.fixed_height;
  int fixedHeightDifference = getDifference(fixedHeight, width, height);
  Api.GifImage fixedWidth = model.images.fixed_width;
  int fixedWidthDifference = getDifference(fixedWidth, width, height);
  if (fixedHeightDifference < fixedWidthDifference && !TextUtils.isEmpty(fixedHeight.url)) {
    return fixedHeight.url;
  } else if (!TextUtils.isEmpty(fixedWidth.url)) {
    return fixedWidth.url;
  } else if (!TextUtils.isEmpty(model.images.original.url)) {
    return model.images.original.url;
  } else {
    return null;
  }
}
```

Using ``BaseGlideUrlLoader`` allows you to skip constructing the disk cache key and ``LoadData`` and allows you to avoid dealing with delegation, aside from the ``ModelLoader<GlideUrl, InputStream>`` you have to pass in to the constructor.

#### getHeaders

Although Glide's sample apps don't need to use ``getHeaders()``, it's not uncommon to have to attach some form of authentication when retrieving non-public images. The ``getHeaders()`` method can be optionally implemented to return any set of HTTP headers that is appropriate for a given model. 

For example, if you had a string authorization token, you might use the [``LazyHeaders``][39] class to write something like this:

```java
@Nullable
@Override
protected Headers getHeaders(GifResult gifResult, int width, int height, Options options) {
  return new LazyHeaders.Builder()
      .addHeader("Authorization", getAuthToken())
      .build();
}
```

If your ``getAuthToken()`` method is especially expensive, you should use the [``LazyHeaderFactory``][40] instead:


```java
@Override
protected Headers getHeaders(GifResult gifResult, int width, int height, Options options) {
  return new LazyHeaders.Builder()
      .addHeader("Authorization", new LazyHeaderFactory() {
        @Nullable
        @Override
        public String buildHeader() {
          return getAuthToken();
        }
      })
      .build();
}
```

Using ``LazyHeaderFactory`` will avoid running expensive calls until the HTTP request is made in the ``DataFetcher``. Although the ``ModelLoader`` methods are called on background threads, ``buildLoadData()`` is called, even if the corresponding image is in Glide's disk cache. As a result, it's wasteful to perform expensive work during the ``buildLoadData()`` method or any of the ``BaseGlideUrlLoader`` methods because the result may noto be used. Using ``LazyHeaderFactory`` will defer the work, saving a significant amount of time for expensive to acquire headers.

## Credits

Thanks to jasonch@ for Glide's [data uri ModelLoader implementation][27].

[1]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/load/model/ModelLoader.html
[2]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/load/data/DataFetcher.html
[3]: {{ site.baseurl }}/int/about.html
[4]: {{ site.baseurl }}/doc/configuration.html#applications
[5]: {{ site.baseurl }}/doc/configuration.html#libraries
[6]: https://github.com/bumptech/glide/issues/2677
[7]: https://developer.android.com/reference/java/io/InputStream.html
[8]: https://developer.android.com/reference/java/nio/ByteBuffer.html
[9]: https://developer.android.com/reference/android/os/ParcelFileDescriptor.html
[10]: https://developer.android.com/reference/android/util/Base64.html
[11]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/load/model/ModelLoader.html#handles-Model-
[12]: https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/Data_URIs
[13]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/load/model/ModelLoader.html#buildLoadData-Model-int-int-com.bumptech.glide.load.Options-
[14]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/load/model/ModelLoader.LoadData.html
[15]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/load/Key.html
[16]: https://developer.android.com/reference/java/lang/Object.html#toString()
[17]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/signature/ObjectKey.html
[18]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/load/Key.html#updateDiskCacheKey-java.security.MessageDigest-
[19]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/Priority.html
[20]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/load/data/DataFetcher.DataCallback.html
[21]: {{ site.baseurl }}/doc/configuration.html#applications
[22]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/module/AppGlideModule.html
[23]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/Registry.html
[24]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/module/LibraryGlideModule.html#registerComponents-android.content.Context-com.bumptech.glide.Glide-com.bumptech.glide.Registry-
[25]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/Registry.html#prepend-java.lang.Class-java.lang.Class-com.bumptech.glide.load.model.ModelLoaderFactory-
[26]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/load/model/ModelLoaderFactory.html
[27]: https://github.com/bumptech/glide/blob/c3dafde00a061bafcd43a739336ca3503af13a7d/library/src/main/java/com/bumptech/glide/load/model/DataUrlLoader.java#L19
[28]: {{ site.baseurl }}/ref/samples.html#giphy
[29]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/load/model/MultiModelLoaderFactory.html
[30]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/request/target/Target.html
[31]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/request/RequestOptions.html#override-int-int-
[32]: https://github.com/bumptech/glide/blob/b4b45791cca6b72345a540dcaa71a358f5706276/samples/giphy/src/main/java/com/bumptech/glide/samples/giphy/GiphyModelLoader.java#L31
[33]: {{ site.baseurl }}/ref/samples.html#flickr
[34]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/load/model/stream/BaseGlideUrlLoader.html
[35]: https://github.com/bumptech/glide/blob/b4b45791cca6b72345a540dcaa71a358f5706276/samples/flickr/src/main/java/com/bumptech/glide/samples/flickr/FlickrModelLoader.java#L21
[36]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/load/model/Headers.html
[37]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/load/model/stream/BaseGlideUrlLoader.html#getUrl-Model-int-int-com.bumptech.glide.load.Options-
[38]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/load/model/stream/BaseGlideUrlLoader.html#getHeaders-Model-int-int-com.bumptech.glide.load.Options-
[39]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/load/model/LazyHeaders.html
[40]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/load/model/LazyHeaderFactory.html
