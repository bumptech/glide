---
layout: page
title: "RecyclerView"
category: int
date: 2017-05-10 07:47:20
order: 4
disqus: 1
---

* TOC
{:toc}

### About

The RecyclerView integration library makes the [``RecyclerViewPreloader``][2] available in your application. [``RecyclerViewPreloader``][2] can automatically load images just ahead of where a user is scrolling in a RecyclerView. 

Combined with the right image size and an effective disk cache strategy, this library can dramatically decrease the number of loading tiles/indicators users see when scrolling through lists of images by ensuring that the images the user is about to reach are already in memory.

### Gradle

To use the RecyclerView integration library, add a dependency on it in your ``build.gradle`` file:

```groovy
compile ("com.github.bumptech.glide:recyclerview-integration:4.8.0") {
  // Excludes the support library because it's already included by Glide.
  transitive = false
}
```

If you haven't already, you will also need to make sure that you already have a dependency on ``RecyclerView`` and that you're using ``RecyclerView`` in your app :).

### Setup

To use the ``RecyclerView`` integration library you need to follow a couple of steps:

1. Create a [``PreloadSizeProvider``][3]
2. Create a [``PreloadModelProvider``][6]
3. Create the [``RecyclerViewPreloader``][2] given the [``PreloadSizeProvider``][3] and [``PreloadModelProvider``][6]s you created in the first two steps
4. Add your [``RecyclerViewPreloader``][2] to your ``RecyclerView`` as a scroll listener.

Each of these steps is outlined in more detail below.

#### PreloadSizeProvider

After you add the gradle dependency, you next need to create a [``PreloadSizeProvider``][3]. The [``PreloadSizeProvider``][3] is responsible for making sure your ``RecyclerViewPreloader`` loads images in the same size as those loaded by your adapters ``onBindViewHolder`` method.

Glide provides two built in implementations of [``PreloadSizeProvider``][3]:
1. [``ViewPreloadSizeProvider``][4]
2. [``FixedPreloadSizeProvider``][5]

If you have uniform ``View`` sizes in your ``RecyclerView``, you're loading images with ``into(ImageView)`` and you're not using ``override()`` to set a different size, you can use [``ViewPreloadSizeProvider``][4].

If you're using ``override()`` or are otherwise loading image sizes that don't exactly match the size of your ``Views``, you can use [``FixedPreloadSizeProvider``][5].

If the logic required to determine the image size used for a given position in your ``RecyclerView`` doesn't fit either of those cases, you can always write your own implementation of [``PreloadSizeProvider``][3].

If you are using a fixed size to load your images, typically [``FixedPreloadSizeProvider``][5] is simplest:

```java
private final imageWidthPixels = 1024;
private final imageHeightPixels = 768;

...

PreloadSizeProvider sizeProvider = 
    new FixedPreloadSizeProvider(imageWidthPixels, imageHeightPixels);
```

#### PreloadModelProvider

