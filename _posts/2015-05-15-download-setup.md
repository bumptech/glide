---
layout: page
title: "Download & Setup"
category: doc
date: 2015-05-15 21:08:05
order: 1
disqus: 1
---
* TOC
{:toc}

### Android SDK Requirements
**Minimum SDK Version** - Glide requires a minimum SDK version of **14** (Ice Cream Sandwich) or higher.

**Compile SDK Version** - Glide must be compiled against SDK version **26** (Oreo) or higher.

### Download

Glide's public releases are accessible in a number of ways.

#### Jar

You can download [the latest jar][1] from GitHub directly. Note that you will also need to include a jar for Android's [v4 support library][2].

#### Gradle

If you use Gradle you can add a dependency on Glide using either Maven Central or JCenter. You will also need to include a dependency on the support library.

```groovy
repositories {
  mavenCentral()
  maven { url 'https://maven.google.com' }
}

dependencies {
    compile 'com.github.bumptech.glide:glide:4.2.0'
    annotationProcessor 'com.github.bumptech.glide:compiler:4.2.0'
}
```

#### Maven

If you use Maven you can add a dependency on Glide as well. Again, you will also need to include a dependency on the support library.

```xml
<dependency>
  <groupId>com.github.bumptech.glide</groupId>
  <artifactId>glide</artifactId>
  <version>4.2.0</version>
  <type>aar</type>
</dependency>
<dependency>
  <groupId>com.google.android</groupId>
  <artifactId>support-v4</artifactId>
  <version>r7</version>
</dependency>
<dependency>
  <groupId>com.github.bumptech.glide</groupId>
  <artifactId>compiler</artifactId>
  <version>4.2.0</version>
  <optional>true</optional>
</dependency>
```

### Setup

Depending on your build configuration you may also need to do some additional setup.

#### Proguard

If you use proguard, you may need to add the following lines to your ``proguard.cfg``:

```
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
```

#### Jack
Glide's build configuration requires features that [Jack][3] does not currently support. Jack was recently [deprecated][4] and it's unlikely that the features Glide requires will ever be added.

#### Java 8
There is not yet (as of 6/2017) a stable release of the Android tool chain that will allow you to use Glide with Java 8 features. If you'd like to use Java 8 and are ok with less stability, there is at least an alpha version of the android gradle plugin that supports Java 8. The alpha version of the plugin has not yet been tested with Glide. See Android's [Java 8 support page][5] for more details.

#### Kotlin

If you use Glide's annotations on classes implemented in Kotlin, you need to include a ``kapt`` dependency on Glide's annotation processor instead of a ``annotationProcessor`` dependency:

```groovy
dependencies {
  kapt 'com.github.bumptech.glide:compiler:4.2.0'
}
```
Note that you must also include the ``kotlin-kapt`` plugin in your ``build.gradle`` file:

```groovy
apply plugin: 'kotlin-kapt'
```

See the [generated API][6] page for details.

[1]: https://github.com/bumptech/glide/releases/download/v3.6.0/glide-3.6.0.jar
[2]: http://developer.android.com/tools/support-library/features.html#v4
[3]: https://source.android.com/source/jack
[4]: https://android-developers.googleblog.com/2017/03/future-of-java-8-language-feature.html
[5]: https://developer.android.com/studio/write/java8-support.html
[6]: {{ site.baseurl }}/doc/generatedapi.html#kotlin
