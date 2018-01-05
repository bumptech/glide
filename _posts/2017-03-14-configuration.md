---
layout: page
title: "Configuration"
category: doc
date: 2017-03-14 13:37:04
order: 9
disqus: 1
---
* TOC
{:toc}

### Setup
For Glide's configuration to work properly, libraries and applications need to perform a certain set of steps. Note that libraries that do not wish to register additional components are not required to do this.

#### Applications
Applications must:
1. Add exactly one [``AppGlideModule``][1] implementation
2. Optionally add one or more [``LibraryGlideModule``][2] implementations.
3. Add the [``@GlideModule``][5] annotation to the [``AppGlideModule``][1] implementation and all [``LibraryGlideModule``][2] implementations.
4. Add a dependency on Glide's annotation processor.
5. Add a proguard keep for [``AppGlideModules``][1].

An example [``AppGlideModule``][1] from Glide's [Flickr sample app][8] looks like this:
```java
@GlideModule
public class FlickrGlideModule extends AppGlideModule {
  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    registry.append(Photo.class, InputStream.class, new FlickrModelLoader.Factory());
  }
}
```

Including Glide's annotation processor requires dependencies on Glide's annotations and the annotation processor:
```groovy
compile 'com.github.bumptech.glide:annotations:4.5.0'
annotationProcessor 'com.github.bumptech.glide:compiler:4.5.0'
```

Finally, you should keep AppGlideModule implementations in your ``proguard.cfg``:
```
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl
```

#### Libraries
Libraries that do not register custom components do not need to perform any configuration steps and can skip the sections on this page entirely.

Libraries that do need to register a custom component, like a ``ModelLoader``, can do the following: 

1. Add one or more [``LibraryGlideModule``][2] implementations that register the new components.
2. Add the [``@GlideModule``][5] annotation to every [``LibraryGlideModule``][2] implementation
3. Add a dependency on Glide's annotation processor.

An example [``LibraryGlideModule``][2] from Glide's [OkHttp integration library][7] looks like this:
```java
@GlideModule
public final class OkHttpLibraryGlideModule extends LibraryGlideModule {
  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory());
  }
}
```

Using the [``@GlideModule``][5] annotation requires a dependency on Glide's annotations:
```groovy
compile 'com.github.bumptech.glide:annotations:4.5.0'
```

##### Avoid AppGlideModule in libraries
Libraries must **not** include ``AppGlideModule`` implementations. Doing so will prevent any applications that depend on the library from managing their dependencies or configuring options like Glide's cache sizes and locations. 

In addition, if two libraries include ``AppGlideModule``s, applications will be unable to compile if they depend on both and will be forced to pick one or other other. 

This does mean that libraries won't be able to use Glide's generated API, but loads with ``RequestOptions`` will still work just fine (see the [options page][42] for examples).

### Application Options
Glide allows applications to use [``AppGlideModule``][1] implementations to completely control Glide's memory and disk cache usage. Glide tries to provide reasonable defaults for most applications, but for some applications, it will be necessary to customize these values. Be sure to measure the results of any changes to avoid performance regressions.

#### Memory cache
By default, Glide uses [``LruResourceCache``][10], a default implementation of the [``MemoryCache``][9] interface that uses a fixed amount of memory with LRU eviction. The size of the [``LruResourceCache``][10] is determined by Glide's [``MemorySizeCalculator``][11] class, which looks at the device memory class, whether or not the device is low ram and the screen resolution. 

Applications can customize the [``MemoryCache``][9] size in their [``AppGlideModule``][1] with the [``applyOptions(Context, GlideBuilder)``][12] method by configuring [``MemorySizeCalculator``][11]:

```java
@GlideModule
public class YourAppGlideModule extends AppGlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    MemorySizeCalculator calculator = new MemorySizeCalculator.Builder(context)
        .setMemoryCacheScreens(2)
        .build();
    builder.setMemoryCache(new LruResourceCache(calculator.getMemoryCacheSize()));
  }
}
```

Applications can also directly override the cache size: 

```java
@GlideModule
public class YourAppGlideModule extends AppGlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    int memoryCacheSizeBytes = 1024 * 1024 * 20; // 20mb
    builder.setMemoryCache(new LruResourceCache(memoryCacheSizeBytes));
  }
}
```

Applications can even provide their own implementation of [``MemoryCache``][9]:
```java
@GlideModule
public class YourAppGlideModule extends AppGlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    builder.setMemoryCache(new YourAppMemoryCacheImpl());
  }
}
```

