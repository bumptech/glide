---
layout: page
title: "Transformations"
category: doc
date: 2015-05-21 20:04:53
order: 6
disqus: 1
---
* TOC
{:toc}

### About
[Transformations][1] in Glide take a resource, mutate it, and return the mutated resource. Typically transformations are used to crop, or apply filters to Bitmaps, but they can also be used to transform animated GIFs, or even custom resource types.

### Built in types

Glide includes a number of built in transformations, including:

* [CenterCrop][4]
* [FitCenter][2]
* [CircleCrop][6]

### Applying Transformations
Transformations are applied using the [RequestOptions][9] class:

#### Default Transformations

```java
RequestOptions options = new RequestOptions();
options.centerCrop();

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
    .apply(fitCenterTransform())
    .into(imageView);
```

If you're using the [generated API][16] the transformation methods are inlined, so it's even easier:

```java
GlideApp.with(fragment)
  .load(url)
  .fitCenter()
  .into(imageView);
```

For more information on using RequestOptions, see the [Options][3] wiki page.

#### Multiple Transformations.
By default, each subsequent call to [``transform()``][17] or any specific transform method (``fitCenter()``, ``centerCrop()``, ``bitmapTransform()`` etc) will replace the previous transformation.

To instead apply multiple transformations to a single load, use the [``MultiTransformation``][18] class or the shortcut [``.transforms()``][19] method.

With the [generated API][16]:

```java
GlideApp.with(fragment)
  .load(url)
  .transform(new MultiTransformation(new FitCenter(), new YourCustomTransformation())
  .into(imageView);
```

Or with the shortcut method and the [generated API][16]:

```java
GlideApp.with(fragment)
  .load(url)
  .transforms(new FitCenter(), new YourCustomTransformation())
  .into(imageView);
```

The order in which you pass transformations to [``MultiTransformation``][18]'s constructor determines the order in which the transformations are applied.

### Custom transformations.

Although Glide provides a variety of built in [``Transformation``][1] implementations, you may also implement your own custom [``Transformation``][2]s if you need additional functionality.

#### BitmapTransformation

If you only need to transform ``Bitmap``s, it's best to start by subclassing [``BitmapTransformation``][20]. ``BitmapTransformation`` takes care of a few of the basic things for you, including extracting recycling the original Bitmap if your ``Transformation`` returns a new modified Bitmap.

A simple implementation might look something like this:

```java
public class FillSpace extends BitmapTransformation {
    private static final String ID = "com.bumptech.glide.transformations.FillSpace";
    private static final String ID_BYTES = ID.getBytes(STRING_CHARSET_NAME);

    {@literal @Override}
    public Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
        if (toTransform.getWidth() == outWidth && toTransform.getHeight() == outHeight) {
            return toTransform;
        }

        return Bitmap.createScaledBitmap(toTransform, outWidth, outHeight, /*filter=*/ true);
    }

    {@literal @Override}
    public void equals(Object o) {
      return o instanceof FillSpace;
    }

    {@literal @Override}
    public int hashCode() {
      return ID.hashCode();
    }

    {@literal @Override}
    public void updateDiskCacheKey(MessageDigest messageDigest)
        throws UnsupportedEncodingException {
      messageDigest.update(ID_BYTES);
    }
}
```

Although your ``Transformation`` will almost certainly do something more sophisticated than our example, it should contain the same basic elements and method overrides.

#### Required methods

In particular, note that there are three methods that you **must** implement for any ``Transformation`` subclass, including ``BitmapTransformation`` in order for disk and memory caching to work correctly:

1. ``equals()``
2. ``hashCode()``
3. ``updateDiskCacheKey``

If your [``Transformation``][1] takes no arguments, it's often easy to just use a ``static`` ``final`` ``String`` containing the fully qualified package name as an id that can form the basis of ``hashCode()`` and can be used to update the ``MessageDigest`` passed to ``updateDiskCacheKey()``. If your [``Transformation``][1] does take arguments that affect the way the ``Bitmap`` is transformed, they must be included in these three methods as well.

For example, Glide's [``RoundedCorners``][21] ``Transformation`` accepts an ``int`` that determines the radius of the rounded corners. Its implementations of ``equals()``, ``hashCode()`` and ``updateDiskCacheKey`` looks like this:

