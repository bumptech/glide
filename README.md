Glide
=====
Glide is fast and efficient image loading library for Android that wraps image downloading, resizing, memory and disk
caching, and bitmap recycling into one simple and easy to use interface. Glide includes a flexible api allowing it to
plug in to almost any network stack. By default Glide uses a custom HttpUrlConnection based stack, but also includes
utility libraries plug in to Google's Volley project or Square's OkHttp library instead.

Glide's primary focus is on making scrolling any kind of a list of images as smooth and fast as possible, but Glide is
also effective for almost any case where you need to fetch, resize, and display a remote image.

Download
--------
You can download a jar from GitHub's [release page](https://github.com/bumptech/glide/releases) or to use the 3.0 alpha
branch, use Gradle:

```groovy
repositories {
  mavenCentral()
}

dependencies {
    compile 'com.github.bumptech.glide:glide:3.3.0'
    compile 'com.android.support:support-v4:19.0.0'
}
```

Or Maven:

In your module:

```xml
<dependency>
  <groupId>com.github.bumptech.glide</groupId>
  <artifactId>glide</artifactId>
  <version>3.3.0</version>
  <type>aar</type>
</dependency>
<dependency>
  <groupId>com.google.android</groupId>
  <artifactId>support-v4</artifactId>
  <version>r7</version>
</dependency>
```

How do I use Glide?
-------------------
Checkout the GitHub wiki for pages on a variety of topics and links to javadocs.

Simple use cases will look something like this:

```Java

// For a simple view:
@Override
public void onCreate(Bundle savedInstanceState) {
    ...

    ImageView imageView = (ImageView) findViewById(R.id.my_image_view);

    Glide.with(this).load("http://goo.gl/h8qOq7").into(imageView);
}

// For a list:
@Override
public View getView(int position, View recycled, ViewGroup container) {
    final ImageView myImageView;
    if (recycled == null) {
        myImageView = (ImageView) inflater.inflate(R.layout.my_image_view,
                container, false);
    } else {
        myImageView = (ImageView) recycled;
    }

    String url = myUrls.get(position);

    Glide.with(myFragment)
        .load(url)
        .centerCrop()
        .placeholder(R.drawable.loading_spinner)
        .animate(R.anim.fade_in)
        .into(myImageView);

    return myImageView;
}

```

Volley
-------
Volley is now an optional dependency that can be included via a utility library. To use Volley to fetch media over http/https:

With Gradle:

```groovy
dependencies {
    compile 'com.github.bumptech.glide:volley-integration:3.3.0'
    compile 'com.mcxiaoke.volley:library:1.0.+'
}
```

Or with Maven:

```xml
<dependency>
    <groupId>com.github.bumptech.glide</groupId>
    <artifactId>volley-integration</artifactId>
    <version>3.3.0</version>
    <type>jar</type>
</dependency>
<dependency>
    <groupId>com.mcxiaoke.volley</groupId>
    <artifactId>library</artifactId>
    <version>1.0.5</version>
    <type>aar</type>
</dependency>
```

Then in your Activity or Application, register the Volley based model loader:
```java
public void onCreate() {
  Glide.get(this).register(GlideUrl.class, InputStream.class, new VolleyUrlLoader.Factory(yourRequestQueue));
  ...
}
```

After the call to register any requests using http or https will go through Volley.

OkHttp
------
In addition to Volley, Glide also includes support for fetching media using OkHttp. To use OkHttp to fetch media over http/https:

With Gradle:

```groovy
dependencies {
    compile 'com.github.bumptech.glide:okhttp-integration:3.3.0'
    compile 'com.squareup.okhttp:okhttp:2.0.+'
}
```

Or with Maven:

```xml
<dependency>
    <groupId>com.github.bumptech.glide</groupId>
    <artifactId>okhttp-integration</artifactId>
    <version>3.3.0</version>
    <type>jar</type>
</dependency>
<dependency>
    <groupId>com.squareup.okhttp</groupId>
    <artifactId>okhttp</artifactId>
    <version>2.0.0</version>
    <type>jar</type>
</dependency>
```

Then in your Activity or Application, register the OkHttp based model loader:
```java
public void onCreate() {
  Glide.get(this).register(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(yourOkHttpClient));
  ...
}
```

Status
------
Version 3.0 is in the beta stage with a nearly final api and is used in multiple open source projects at Google including in the Android Camera app and in the 2014 Google IO app. Comments/bugs/questions/pull requests welcome!

Build
------
Building Glide with gradle is fairly straight forward:

```
git clone git@github.com:bumptech/glide.git
cd glide
git checkout origin/3.0a
git submodule init && git submodule update
./gradlew jar
```

Note: Make sure your Android SDK has the Android Support Repository installed, and that your `$ANDROID_HOME` environment variable is pointing at the SDK.

Development
-----------
Follow the steps in the 'Build' section to setup the project and then edit the files however you wish. Intellij's [IDEA 14 early access build](http://confluence.jetbrains.com/display/IDEADEV/IDEA+14+EAP) cleanly imports both Glide's source and tests and is the recommended way to work with Glide. Earlier versions of intellij do not import the gradle project cleanly. Although Android Studio imports the source cleanly, it is not possible to run or debug the tests without manually modifying the tests' classpath. To open the project in Intellij 14 go to File > Open... > navigate to Glide's root directory > select settings.gradle.

Getting Help
------------
To report a specific problem or feature request, [open a new issue on Github](https://github.com/bumptech/glide/issues/new). For questions, suggestions, or anything else, join or email [Glide's discussion group](https://groups.google.com/forum/#!forum/glidelibrary)

Contributing
------------
Before submitting pull requests, contributors must sign Google's [individual contribution license agreement](https://developers.google.com/open-source/cla/individual).

Thanks
------
Thanks to the Android project and Jake Wharton for the [disk cache implementation](https://github.com/JakeWharton/DiskLruCache) Glide's disk cache is based on, to Dave Smith for the [gif decoder implementation](https://gist.github.com/devunwired/4479231) Glide's gif decoder is based on and thanks also to everyone who has contributed code and reported issues!

Author
------
Sam Judd - @samajudd

Disclaimer
---------
This is not an official Google product.
