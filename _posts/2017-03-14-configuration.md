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

#### Libraries
Libraries must:
1. Add one or more [``LibraryGlideModule``][2] implementations.
2. Add the [``@GlideModule``][5] annotation to every [``LibraryGlideModule``][2] implementation
3. Add a dependency on Glide's annotation processor.

An example [``LibraryGlideModule``][2] from Glide's [OkHttp integration library][7] looks like this:
```java
@GlideModule
public final class OkHttpLibraryGlideModule extends LibraryGlideModule {
  @Override
  public void registerComponents(Context context, Registry registry) {
    registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory());
  }
}
```

Using the [``@GlideModule``][5] annotation requires a dependency on Glide's annotations:
```groovy
annotationProcessor 'com.github.bumptech.glide:annotation:1.0.0-SNAPSHOT'
```

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
  public void registerComponents(Context context, Registry registry) {
    registry.append(Photo.class, InputStream.class, new FlickrModelLoader.Factory());
  }
}
```

Including Glide's annotation processor requires dependencies on Glide's annotations and the annotation processor:
```groovy
annotationProcessor 'com.github.bumptech.glide:annotation:1.0.0-SNAPSHOT'
annotationProcessor 'com.github.bumptech.glide:compiler:1.0.0-SNAPSHOT'
```

Finally, you should keep AppGlideModule implementations in your ``proguard.cfg``:
```
-keep public class * extends com.bumptech.glide.AppGlideModule
```

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
    builder.setDiskCache(new InternalDiskCacheFactory(context), diskCacheSizeBytes);
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
    builder.setDiskCache(new InternalDiskCacheFactory(context), "cacheFolderName", 
        diskCacheSizeBytes);
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


### Key components
Glide v4 relies on two classes, [``AppGlideModule``][1] and [``LibraryGlideModule``][2], to configure the Glide singleton. Both classes are allowed to register additional components, like [``ModelLoaders``][3], [``ResourceDecoders``][4] etc. Only the [``AppGlideModules``][1] are allowed to configure application specific settings, like cache implementations and sizes. 

#### AppGlideModule
All applications must add a [``AppGlideModule``][1] implementation, even if the Application is not changing any additional settings or implementing any methods in [``AppGlideModule``][1]. The [``AppGlideModule``][1] implementation acts as a signal that allows Glide's annotation processor to generate a single combined class with with all discovered [``LibraryGlideModules``][2].

There can be only one [``AppGlideModule``][1] implementation in a given application (having more than one produce errors at compile time). As a result, libraries must never provide a [``AppGlideModule``][1] implementation. 

#### @GlideModule
In order for Glide to properly discover [``AppGlideModule``][1] and [``LibraryGlideModule``][2] implementations, all implementations of both classes must be annotated with the [``@GlideModule``][5] annotation. The annotation will allow Glide's [annotation processor][6] to discover all implementations at compile time. 

#### Annotation Processor
In addition, to enable discovery of the [``AppGlideModule``][1] and [``LibraryGlideModules``][2] all libraries and applications must also include a dependency on Glide's annotation processor. 

[1]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/module/AppGlideModule.html
[2]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/module/LibraryGlideModule.html
[3]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/load/model/ModelLoader.html
[4]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/load/ResourceDecoder.html
[5]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/annotation/GlideModule.html
[6]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/annotation/compiler/ModuleAnnotationProcessor.html
[7]: https://github.com/bumptech/glide/blob/master/integration/okhttp3/src/main/java/com/bumptech/glide/integration/okhttp3/OkHttpLibraryGlideModule.java
[8]: https://github.com/bumptech/glide/blob/master/samples/flickr/src/main/java/com/bumptech/glide/samples/flickr/FlickrGlideModule.java
[9]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/load/engine/cache/MemoryCache.html
[10]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/load/engine/cache/LruResourceCache.html
[11]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/load/engine/cache/MemorySizeCalculator.html
[12]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/module/AppGlideModule.html#applyOptions-android.content.Context-com.bumptech.glide.GlideBuilder-
[13]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/load/engine/cache/DiskLruCacheWrapper.html
[14]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/load/engine/cache/DiskCache.html
[15]: https://github.com/bumptech/glide/blob/master/library/src/main/java/com/bumptech/glide/load/engine/cache/DiskCache.java#L18
[16]: https://github.com/bumptech/glide/blob/master/library/src/main/java/com/bumptech/glide/load/engine/cache/DiskCache.java#L19
[17]: https://developer.android.com/reference/android/content/Context.html#getCacheDir()
[18]: https://github.com/bumptech/glide/blob/master/library/src/main/java/com/bumptech/glide/load/engine/cache/DiskCache.java#L15
[19]: https://developer.android.com/reference/android/os/StrictMode.html

