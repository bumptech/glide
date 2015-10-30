Glide
=====

[![Build Status](https://travis-ci.org/bumptech/glide.svg?branch=master)](https://travis-ci.org/bumptech/glide)

Glide is a fast and efficient open source media management and image loading framework for Android that wraps media
decoding, memory and disk caching, and resource pooling into a simple and easy to use interface.

![](static/glide_logo.png)

Glide supports fetching, decoding, and displaying video stills, images, and animated GIFs. Glide includes a flexible API
that allows developers to plug in to almost any network stack. By default Glide uses a custom `HttpUrlConnection` based
stack, but also includes utility libraries plug in to Google's Volley project or Square's OkHttp library instead.

Glide's primary focus is on making scrolling any kind of a list of images as smooth and fast as possible, but Glide is
also effective for almost any case where you need to fetch, resize, and display a remote image.

Download
--------
You can download a jar from GitHub's [releases page][1].

Or use Gradle:

```gradle
repositories {
    mavenCentral()
}

dependencies {
    compile 'com.github.bumptech.glide:glide:3.6.1'
    compile 'com.android.support:support-v4:19.1.0'
}
```

Or Maven:

```xml
<dependency>
    <groupId>com.github.bumptech.glide</groupId>
    <artifactId>glide</artifactId>
    <version>3.6.1</version>
</dependency>
<dependency>
    <groupId>com.google.android</groupId>
    <artifactId>support-v4</artifactId>
    <version>r7</version>
</dependency>
```

Proguard
--------
Depending on your proguard config and usage, you may need to include the following lines in your proguard.cfg:

```pro
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
```

How do I use Glide?
-------------------
Checkout the [GitHub wiki][2] for pages on a variety of topics, and see the [javadocs][3].

Simple use cases will look something like this:

```java
// For a simple view:
@Override
public void onCreate(Bundle savedInstanceState) {
    ...
    ImageView imageView = (ImageView) findViewById(R.id.my_image_view);

    Glide.with(this).load("http://goo.gl/gEgYUd").into(imageView);
}

// For a simple image list:
@Override
public View getView(int position, View recycled, ViewGroup container) {
    final ImageView myImageView;
    if (recycled == null) {
        myImageView = (ImageView) inflater.inflate(R.layout.my_image_view, container, false);
    } else {
        myImageView = (ImageView) recycled;
    }

    String url = myUrls.get(position);

    Glide.with(myFragment)
        .load(url)
        .centerCrop()
        .placeholder(R.drawable.loading_spinner)
        .crossFade()
        .into(myImageView);

    return myImageView;
}

```

OkHttp and Volley
-----------------
Support for OkHttp and Volley is provided by integration libraries you can optionally include as dependencies.
The integration libraries are available via Maven or the [releases page][1].

For instructions on including either the OkHttp or the Volley integration libraries, see the [Integration Libraries][12] wiki page.

Android SDK Version
-------------------
Glide requires a minimum SDK version of 10.

License
-------
BSD, part MIT and Apache 2.0. See the [LICENSE][16] file for details.

Status
------
[*Version 3*][14] is a stable public release used in multiple open source projects at Google including in the Android Camera
app and in the 2014 Google IO app. *Version 4* is currently under development on the `master` branch.
Comments/bugs/questions/pull requests welcome!

Build
-----
Building Glide with gradle is fairly straight forward:

```shell
git clone git@github.com:bumptech/glide.git # use https://github.com/bumptech/glide.git if "Permission Denied"
cd glide
git submodule init && git submodule update
./gradlew jar
```

**Note**: Make sure your *Android SDK* has the *Android Support Repository* installed, and that your `$ANDROID_HOME` environment
variable is pointing at the SDK or add a `local.properties` file in the root project with a `sdk.dir=...` line.

Samples
-------
Follow the steps in the [Build](#build) section to setup the project and then:

```shell
./gradlew :samples:flickr:run
./gradlew :samples:giphy:run
./gradlew :samples:svg:run
```
You may also find precompiled APKs on the [releases page][1].

Development
-----------
Follow the steps in the [Build](#build) section to setup the project and then edit the files however you wish.
[Intellij IDEA 14][4] cleanly imports both Glide's source and tests and is the recommended way to work with Glide.

To open the project in Intellij 14:

1. Go to *File* menu or the *Welcome Screen*
2. Click on *Open...*
3. Navigate to Glide's root directory.
4. Select `build.gradle`

Getting Help
------------
To report a specific problem or feature request, [open a new issue on Github][5]. For questions, suggestions, or
anything else, join or email [Glide's discussion group][6], or join our IRC channel: [irc.freenode.net#glide-library][13].

Contributing
------------
Before submitting pull requests, contributors must sign Google's [individual contributor license agreement][7].

Thanks
------
* The **Android team** and **Jake Wharton** for the [disk cache implementation][8] Glide's disk cache is based on.
* **Dave Smith** for the [gif decoder gist][9] Glide's gif decoder is based on.
* **Chris Banes** for his [gradle-mvn-push][10] script.
* **Corey Hall** for Glide's [amazing logo][11].
* Everyone who has contributed code and reported issues!

Author
------
Sam Judd - @samajudd

Disclaimer
---------
This is not an official Google product.

[1]: https://github.com/bumptech/glide/releases
[2]: https://github.com/bumptech/glide/wiki
[3]: http://bumptech.github.io/glide/javadocs/latest/index.html
[4]: https://www.jetbrains.com/idea/download/
[5]: https://github.com/bumptech/glide/issues/new?body=**Glide%20Version/Integration%20library%20%28if%20any%29**%3A%0A**Device/Android%20Version**%3A%0A**Issue%20details/Repro%20steps/Use%20case%20background**%3A%0A%0A**Glide%20load%20line**%3A%0A%60%60%60java%0AGlide.with%28...%29.....load%28...%29.....into%28...%29%3B%0A%60%60%60%0A%0A**Layout%20XML**%3A%0A%60%60%60xml%0A%3C...Layout%3E%0A%20%20%20%20%3CImageView%20android%3AscaleType%3D%22...%22%20...%20/%3E%0A%3C/..Layout%3E%0A%60%60%60%0A%0A**Stack%20trace%20/%20LogCat**%3A%0A%60%60%60ruby%0Apaste%20stack%20trace%20here%0A%60%60%60
[6]: https://groups.google.com/forum/#!forum/glidelibrary
[7]: https://developers.google.com/open-source/cla/individual
[8]: https://github.com/JakeWharton/DiskLruCache
[9]: https://gist.github.com/devunwired/4479231
[10]: https://github.com/chrisbanes/gradle-mvn-push
[11]: static/glide_logo.png
[12]: https://github.com/bumptech/glide/wiki/Integration-Libraries
[13]: http://webchat.freenode.net/?channels=glide-library
[14]: https://github.com/bumptech/glide/tree/3.0
[15]: https://github.com/bumptech/glide/tree/master
[16]: https://github.com/bumptech/glide/blob/master/LICENSE
