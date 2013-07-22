Glide
=====
Glide is an all in one image download and resizing library for Android that wraps image downloading, resizing, memory and disk caching, and bitmap recycling into one simple and easy to use interface. Glide includes an implementation for fetching images over http based on Google's Volley project for fast, parallelized network operations on Android.

Glide works best for long lists or grids where every item contains an image or images, but it's also effective for almost any case where you need to fetch, resize, and display a remote image.

How do I use Glide?
-------------------
The GitHub project wiki has tutorials on a variety of topics, including basic usage and adding Glide to a project. The javadocs for version 1.0+ will also be available via a link on the wiki.

Simple use cases will look something like this:

```Java

@Override
public View getView(int position, View recycled, ViewGroup container) {
    final ImageView myImageView;
    if (recycled == null) {
        myImageView = (ImageView) inflater.inflate(R.layout.my_image_view, container, false);
    } else {
        myImageView = (ImageView) recycled);
    }

    URL myUrl = myUrls.get(position);

    Glide.load(myUrl).into(myImageView).centerCrop().animate(R.anim.fade_in).begin();

    return myImageView;
}
```

Status
------
Glide has been in use at Bump for about six months in two of our Android apps. The API after 1.0 is mostly stable though there may be some superficial changes. Comments/bugs/questions/pull requests welcome!

Thanks
------
Thanks to the Android project and Jake Wharton for the [disk cache implementation](https://github.com/JakeWharton/DiskLruCache) included with Glide.
Thanks also to Android team for [Volley](https://android.googlesource.com/platform/frameworks/volley/)
