---
layout: page
title: "RecyclerView"
category: int
date: 2017-05-10 07:47:20
order: 4
disqus: 1
---

### RecyclerView
The RecyclerView library adds a class that will automatically load images just ahead of where a user is scrolling in a RecyclerView. Combined with the right image size and an effective disk cache strategy, this library can dramatically decrease the number of loading tiles/indicators users see when scrolling through lists of images.

**Code:**
[https://github.com/bumptech/glide/tree/master/integration/recyclerview][1]

**Gradle Dependency:**
```groovy
compile ("com.github.bumptech.glide:recyclerview-integration:4.1.1") {
  // Excludes the support library because it's already included by Glide.
  transitive = false
}
```

[1]: https://github.com/bumptech/glide/tree/master/integration/recyclerview
