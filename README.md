Glide
=====
Glide is a view centric image loading library for Android that wraps image downloading, resizing, memory and disk caching, and bitmap reuse into one simple and easy to use interface. Glide includes an implementation for fetching images over http based on Google's Volley project for fast, parallelized network operations on Android.

Glide works best for long lists or grids where every item contains an image or images, but it's also effective for almost any case where you need to fetch, resize, and display a remote image in a view.

How do I use Glide?
-------------------
The GitHub project wiki has tutorials on a variety of topics, including basic usage and adding Glide to a project. The javadocs for version 2.0+ will also be available via a link on the wiki.

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
    if (recycled == null) {
        myImageView = (ImageView) inflater.inflate(R.layout.my_image_view, container, false);
    } else {
        myImageView = (ImageView) recycled);
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
Glide has been in use at Bump for about six months in two of our Android apps at version 1.0. Version 2.0 is in progress and will include a more general and easier to use interface. After version 2.0 the api will be stable and any outdated apis will first be deprecated before being removed. That being said, the 2.0+ api is definitely a work in progress and new functionality will probably be added in intermediate versions. Comments/bugs/questions/pull requests welcome!

Thanks
------
Thanks to the Android project and Jake Wharton for the [disk cache implementation](https://github.com/JakeWharton/DiskLruCache) included with Glide.
Thanks also to the Android team for [Volley](https://android.googlesource.com/platform/frameworks/volley/)