The next step is to implement your [``PreloadModelProvider``][6]. The [``PreloadModelProvider``][6] performs two actions. First it collects and returns a list of ``Models`` (the items you pass in to Glide's ``load(Object)`` method, like URLs or file paths) for a given position. Second it takes a ``Model`` and produces a Glide ``RequestBuilder`` that will be used to preload the given ``Model`` into memory.

For example, let's say that we have a ``RecyclerView`` that contains a list of image urls where each position in the ``RecyclerView`` displays a single URL. Then, let's say that you load your images in your ``RecyclerView.Adapter``'s ``onBindViewHolder`` method like this:


```java
private List<String> myUrls = ...;

...

@Override
public void onBindViewHolder(ViewHolder viewHolder, int position) {
  ImageView imageView = ((MyViewHolder) viewHolder).imageView;
  String currentUrl = myUrls.get(position);

  GlideApp.with(fragment)
    .load(currentUrl)
    .override(imageWidthPixels, imageHeightPixels)
    .into(imageView);
}
```

Your [``PreloadModelProvider``][6] implementation might then look like this:

```java
private List<String> myUrls = ...;

...

private class MyPreloadModelProvider implements PreloadModelProvider {
  @Override
  @NonNull
  List<U> getPreloadItems(int position) {
    String url = myUrls.get(position);
    if (TextUtils.isEmpty(url)) {
      return Collections.emptyList();
    }
    return Collections.singletonList(url);
  }

  @Override
  @Nullable
  RequestBuilder getPreloadRequestBuilder(String url) {
    return 
      GlideApp.with(fragment)
        .load(url) 
        .override(imageWidthPixels, imageHeightPixels);
  }
}
```

It's critical that the ``RequestBuilder`` returned from ``getPreloadRequestBuilder`` use exactly the same set of options (placeholders, transformations etc) and exactly the same size as the request you start in ``onBindViewHolder``. If any of the options aren't exactly the same in the two methods for a given position, your preload request will be wasted because the image it loads will be cached with a cache key that doesn't match the cache key of the image you load in ``onBindViewHolder``. If you have trouble getting these cache keys to match, see the [debugging page][7].

If you have nothing to preload for a given position, you can return an empty list from ``getPreloadItems``. If you later discover that you're unable to create a ``RequestBuilder`` for a given ``Model``, you may return ``null`` from ``getPreloadRequestBuilder``. 

#### RecyclerViewPreloader

Once you have your [``PreloadSizeProvider``][3] and your [``PreloadModelProvider``][6], you're ready to create your [``RecyclerViewPreloader``][2]:

```java
private final imageWidthPixels = 1024;
private final imageHeightPixels = 768;
private List<String> myUrls = ...;
 
...

PreloadSizeProvider sizeProvider = 
    new FixedPreloadSizeProvider(imageWidthPixels, imageHeightPixels);
PreloadModelProvider modelProvider = new MyPreloadModelProvider();
RecyclerViewPreloader<Photo> preloader = 
    new RecyclerViewPreloader<>(
        Glide.with(this), modelProvider, sizeProvider, 10 /*maxPreload*/);
```

Using 10 for maxPreload is just a placeholder, for a detailed discussion on how to pick a number, see the section immediately below this one.

##### maxPreload

The ``maxPreload`` is an integer that indicates how many items you want to preload. The optimal number will vary by your image size, quantity, the layout of your ``RecyclerView`` and in some cases even the devices your application is running on. 

A good starting point is to pick a number large enough to include all of the images in two or three rows. Once you've picked your initial number, you can try running your application on a couple of devices and tweaking it as necessary to maximize the number of cache hits. 

An overly large number will mean you're preloading too far ahead to be useful. An overly small number will prevent you from loading enough images ahead of time.

#### RecyclerView

The final step, once you have your [``RecyclerViewPreloader``][2] is to add it as a scroll listener to your ``RecyclerView``:

```java
RecyclerView myRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
myRecyclerView.addOnScrollListener(preloader);
```

Adding the [``RecyclerViewPreloader``][2] as a scroll listener allows the [``RecyclerViewPreloader``][2] to automatically load images ahead of the direction the user is scrolling in and detect changes of direction or velocity.

**Warning** - Glide's default scroll listener, [``RecyclerToListViewScrollListener``][8] assumes you're using a [``LinearLayoutManager``][9] or a subclass and will crash if that's not the case. If you're using a different ``LayoutManager`` type, you will need to implement your own [``OnScrollListener``][10], translate the calls ``RecyclerView`` provides into positions, and call [``RecyclerViewPreloader``][2] with those positions.

#### All together

Once you've completed all of these steps, you'll end up with something like this:

```java
public final class ImagesFragment extends Fragment {
  // These are totally arbitrary, pick sizes that are right for your UI.
  private final imageWidthPixels = 1024;
  private final imageHeightPixels = 768;
  // You will need to populate these urls somewhere...
  private List<String> myUrls = ...;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    
    View result = inflater.inflate(R.layout.images_fragment, container, false);

    PreloadSizeProvider sizeProvider = 
        new FixedPreloadSizeProvider(imageWidthPixels, imageHeightPixels);
    PreloadModelProvider modelProvider = new MyPreloadModelProvider();
    RecyclerViewPreloader<Photo> preloader = 
        new RecyclerViewPreloader<>(
            Glide.with(this), modelProvider, sizeProvider, 10 /*maxPreload*/);

    RecyclerView myRecyclerView = (RecyclerView) result.findViewById(R.id.recycler_view);
    myRecyclerView.addOnScrollListener(preloader);
   
    // Finish setting up your RecyclerView etc.
    myRecylerView.setLayoutManager(...);
    myRecyclerView.setAdapter(...);

    ... 

    return result;
  }

  private class MyPreloadModelProvider implements PreloadModelProvider {
    @Override
    @NonNull
    public List<U> getPreloadItems(int position) {
      String url = myUrls.get(position);
      if (TextUtils.isEmpty(url)) {
        return Collections.emptyList();
      }
      return Collections.singletonList(url);
    }

    @Override
    @Nullable
    public RequestBuilder getPreloadRequestBuilder(String url) {
      return 
        GlideApp.with(fragment)
          .load(url) 
          .override(imageWidthPixels, imageHeightPixels);
    }
  }
}
```

### Examples

Glide's [sample apps][11] contain a couple of example usages of [``RecyclerViewPreloader``][2], including:

1. [FlickrPhotoGrid][12], uses a [``FixedPreloadSizeProvider``][5] to preload in the flickr sample's two smaller photo grid views.
2. [FlickrPhotoList][13] uses a [``ViewPreloadSizeProvider``][4] to preload in the flickr sample's larger list view.
3. [MainActivity][14] in the Giphy sample uses a [``ViewPreloadSizeProvider``][4] to preload GIFs while scrolling.
4. [HorizontalGalleryFragment][15] in the Gallery sample uses a custom ``PreloadSizeProvider`` to preload local images while scrolling horizontally.

### Tips and tricks

1. Use ``override()`` to ensure that the images you're loading are uniformally sized in your ``Adapter`` and in your [``RecyclerViewPreloader``][2]. You don't necessarily need to match the size of the ``View`` you're using exactly with the dimensions you pass in to ``override()``, Android's ``ImageView`` class can easily handle scaling up or down minor differences in sizes.
2. Fewer larger images are often faster to load than many smaller images. There's a fair amount of overhead to starting each request, so if you can, load fewer and larger images in your UI.
3. If scrolling is janky, consider using ``override()`` to deliberately decrease image sizes. Uploading textures (Bitmaps) on Android can be expensive, especially for large Bitmaps. You can use ``override()`` to force the images to be smaller than your ``Views`` for smoother scrolling. You can even swap out the lower resolution images with higher resolution images once the user stops scrolling if you're concerned about quality.
4. Check out the [unexpected cache misses][7] section of the debugging docs page if you're having trouble getting your ``Adapter`` to use the images loaded in your [``RecyclerViewPreloader``][2]


### Code

* [https://github.com/bumptech/glide/tree/master/integration/recyclerview][1]

[1]: https://github.com/bumptech/glide/tree/master/integration/recyclerview
[2]: {{ site.baseurl }}/javadocs/420/com/bumptech/glide/integration/recyclerview/RecyclerViewPreloader.html
[3]: {{ site.baseurl }}/javadocs/420/com/bumptech/glide/ListPreloader.PreloadSizeProvider.html
[4]: {{ site.baseurl }}/javadocs/420/com/bumptech/glide/util/ViewPreloadSizeProvider.html
[5]: {{ site.baseurl }}/javadocs/410/com/bumptech/glide/util/FixedPreloadSizeProvider.html
[6]: {{ site.baseurl }}/javadocs/410/com/bumptech/glide/ListPreloader.PreloadModelProvider.html
[7]: /glide/doc/debugging.html#unexpected-cache-misses
[8]: {{ site.baseurl }}/javadocs/420/com/bumptech/glide/integration/recyclerview/RecyclerToListViewScrollListener.html
[9]: https://developer.android.com/reference/android/support/v7/widget/LinearLayoutManager.html
[10]: https://developer.android.com/reference/android/support/v7/widget/RecyclerView.OnScrollListener.html
[11]: /glide/ref/samples.html
[12]: https://github.com/bumptech/glide/blob/853c0d94f1ad353048b3d2556b49729ef3534430/samples/flickr/src/main/java/com/bumptech/glide/samples/flickr/FlickrPhotoGrid.java#L107
[13]: https://github.com/bumptech/glide/blob/853c0d94f1ad353048b3d2556b49729ef3534430/samples/flickr/src/main/java/com/bumptech/glide/samples/flickr/FlickrPhotoList.java#L68
[14]: https://github.com/bumptech/glide/blob/853c0d94f1ad353048b3d2556b49729ef3534430/samples/giphy/src/main/java/com/bumptech/glide/samples/giphy/MainActivity.java#L48
[15]: https://github.com/bumptech/glide/blob/853c0d94f1ad353048b3d2556b49729ef3534430/samples/gallery/src/main/java/com/bumptech/glide/samples/gallery/HorizontalGalleryFragment.java#L53
