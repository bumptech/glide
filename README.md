Glide
=====
Glide is fast and efficient image loading library for Android that wraps image downloading, resizing, memory and disk caching, and bitmap recycling into one simple and easy to use interface. By default, Glide includes an implementation for fetching images over http based on Google's Volley project for fast, parallelized network operations on Android.

Glide's primary focus is on making scrolling any kind of a list of images as smooth and fast as possible, but Glide is also effective for almost any case where you need to fetch, resize, and display a remote image.

How do I use Glide?
-------------------
You can download a .jar from GitHub's release page for the Glide project. The wiki also has pages on a variety of topics and the javadocs for version 2.0+ will also be available via a link there as well.

Simple use cases will look something like this:

```Java

//For a simple view:
@Override
public void onCreate(Bundle savedInstanceState) {
    ...

    ImageView imageView = (ImageView) findViewById(R.id.my_image_view);

    Glide.load("http://goo.gl/h8qOq7").into(imageView);
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

    Glide.load(url)
        .centerCrop()
        .placeholder(R.drawable.loading_spinner)
        .animate(R.anim.fade_in)
        .into(myImageView);

    return myImageView;
}

```

Status
------
Glide has been in use at Bump for about six months in two of our Android apps at version 1.0. Version 2.0 is the first public release with a stable api. Comments/bugs/questions/pull requests welcome!

Thanks
------
Thanks to the Android project and Jake Wharton for the [disk cache implementation](https://github.com/JakeWharton/DiskLruCache) included with Glide. Thanks also to the Android team for [Volley](https://android.googlesource.com/platform/frameworks/volley/).

Author
------
Sam Judd - @samajudd