```java
  @Override
  public boolean equals(Object o) {
    if (o instanceof RoundedCorners) {
      RoundedCorners other = (RoundedCorners) o;
      return roundingRadius == other.roundingRadius;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Util.hashCode(ID.hashCode(),
        Util.hashCode(roundingRadius));
  }

  @Override
  public void updateDiskCacheKey(MessageDigest messageDigest) {
    messageDigest.update(ID_BYTES);

    byte[] radiusData = ByteBuffer.allocate(4).putInt(roundingRadius).array();
    messageDigest.update(radiusData);
  }
```

The original ``String`` id remains as well, but the ``roundingRadius`` is included in all three methods as well. The ``updateDiskCacheKey`` method here also demonstrates how you can use ``ByteBuffer`` to including primitive arguments in your ``updateDiskCacheKey`` implementation.

#### Don't forget equals()/hashCode()!

It's worth re-iterating one more time that it's essential that you implement ``equals()`` and ``hashCode()`` for memory caching to work correctly. Unfortunately ``BitmapTransformation`` and ``Transformation`` implementations will compile if those methods are not overridden, but that doesn't mean they work correctly. We're exploring options for making using the default ``equals()`` and ``hashCodeMethods`` a compile time error in future versions of Glide.

### Special Behavior in Glide

#### Re-using Transformations
``Transformations`` are meant to be stateless. As a result, it should always be safe to re-use a ``Transformation`` instance for multiple loads. It's usually good practice to create a ``Transformation`` once and then pass it in to multiple loads.

#### Automatic Transformations for ImageViews
When you start a load into an [ImageView][7] in Glide, Glide may automatically apply either [FitCenter][2] or [CenterCrop][4], depending on the [ScaleType][8] of the view. If the scale type is ``CENTER_CROP``, Glide will automatically apply the ``CenterCrop`` transformation. If the scale type is ``FIT_CENTER`` or ``CENTER_INSIDE``, Glide will automatically apply the ``FitCenter`` transformation.

You can always override the default transformation by applying a [RequestOptions][9] with a ``Transformation`` set. In addition, you can ensure no ``Transformation`` is automatically applied using [``dontTransform()``][10].

#### Custom resources
Because Glide 4.0 allows you to specify a super type of the resource you're going to decode, you may not know exactly what type of transformation to apply. For example, when you use [``asDrawable()``][11] (or just ``with()`` since ``asDrawable()`` is the default) to ask for a Drawable resource, you may get either the [``BitmapDrawable``][12] subclass, or the [``GifDrawable``][13] subclass. 

To ensure any ``Transformation`` you add to your ``RequestOptions`` is applied, Glide adds your ``Transformation`` to a map keyed on the resource class you provide to [``transform()``][14]. When a resource is successfully decoded , Glide uses the map to retrieve a corresponding ``Transformation``. 

Glide can apply ``Bitmap`` ``Transformations`` to ``BitmapDrawable``, ``GifDrawable``, and ``Bitmap`` resources, so typically you only need to write and apply ``Bitmap`` ``Transformations``. However, if you add additional resource types you may need to consider sub-classing [``RequestOptions``][15] and always applying a ``Transformation`` for your custom resource type in addition to the built in ``Bitmap`` ``Transformations``.

[1]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/Transformation.html
[2]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/resource/bitmap/FitCenter.html
[3]: options.html
[4]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/resource/bitmap/CenterCrop.html
[6]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/resource/bitmap/CircleCrop.html
[7]: http://developer.android.com/reference/android/widget/ImageView.html
[8]: http://developer.android.com/reference/android/widget/ImageView.ScaleType.html
[9]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/request/RequestOptions.html
[10]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/request/RequestOptions.html#dontTransform--
[11]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/RequestManager.html#asDrawable--
[12]: http://developer.android.com/reference/android/graphics/drawable/BitmapDrawable.html
[13]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/resource/gif/GifDrawable.html
[14]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/request/RequestOptions.html#transform-java.lang.Class-com.bumptech.glide.load.Transformation-
[15]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/request/RequestOptions.html
[16]: {{ site.baseurl }}/doc/generatedapi.html
[17]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/request/RequestOptions.html#transform-java.lang.Class-com.bumptech.glide.load.Transformation-
[18]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/load/MultiTransformation.html
[19]: {{ site.baseurl }}/javadocs/410/com/bumptech/glide/request/RequestOptions.html#transforms-com.bumptech.glide.load.Transformation...-
[20]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/load/resource/bitmap/BitmapTransformation.html
[21]: {{ site.baseurl }}/javadocs/440/com/bumptech/glide/load/resource/bitmap/RoundedCorners.html