#### Bitmap pool
Glide uses [``LruBitmapPool``][39] as the default [``BitmapPool``][40]. [``LruBitmapPool``][39] is a fixed size in memory ``BitmapPool`` that uses LRU eviction. The default size is based on the screen size and density of the device in question as well as the memory class and the return value of [``isLowRamDevice``][41]. The specific calculations are done by Glide's [``MemorySizeCalculator``][11], similar to the way sizes are determined for Glide's [``MemoryCache``][9].

Applications can customize the [``BitmapPool``][40] size in their [``AppGlideModule``][1] with the [``applyOptions(Context, GlideBuilder)``][12] method by configuring [``MemorySizeCalculator``][11]:

```java
@GlideModule
public class YourAppGlideModule extends AppGlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    MemorySizeCalculator calculator = new MemorySizeCalculator.Builder(context)
        .setBitmapPoolScreens(3)
        .build();
    builder.setBitmapPool(new LruBitmapPool(calculator.getBitmapPoolSize()));
  }
}
```

Applications can also directly override the pool size: 

```java
@GlideModule
public class YourAppGlideModule extends AppGlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    int bitmapPoolSizeBytes = 1024 * 1024 * 30; // 30mb
    builder.setBitmapPool(new LruBitmapPool(bitmapPoolSizeBytes));
  }
}
```

Applications can even provide their own implementation of [``BitmapPool``][40]:
```java
@GlideModule
public class YourAppGlideModule extends AppGlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    builder.setBitmapPool(new YourAppBitmapPoolImpl());
  }
}
```

#### Disk Cache
Glide uses [``DiskLruCacheWrapper``][13] as the default [``DiskCache``][14]. [``DiskLruCacheWrapper``][13] is a fixed size disk cache with LRU eviction. The default disk cache size is [250 MB][15] and is placed in a [specific directory][16] in the Application's [cache folder][17].

Applications can change the location to external storage if the media they display is public (obtained from websites without authentication, search engines etc):
```java
@GlideModule
public class YourAppGlideModule extends AppGlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    builder.setDiskCache(new ExternalDiskCacheFactory(context));
  }
}
```

Applications can change the size of the disk, for either the internal or external disk caches:
```java
@GlideModule
public class YourAppGlideModule extends AppGlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    int diskCacheSizeBytes = 1024 * 1024 * 100; // 100 MB
    builder.setDiskCache(new InternalDiskCacheFactory(context, diskCacheSizeBytes));
  }
}
```

Applications can change the name of the folder the cache is placed in within external or internal storage:
```java
@GlideModule
public class YourAppGlideModule extends AppGlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    int diskCacheSizeBytes = 1024 * 1024 * 100; // 100 MB
    builder.setDiskCache(
        new InternalDiskCacheFactory(context, "cacheFolderName", diskCacheSizeBytes));
  }
}
```

Applications can also choose to implement the [``DiskCache``][14] interface themselves and provide their own [``DiskCache.Factory``][18] to produce it. Glide uses a Factory interface to open [``DiskCaches``][14] on background threads so that the caches can do I/O like checking the existence of their target directory without causing a [StrictMode][19] violation.

```java
@GlideModule
public class YourAppGlideModule extends AppGlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    builder.setDiskCache(new DiskCache.Factory() {
        @Override
        public DiskCache build() {
          return new YourAppCustomDiskCache();
        }
    });
  }
}
```

#### Default Request Options
Although [``RequestOptions``][33] are typically specified per request, you can also apply a default set of [``RequestOptions``][33] that will be applied to every load you start in your application by using an [``AppGlideModule``][1]:

```java
@GlideModule
public class YourAppGlideModule extends AppGlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    builder.setDefaultRequestOptions(
        new RequestOptions()
          .format(DecodeFormat.RGB_565)
          .disallowHardwareBitmaps());
  }
}
```

Options applied with ``setDefaultRequestOptions`` in ``GlideBuilder`` are applied as soon as you create a new request. As a result, options applied to any individual request will override any conflicting options that are set in the ``GlideBuilder``.

