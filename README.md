Glide
=====
Glide is an image loading library for Android that wraps image loading, resizing, memory and disk caching, and bitmap recycling into one simple and easy to use interace.

Glide works best for long lists or grids where every item contains an image or images, but it's also effective for almost any case where you need to fetch, resize, and display a remote image.

Who Shouldn't Use Glide?
------------------------
If you only have local assets or very infrequently display remote assets and never display remote or resized assets in a list, then Glide is probably overkill for you.

Glide is also focused primarily on loading images from the sdcard efficiently and does not currently include code to download images from a URL (though that code does exist in the sample project).

Also you probably shouldn't bother if you're not writing an Android app...

Who Should Use Glide?
---------------------
Anyone who displays large numbers of images can benefit from Glide.

Glide abstracts away most of the complications with image view recycling in lists, as well as resizing and caching images.

Glide is also unobtrusive. It doesn't require you to change any xml or create any subclasses.

Finally Glide is performant. Image loading is fast without using lots of cpu or causing jerk inducing garbage collections.

How Do I Use Glide?
-------------------
The only thing you need to implement is a simple interface that fetches a path for a given model. You then need an ImageManager instance which manages loading images from paths in the background, and an ImagePresenter instance per image view. To use Glide in an adapter for a list, your code will look something like this:

```Java

public class MyAdapter extends BaseAdapter {
    private final ImageManager imageManager;

    public MyAdapter(Context context) {
         imageManager = new ImageManager.Builder(context).build();
    }

    ...
    @Override
    public View getView(int position, View recycled, ViewGroup container) {
        final MyModel current = myModels.get(position);
        final ImagePresenter<MyModel> presenter;
        if (recycled == null) {
            recycled = myInflater.inflate(R.layout.my_image_view, container, false);

            presenter = new ImagePresenter.Builder<MyModel>()
                    .setImageView((ImageView) recycled)
                    .setPathLoader(new MyPathLoader())
                    .setImageLoader(new CenterCrop(imageManager))
                    .build();
            recycled.setTag(presenter);
        } else {
            presenter = (ImagePresenter<MyModel>) recycled.getTag();
        }
        presenter.setModel(current);
    }
}

```

The ImagePresenter will ensure that the view only displays the most recent model. It also determines the exact size of the image and passes that to both the PathLoader and the ImageLoader.

This means that you can fetch the correctly sized image directly or Glide can resize each image to the right size even if you don't know the size at compile time (because you're using layout weights for example).

Behind the scenes the ImageManager will load resize the image from disk using a background thread and then cache that resized image in memory and on disk. This means that you don't have to waste cpu and memory displaying images that are larger than your view. On newer devices (SDK >= 11), the ImageManager will also recycle bitmaps and prevent the garbage collection pauses that typically go along with bitmap allocation.

Fewer garbage collections, less memory per image, and less cpu time dedicated to resizing each image on the fly means dramatically smoother scrolling and faster image loading.

For more examples see the sample flickr app.

How do I add Glide to my project?
--------------------------------
Glide is an Androlid library project so its fairly straightforward to add it. Either add this repo as a submodule or otherwise check out this repository in your project and then follow the steps in the Android docs to add it as a library project: http://developer.android.com/tools/projects/projects-cmdline.html#ReferencingLibraryProject

Intellij and Eclipse instructions coming soon.

Status
------
Glide has been in use at Bump for about six months in two of our Android apps. The API is mostly stable though there may be some superficial changes. Comments/bugs/questions/pull requests welcome!
