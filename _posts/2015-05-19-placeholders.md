---
layout: page
title: "Placeholders"
category: doc
date: 2015-05-19 07:14:23
order: 4
disqus: 1
---
* TOC
{:toc}

### Types
Glide allows users to specify three different placeholders that are used under different circumstances:

* [placeholder][1]
* [error][2]
* [fallback][3]

#### Placeholder
Placeholders are Drawables that are shown while a request is in progress. When a request completes successfully, the placeholder is replaced with the requested resource. If the requested resource is loaded from memory, the placeholder may never be shown. If the request fails and an error Drawable is not set, the placeholder will continue to be displayed. Similarly if the requested url/model is ``null`` and neither an error Drawable nor a fallback Drawable are set, the placeholder will also continue to be displayed.

With the [generated API][4]:

```java
GlideApp.with(fragment)
  .load(url)
  .placeholder(R.drawable.placeholder)
  .into(view);
```

Or:

```java
GlideApp.with(fragment)
  .load(url)
  .placeholder(new ColorDrawable(Color.BLACK))
  .into(view);
```

#### Error
Error Drawables are shown when a request permanently fails. Error Drawables are also shown if the requested url/model is ``null`` and no fallback Drawable is set

With the [generated API][4]:

```java
GlideApp.with(fragment)
  .load(url)
  .error(R.drawable.error)
  .into(view);
```

Or:

```java
GlideApp.with(fragment)
  .load(url)
  .error(new ColorDrawable(Color.RED))
  .into(view);
```

#### Fallback
Fallback Drawables are shown when the requested url/model is ``null``. The primary purpose of fallback Drawables is to allow users to indicate whether or not ``null`` is expected. For example, a ``null`` profile url may indicate that the user has not set a profile photo and that a default should be used. However, ``null`` may also indicate that meta-data is invalid or couldn't be retrieved. By default Glide treats ``null`` urls/models as errors, so users who expect ``null`` should set a fallback Drawable.

With the [generated API][4]:

```java
GlideApp.with(fragment)
  .load(url)
  .fallback(R.drawable.fallback)
  .into(view);
```

Or:

```java
GlideApp.with(fragment)
  .load(url)
  .fallback(new ColorDrawable(Color.GREY))
  .into(view);
```

### FAQ

##### Are placeholders loaded asynchronously?
No. Placeholders are loaded from Android resources on the main thread. We typically expect placeholders to be small and easily cacheable by the system resource cache.

##### Are Transformations applied to placeholders?
No. Transformations are applied only to the requested resource, not to any placeholder. If you're loading circular images for example, you may want to include circular placeholder resources with your application. Alternatively you could also consider a custom View to clip your placeholder in the same manner as your Transformation.

##### Is it ok to use the same Drawable as a placeholder in multiple Views?
Usually, but not always. Any non-stateful Drawable (like BitmapDrawable) is typically ok to display in multiple views at once. Stateful Drawables however, are typically not safe to display in multiple views at the same time because multiple Views will mutate the state at once. For stateful Drawables, pass in a resource id, or use ``newDrawable()`` to pass in a new copy to each request.

[1]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/request/RequestOptions.html#placeholder-int-
[2]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/request/RequestOptions.html#error-int-
[3]: {{ site.baseurl }}/javadocs/400/com/bumptech/glide/request/RequestOptions.html#fallback-int-
[4]: generatedapi.html
