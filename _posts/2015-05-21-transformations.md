---
layout: page
title: "Transformations"
category: doc
date: 2015-05-21 20:04:53
order: 5
disqus: 1
---

### About

[Transformations][1] in Glide take a resource, mutate it, and return the mutated resource. Typically transformations are used to crop, or apply filters to Bitmaps, but they can also be used to transform animated GIFs, or even custom resource types.

### Built in types

Glide includes a number of built in transformations, including:

* [CenterCrop][4]
* [FitCenter][2]
* [CircleCrop][6]

### Applying Transformations

Transformations are applied using the [RequestOptions][9] class:

```java
RequestOptions options = new RequestOptions();
options.centerCrop(context);

Glide.with(fragment)
    .load(url)
    .apply(options)
    .into(imageView);
```

Most built in transformations also have static imports for a more fluent API. For example, you can apply a [FitCenter][2] transformation using a static method:

```java
import static com.bumptech.glide.request.RequestOptions.fitCenterTransform;

Glide.with(fragment)
    .load(url)
    .apply(fitCenterTransform(context))
    .into(imageView);
```

For more information on using RequestOptions, see the [Options][3] wiki page.

### Automatic Transformations for ImageViews

When you start a load into an [ImageView][7] in Glide, Glide may automatically apply either [FitCenter][2] or [CenterCrop][4], depending on the [ScaleType][8] of the view. If the scale type is ``CENTER_CROP``, Glide will automatically apply the ``CenterCrop`` transformation. If the scale type is ``FIT_CENTER`` or ``CENTER_INSIDE``, Glide will automatically apply the ``FitCenter`` transformation.

You can always override the default transformation by applying a [RequestOptions][9] with a ``Transformation`` set. In addition, you can ensure no ``Transformation`` is automatically applied using [``dontTransform()``][10].

### Application and custom resources

Because Glide 4.0 allows you to specify a super type of the resource you're going to decode, you may not know exactly what type of transformation to apply. For example, when you use [``asDrawable()``][11] (or just ``with()`` since ``asDrawable()`` is the default) to ask for a Drawable resource, you may get either the [``BitmapDrawable``][12] subclass, or the [``GifDrawable``][13] subclass. 

To ensure any ``Transformation`` you add to your ``RequestOptions`` is applied, Glide adds your ``Transformation`` to a map keyed on the resource class you provide to [``transform()``][14]. When a resource is successfully decoded , Glide uses the map to retrieve a corresponding ``Transformation``. 

Glide can apply ``Bitmap`` ``Transformations`` to ``BitmapDrawable``, ``GifDrawable``, and ``Bitmap`` resources, so typically you only need to write and apply ``Bitmap`` ``Transformations``. However, if you add additional resource types you may need to consider sub-classing [``BaseRequestOptions``][15] and always applying a ``Transformation`` for your custom resource type in addition to the built in ``Bitmap`` ``Transformations``.


[1]: http://bumptech.github.io/glide/javadocs/400/com/bumptech/glide/load/Transformation.html
[2]: http://bumptech.github.io/glide/javadocs/400/com/bumptech/glide/load/resource/bitmap/FitCenter.html
[3]: doc/options.html
[4]: http://bumptech.github.io/glide/javadocs/400/com/bumptech/glide/load/resource/bitmap/CenterCrop.html
[6]: http://bumptech.github.io/glide/javadocs/400/com/bumptech/glide/load/resource/bitmap/CircleCrop.html
[7]: http://developer.android.com/reference/android/widget/ImageView.html
[8]: http://developer.android.com/reference/android/widget/ImageView.ScaleType.html
[9]: http://bumptech.github.io/glide/javadocs/400/com/bumptech/glide/request/RequestOptions.html
[10]: http://bumptech.github.io/glide/javadocs/400/com/bumptech/glide/request/BaseRequestOptions.html#dontTransform()
[11]: http://bumptech.github.io/glide/javadocs/400/com/bumptech/glide/RequestManager.html#asDrawable()
[12]: http://developer.android.com/reference/android/graphics/drawable/BitmapDrawable.html
[13]: http://bumptech.github.io/glide/javadocs/400/com/bumptech/glide/load/resource/gif/GifDrawable.html
[14]: http://bumptech.github.io/glide/javadocs/400/com/bumptech/glide/request/BaseRequestOptions.html#transform(java.lang.Class,%20com.bumptech.glide.load.Transformation)
[15]: http://bumptech.github.io/glide/javadocs/400/com/bumptech/glide/request/BaseRequestOptions.html
