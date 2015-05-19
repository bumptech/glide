---
layout: page
title: "Download & Setup"
category: doc
date: 2015-05-15 21:08:05
order: 1
---

### Download 

Glide's public releases are accessible in a number of ways.

#### Jar

You can download [the latest jar][1] from GitHub directly. Note that you will also need to include a jar for Android's [v4 support library][2].

#### Gradle

If you use Gradle you can add a dependency on Glide using either Maven Central or JCenter. You will also need to include a dependency on the support library.

```groovy
repositories {
  mavenCentral()
}

dependencies {
    compile 'com.github.bumptech.glide:glide:4.0.0'
    compile 'com.android.support:support-v4:19.1.0'
}
```

#### Maven

If you use Maven you can add a dependency on Glide as well. Again, you will also need to include a dependency on the support library.

```xml
<dependency>
  <groupId>com.github.bumptech.glide</groupId>
  <artifactId>glide</artifactId>
  <version>4.0.0</version>
  <type>aar</type>
</dependency>
<dependency>
  <groupId>com.google.android</groupId>
  <artifactId>support-v4</artifactId>
  <version>r7</version>
</dependency>
```

### Setup

Depending on your build configuration you may also need to do some additional setup.

#### Proguard

If you use proguard, you may need to add the following lines to your ``proguard.cfg``:

```
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
```

[1]: https://github.com/bumptech/glide/releases/download/v3.6.0/glide-3.6.0.jar
[2]: http://developer.android.com/tools/support-library/features.html#v4


