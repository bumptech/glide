---
layout: page
title: "Getting Started"
category: doc
date: 2015-05-17 17:01:02
order: 2
disqus: 1
---

### Basic Usage

Loading images with Glide is easy and in many cases requires only a single line:

```java
Glide.with(fragment)
    .load(myUrl)
    .into(imageView);
```

Cancelling loads you no longer need is simple too:

```java
Glide.with(fragment).clear(imageView);
```

Although it's good practice to clear loads you no longer need, you're not required to do so. In fact, Glide will automatically clear the load and recycle any resources used by the load when the Activity or Fragment you pass in to [``Glide.with()``][1] is destroyed.

### Applications

Applications can add an appropriately annotated [``AppGlideModule``][6] implementation to generate a fluent API that inlines most options, including those defined in integration libraries:

```java
package com.example.myapp;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

@GlideModule
public final class MyAppGlideModule extends AppGlideModule {}
```

The API is generated in the same package as the [``AppGlideModule``][6] and is named ``GlideApp`` by default. Applications can use the API by starting all loads with ``GlideApp.with()`` instead of ``Glide.with()``:

```java
GlideApp.with(fragment)
   .load(myUrl)
   .placeholder(placeholder)
   .fitCenter()
   .into(imageView);
```

See Glide's [generated API page][7] for more information.

### ListView and RecyclerView

Loading images in a ListView or RecyclerView uses the same load line as if you were loading in to a single View. Glide handles View re-use and request cancellation automatically:

```java
@Override
public void onBindViewHolder(ViewHolder holder, int position) {
    String url = urls.get(position);
    Glide.with(fragment)
        .load(url)
        .into(holder.imageView);
}
```

You don't need to null check your urls either, Glide will either clear out the view or set whatever [placeholder Drawable][2] or [fallback Drawable][3] you've specified if the url is null.


Glide's sole requirement is that any re-usable ``View`` or [``Target``][5] that you may have started a load into at a previous position either has a new loaded started into it or is explicitly cleared via the [``clear()``][4] API.

```java
@Override
public void onBindViewHolder(ViewHolder holder, int position) {
    if (isImagePosition(position)) {
        String url = urls.get(position);
        Glide.with(fragment)
            .load(url)
            .into(holder.imageView);
    } else {
        Glide.with(fragment).clear(holder.imageView);
        holder.imageView.setImageDrawable(specialDrawable);
    }
}
```

By calling [``clear()``][4] or ``into(View)`` on the ``View``, you're cancelling the load and guaranteeing that Glide will not change the contents of the view after the call completes. If you forget to call [``clear()``][4] and don't start a new load, the load you started into the same View for a previous position may complete after you set your special ``Drawable`` and change the contents of the ``View`` to an old image.

Although the examples we've shown here are for RecyclerView, the same principles apply to ListView as well.

[1]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/Glide.html#with-android.app.Fragment-
[2]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/request/RequestOptions.html#placeholder-int-
[3]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/request/RequestOptions.html#fallback-int-
[4]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/RequestManager.html#clear-com.bumptech.glide.request.target.Target-
[5]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/request/target/Target.html
[6]: {{ site.url }}/glide/javadocs/400/com/bumptech/glide/module/AppGlideModule.html
[7]: {{ site.url }}/glide/doc/generatedapi.html