[``RequestManagers``][34] similarly allow you to set default [``RequestOptions``][33] for all loads started with that particular [``RequestManager``][34]. Since each ``Activity`` and ``Fragment`` gets its own [``RequestManager``][34], you can use [``RequestManager's``][34] [``applyDefaultRequestOptions``][35] method to set default [``RequestOptions``][33] that apply only to a particular ``Activity`` or ``Fragment``:

```java
Glide.with(fragment)
  .applyDefaultRequestOptions(
      new RequestOptions()
          .format(DecodeFormat.RGB_565)
          .disallowHardwareBitmaps());
```

[``RequestManager``][34] also has a [``setDefaultRequestOptions``][36] that will completely replace any default [``RequestOptions``][33] previously set either via the ``GlideBuilder`` in an [``AppGlideModule``][1] or via the [``RequestManager``][34]. Use caution with [``setDefaultRequestOptions``][36] because it's easy to accidentally override important defaults you've set elsewhere. Typically [``applyDefaultRequestOptions``][35] is safer and more intuitive to use.

#### UncaughtThrowableStrategy

When loading a bitmap, if an exception happens (e.g. `OutOfMemoryException`), Glide will use a `GlideExecutor.UncaughtThrowableStrategy`.
The default strategy is to log the exception in the device logcat. The strategy is customizable since Glide 4.2.0. It can be passed to a disk executor and/or a resize executor:

```java
@GlideModule
public class YourAppGlideModule extends AppGlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    final UncaughtThrowableStrategy myUncaughtThrowableStrategy = new ...
    builder.setDiskCacheExecutor(newDiskCacheExecutor(myUncaughtThrowableStrategy));
    builder.setResizeExecutor(newSourceExecutor(myUncaughtThrowableStrategy));
  }
}
```

#### Log level

For a subset of well formatted logs, including lines logged when a request fails, you can use [``setLogLevel``][37] with one of the values from Android's [``Log``][38] class. Generally speaking ``log.VERBOSE`` will make logs noisier and ``Log.ERROR`` will make logs quieter, but see [the javadoc][37] for details.

```java
@GlideModule
public class YourAppGlideModule extends AppGlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    builder.setLogLevel(Log.DEBUG);
  }
}
```

### Registering Components

Both Applications and Libraries can register a number of components that extend Glides functionality. Available components include:

1. [``ModelLoader``][23]s to load custom Models (Urls, Uris, arbitrary POJOs) and Data (InputStreams, FileDescriptors).
2. [``ResourceDecoder``][24]s to decode new Resources (Drawables, Bitmaps) or new types of Data (InputStreams, FileDescriptors).
3. [``Encoder``][25]s to write Data (InputStreams, FileDescriptors) to Glide's disk cache.
4. [``ResourceTranscoder``][26]s to convert Resources (BitmapResource) into other types of Resources (DrawableResource).
5. [``ResourceEncoder``][27]s to write Resources (BitmapResource, DrawableResource) to Glide's disk cache.

Components are registered using the [``Registry``][28] class in the [``registerComponents()``][31] method of ``AppGlideModules`` and ``LibraryGlideModules``:

```java
@GlideModule
public class YourAppGlideModule extends AppGlideModule {
  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    registry.append(...);
  }
}
```

or:

```java
@GlideModule
public class YourLibraryGlideModule extends LibraryGlideModule {
  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    registry.append(...);
  }
}
```

Any number of components can registered in a single ``GlideModule``. Certain types, including [``ModelLoader``][23]s and [``ResourceDecoder``][24]s can have multiple implementations with the same type arguments. 


#### Anatomy of a load
The set of registered components, including both those registered by default in Glide and those registered in Modules are used to define a set of load paths. Each load path is a step by step progression from the the Model provided to [``load()``][29] to the Resource type specified by [``as()``][30]. A load path consists (roughly) of the following steps:

1. Model -> Data (handled by ``ModelLoader``s)
2. Data -> Resource (handled by ``ResourceDecoder``s)
3. Resource -> Transcoded Resource (optional, handled by ``ResourceTranscoder``s).

``Encoder``s can write Data to Glide's disk cache cache before step 2.
``ResourceEncoder``s can write Resource's to Glide's disk cache before step 3. 

When a request is started, Glide will attempt all available paths from the Model to the requested Resource type. A request will succeed if any load path succeeds. A request will fail only if all available load paths fail.  

#### Ordering Components

The ``prepend()``, ``append()``, and ``replace()`` methods in [``Registry``][28] can be used to set the order in which Glide will attempt each ``ModelLoader`` and ``ResourceDecoder``.  Ordering components allows you to register components that handle specific subsets of models or data (IE only certain types of Uris, or only certain image formats) while also having an appended catch-all component to handle the rest. 

##### prepend()
To handle subsets of existing data where you do want to fall back to Glide's default behavior if your ``ModelLoader`` or ``ResourceDecoder`` fails, using ``prepend()``. ``prepend()`` will make sure that your ``ModelLoader`` or ``ResourceDecoder`` is called before all other previously registered components and can run first. If your ``ModelLoader`` or ``ResourceDecoder`` returns ``false`` from its ``handles()`` method or fails, all other ``ModelLoader``s or ``ResourceDecoders`` will be called in the order they're registered, one at a time, providing a fallback. 

##### append()
To handle new types of data or to add a fallback to Glide's default behavior, using ``append()``. ``append()`` will make sure that your ``ModelLoader`` or ``ResourceDecoder`` is called only after Glide's defaults are attempted. If you're trying to handle subtypes that Glide's default components handle (like a specific Uri authority or subclass), you may need to use ``prepend()`` to make sure Glide's default component doesn't load the resource before your custom component. 

##### replace()
To completely replace Glide's default behavior and ensure that it doesn't run, use ``replace()``. ``replace()`` removes all ``ModelLoaders`` that handle the given model and data classes and then adds your ``ModelLoader`` instead. ``replace()`` is useful in particular for swapping out Glide's networking logic with a library like OkHttp or Volley, where you want to make sure that only OkHttp or Volley are used.

#### Adding a ModelLoader
For example, to add a ``ModelLoader`` that can obtain an InputStream for a new custom Model object:

```java
@GlideModule
public class YourAppGlideModule extends AppGlideModule {
  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    registry.append(Photo.class, InputStream.class, new CustomModelLoader.Factory());
  }
}
```

``append()`` can be used safely here because Photo.class is a custom model object specific to your application, so you know that there is no default behavior in Glide that you need to replace.

In contrast, to add handling for a new type of String url in a [``BaseGlideUrlLoader``][32], you should use ``prepend()`` so that your ``ModelLoader`` gets to run before Glide's default ``ModelLoaders`` for ``Strings``:

```java
@GlideModule
public class YourAppGlideModule extends AppGlideModule {
  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    registry.prepend(String.class, InputStream.class, new CustomUrlModelLoader.Factory());
  }
}
```

Finally to completely remove and replace Glide's default handling of a certain type, like a networking library, you should use ``replace()``:

```java
@GlideModule
public class YourAppGlideModule extends AppGlideModule {
  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory());
  }
}
```




### Module classes and annotations.
Glide v4 relies on two classes, [``AppGlideModule``][1] and [``LibraryGlideModule``][2], to configure the Glide singleton. Both classes are allowed to register additional components, like [``ModelLoaders``][3], [``ResourceDecoders``][4] etc. Only the [``AppGlideModules``][1] are allowed to configure application specific settings, like cache implementations and sizes. 

#### AppGlideModule
All applications must add a [``AppGlideModule``][1] implementation, even if the Application is not changing any additional settings or implementing any methods in [``AppGlideModule``][1]. The [``AppGlideModule``][1] implementation acts as a signal that allows Glide's annotation processor to generate a single combined class with with all discovered [``LibraryGlideModules``][2].

There can be only one [``AppGlideModule``][1] implementation in a given application (having more than one produce errors at compile time). As a result, libraries must never provide a [``AppGlideModule``][1] implementation. 

#### @GlideModule
In order for Glide to properly discover [``AppGlideModule``][1] and [``LibraryGlideModule``][2] implementations, all implementations of both classes must be annotated with the [``@GlideModule``][5] annotation. The annotation will allow Glide's [annotation processor][6] to discover all implementations at compile time. 

#### Annotation Processor
In addition, to enable discovery of the [``AppGlideModule``][1] and [``LibraryGlideModules``][2] all libraries and applications must also include a dependency on Glide's annotation processor. 

### Conflicts
Applications may depend on multiple libraries, each of which may contain one or more [``LibraryGlideModules``][2]. In rare cases, these [``LibraryGlideModules``][2] may define conflicting options or otherwise include behavior the application would like to avoid. Applications can resolve these conflicts or avoid unwanted dependencies by adding an [``@Excludes``][20] annotation to their [``AppGlideModule``][1].

For example if you depend on a library that has a [``LibraryGlideModule``][2] that you'd like to avoid, say ``com.example.unwanted.GlideModule``:

```java
@Excludes(com.example.unwanted.GlideModule.class)
@GlideModule
public final class MyAppGlideModule extends AppGlideModule { }
```

You can also excludes multiple modules:

```java
@Excludes({com.example.unwanted.GlideModule.class, com.example.conflicing.GlideModule.class})
@GlideModule
public final class MyAppGlideModule extends AppGlideModule { }
```

[``@Excludes``][20] can be used to exclude both [``LibraryGlideModules``][2] and legacy, deprecated [``GlideModule``][21] implementations if you're still in the process of migrating from Glide v3.


### Manifest Parsing
To maintain backward compatibility with Glide v3's [``GlideModules``][21], Glide still parses ``AndroidManifest.xml`` files from both the application and any included libraries and will include any legacy [``GlideModules``][21] listed in the manifest. Although this functionality will be removed in a future version, we've retained the behavior for now to ease the transition.

If you've already migrated to the Glide v4 [``AppGlideModule``][1] and [``LibraryGlideModule``][2], you can disable manifest parsing entirely. Doing so can improve the initial startup time of Glide and avoid some potential problems with trying to parse metadata. To disable manifest parsing, override the [``isManifestParsingEnabled()``][22] method in your [``AppGlideModule``][1] implementation:

```java
@GlideModule
public final class MyAppGlideModule extends AppGlideModule {
  @Override
  public boolean isManifestParsingEnabled() {
    return false;
  }
}
```

[1]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/module/AppGlideModule.html
[2]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/module/LibraryGlideModule.html
[3]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/model/ModelLoader.html
[4]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/ResourceDecoder.html
[5]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/annotation/GlideModule.html
[6]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/annotation/compiler/GlideAnnotationProcessor.html
[7]: https://github.com/bumptech/glide/blob/master/integration/okhttp3/src/main/java/com/bumptech/glide/integration/okhttp3/OkHttpLibraryGlideModule.java
[8]: https://github.com/bumptech/glide/blob/master/samples/flickr/src/main/java/com/bumptech/glide/samples/flickr/FlickrGlideModule.java
[9]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/engine/cache/MemoryCache.html
[10]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/engine/cache/LruResourceCache.html
[11]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/engine/cache/MemorySizeCalculator.html
[12]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/module/AppGlideModule.html#applyOptions-android.content.Context-com.bumptech.glide.GlideBuilder-
[13]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/engine/cache/DiskLruCacheWrapper.html
[14]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/engine/cache/DiskCache.html
[15]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/engine/cache/DiskCache.Factory.html#DEFAULT_DISK_CACHE_SIZE
[16]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/engine/cache/DiskCache.Factory.html#DEFAULT_DISK_CACHE_DIR
[17]: https://developer.android.com/reference/android/content/Context.html#getCacheDir()
[18]: {{ site.url}}/glide/javadocs/400/com/bumptech/glide/load/engine/cache/DiskCache.Factory.html
[19]: https://developer.android.com/reference/android/os/StrictMode.html
[20]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/annotation/Excludes.html
[21]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/module/GlideModule.html
[22]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/module/AppGlideModule.html#isManifestParsingEnabled--
[23]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/model/ModelLoaderFactory.html
[24]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/ResourceDecoder.html
[25]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/Encoder.html
[26]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/resource/transcode/ResourceTranscoder.html
[27]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/ResourceEncoder.html
[28]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/Registry.html
[29]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/RequestBuilder.html#load-java.lang.Object-
[30]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/RequestManager.html#as-java.lang.Class-
[31]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/module/LibraryGlideModule.html#registerComponents-android.content.Context-com.bumptech.glide.Glide-com.bumptech.glide.Registry-
[32]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/model/stream/BaseGlideUrlLoader.html
[33]: {{ site.baseurl }}/javadocs/410/com/bumptech/glide/request/RequestOptions.html
[34]: {{ site.baseurl }}/javadocs/410/com/bumptech/glide/RequestManager.html
[35]: {{ site.baseurl }}/javadocs/410/com/bumptech/glide/RequestManager.html#applyDefaultRequestOptions-com.bumptech.glide.request.RequestOptions-
[36]: {{ site.baseurl }}/javadocs/410/com/bumptech/glide/RequestManager.html#setDefaultRequestOptions-com.bumptech.glide.request.RequestOptions-
[37]: {{ site.baseurl }}/javadocs/420/com/bumptech/glide/GlideBuilder.html#setLogLevel-int-
[38]: https://developer.android.com/reference/android/util/Log.html
[39]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/load/engine/bitmap_recycle/LruBitmapPool.html
[40]: {{ site.baseurl }}/javadocs/431/com/bumptech/glide/load/engine/bitmap_recycle/BitmapPool.html
[41]: https://developer.android.com/reference/android/app/ActivityManager.html#isLowRamDevice()
[42]: {{ site.baseurl }}/doc/options.html
