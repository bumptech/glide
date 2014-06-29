Glide
=====
Glide is fast and efficient image loading library for Android that wraps image downloading, resizing, memory and disk
caching, and bitmap recycling into one simple and easy to use interface. Glide includes a flexible api allowing it to
plug in to almost any network stack. By default Glide uses a custom HttpUrlConnection based stack, but also includes a
utility library to plug in to Google's Volley project instead.

Glide's primary focus is on making scrolling any kind of a list of images as smooth and fast as possible, but Glide is
also effective for almost any case where you need to fetch, resize, and display a remote image.

Download
--------
You can download a jar from GitHub's [release page](https://github.com/bumptech/glide/releases) or to use the 3.0 alpha
branch, use Gradle:

```groovy
repositories {
  mavenCentral()
  maven {
      url "https://oss.sonatype.org/content/repositories/snapshots"
  }
}

dependencies {
  compile group: 'com.github.bumptech.glide', name:'glide', version:'3.3.0-SNAPSHOT', changing: true
}
```

Or Maven:

In your parent pom:

```xml
<parent>
  <groupId>org.sonatype.oss</groupId>
  <artifactId>oss-parent</artifactId>
  <version>7</version>
</parent>
```

In your module:

```xml
<dependency>
  <groupId>com.github.bumptech.glide</groupId>
  <artifactId>library</artifactId>
  <version>3.3.0-SNAPSHOT</version>
  <type>aar</type>
</dependency>
```

Volley
-------
Volley is now an optional dependency that can be included via a utility library. More utility libraries for other
projects will hopefully be coming soon. To use the utility library with Gradle, add:

```groovy
dependencies {
    compile group: 'com.github.bumptech.glide', name:'volley', version:'3.3.0-SNAPSHOT', changing:true
    compile 'com.mcxiaoke.volley:library:1.0.+'
}
```

Or with maven:

```xml
<dependency>
    <groupId>com.github.bumptech.glide</groupId>
    <artifactId>volley</artifactId>
    <version>3.3.0-SNAPSHOT</version>
    <type>aar</type>
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

How do I use Glide?
-------------------
Checkout the GitHub wiki for pages on a variety of topics and links to javadocs.

Simple use cases will look something like this:

```Java

//For a simple view:
@Override
public void onCreate(Bundle savedInstanceState) {
    ...

    ImageView imageView = (ImageView) findViewById(R.id.my_image_view);

    Glide.with(this).load("http://goo.gl/h8qOq7").into(imageView);
}

//For a list:
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

Status
------
Glide was used at Bump for around a year in two of our Android apps at version 1.0. Version 2.0 was the first public release with a stable api. Version 3.0 is a work in progress and is in use in open source projects at Google including in the Android Camera app and in the 2014 Google IO app. Comments/bugs/questions/pull requests welcome!

Build
------
Building Glide with gradle is fairly straight forward:

```
git submodule init && git submodule update
cd glide/library
./gradlew build
```

Note: Make sure your Android SDK has the Android Support Repository installed, and that your `$ANDROID_HOME` environment variable is pointing at the SDK.

Thanks
------
Thanks to the Android project and Jake Wharton for the [disk cache implementation](https://github.com/JakeWharton/DiskLruCache) included with Glide. Thanks also to the Android team for [Volley](https://android.googlesource.com/platform/frameworks/volley/). Thanks to Dave Smith for his [GifDecoder gist](https://gist.github.com/devunwired/4479231) on which Glide's is based. Thanks also to everyone who has contributed code and reported issues!

Author
------
Sam Judd - @samajudd
