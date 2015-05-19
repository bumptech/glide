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
    .asDrawable()
    .load(myUrl)
    .into(imageView);
```

Cancelling loads you no longer need is simple too:

```java
Glide.with(fragment).clear(imageView);
```

Although it's good practice to clear loads you no longer need, you're not required to do so. In fact, Glide will automatically clear the load and recycle any resources used by the load when the Activity or Fragment you pass in to [``Glide.with()``][1] is destroyed.


### ListView and RecyclerView

Loading images in a ListView or RecyclerView uses the same load line as if you were loading in to a single View. Glide handles View re-use and request cancellation automatically:

```java
@Override
public void onBindViewHolder(ViewHolder holder, int position) {
    String url = urls.get(position);
    Glide.with(fragment)
        .asDrawable()
        .load(url)
        .into(holder.imageView);
}
```

You don't need to null check your urls either, Glide will either clear out the view or set whatever [placeholder Drawable][2] or [fallback Drawable][3] you've specified if the url is null.

The only time you need to do anything special is if you want to set some custom resource on your view that you're not going to load using Glide:

```java
@Override
public void onBindViewHolder(ViewHolder holder, int position) {
    if (isSpecialPosition(position)) {
        Glide.with(fragment).clear(holder.imageView);
        holder.imageView.setImageDrawable(specialDrawable);
    } else {
        String url = urls.get(position);
        Glide.with(fragment)
            .asDrawable()
            .load(url)
            .into(holder.imageView);
    }
}
```
By calling [clear()][4] on the View, you're cancelling the load and guaranteeing that Glide will not change the contents of the view after the call completes. If you forget to call [clear()][4], the load you started into the same View for a previous position may complete after you set your special Drawable and change the contents of the View to an old image.


Although the examples we've shown here are for RecyclerView, the same principles apply to ListView as well.

[1]: http://bumptech.github.io/glide/javadocs/400/com/bumptech/glide/Glide.html#with(android.support.v4.app.Fragment)
[2]: http://bumptech.github.io/glide/javadocs/400/com/bumptech/glide/request/BaseRequestOptions.html#placeholder(int)
[3]: http://bumptech.github.io/glide/javadocs/400/com/bumptech/glide/request/BaseRequestOptions.html#fallback(int)
[4]: http://bumptech.github.io/glide/javadocs/400/com/bumptech/glide/RequestManager.html#clear(com.bumptech.glide.request.target.Target)
