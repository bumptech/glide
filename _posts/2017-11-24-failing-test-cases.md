---
layout: page
title: "Test cases for issues"
category: tut
date: 2017-11-24 14:48:43
disqus: 1
---
* TOC
{:toc}

When reporting a bug in Glide, it's helpful if you can also send a pull request containing a failing test case that demonstrates the issue you're reporting. Failing test cases help avoid communication issues, make it trivial for the maintainers to reproduce the issue, and provides some guarantee that the problem won't re-occur in the future.

This guide will walk you through writing a failing test in Glide, step by step.

## Setup
Before writing any code, you need a few pre-requisites, several of which you'll probably already have if you're working on Android apps on a regular basis:

1. Install and setup [Android Studio](https://developer.android.com/studio/index.html)
2. Create an [Android Emulator in Android Studio](https://developer.android.com/studio/run/managing-avds.html#createavd), using x86 and API 26 should work well.
3. Fork and Clone Glide, then open the project in Android Studio (see [the contributing page](http://bumptech.github.io/glide/dev/contributing.html#contribution-workflow) for more details)

## Adding an Instrumentation Test
Now that you have Glide open in Android Studio, the next step is to write an instrumentation test that will fail due to the bug you're reporting.

Glide's instrumentation tests live in a module called `instrumentation` in the root directory of the project. The full path to the instrumentation tests is `glide/instrumentation/src/androidTest/java`.

### Add a new test file
To add a new instrumentation test file:

1. Expand `instrumentation/src/androidTest/java` in Android Studio's project window
2. Right click on `com.bumptech.glide` (or any appropriate package)
3. Highlight `New` then select `Java Class`
4. Enter an appropriate name (`Issue###Test` if you have an issue number, or just something that describes the problem you're reporting)
5. Click `Ok`

You should now see a new Java class that looks something like this:

```java
package com.bumptech.glide;

public class IssueXyzTest {

}
```

If so, you're ready to move on to writing your test.

### Writing your instrumentation test
After adding your test file, you need to do a little bit of set up so that your test will run reliably before writing your actual test case.

#### Setup
First, you need to specify the Junit 4 test runner by adding `@RunWith(AndroidJUnit4.class)` to your test class:

```java
package com.bumptech.glide;

import android.support.test.runner.AndroidJUnit4;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class IssueXyzTest {

}
```

Next you'll want to add the ``TearDownGlide`` rule which will make sure threads or configuration from one test don't overlap with your test. Doing so just requires adding one line at the top of the file:

```java
package com.bumptech.glide;

import android.support.test.runner.AndroidJUnit4;
import com.bumptech.glide.test.TearDownGlide;
import org.junit.Rule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class IssueXyzTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();

}
```

And we'll create an instance of Glide's ``ConcurrencyHelper`` which helps us make sure our steps execute in order:

```java
package com.bumptech.glide;

import android.support.test.runner.AndroidJUnit4;
import com.bumptech.glide.test.ConcurrencyHelper;
import com.bumptech.glide.test.TearDownGlide;
import org.junit.Rule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class IssueXyzTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();

}
```

Finally we'll add a ``@Before`` step to create a ``Context`` object that we'll need in most of our tests and helper methods:

```java
package com.bumptech.glide;

import android.support.test.runner.AndroidJUnit4;
import com.bumptech.glide.test.ConcurrencyHelper;
import com.bumptech.glide.test.TearDownGlide;
import org.junit.Rule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class IssueXyzTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private Context context;

  @Before
  public void setUp() {
    context = InstrumentationRegistry.getTargetContext();
  }
}
```

That's it! You're now ready to write your actual test.

#### Adding a test method.
The next step is to add your specific test method. In the class file add a method annotated with ``@Test`` so JUnit knows to run it:

```java
@Test
public void method_withSomeSetup_producesExpectedResult() {
}
```

The test method ideally is named with the above format filled in with information specific to your issue, but there are no requirements other than the ``@Test`` annotation.

#### Writing the failing test
Since we're need to write a test case here that does something useful, we'll use [Issue #2638](https://github.com/bumptech/glide/issues/2638) as an example, and write a test case that covers the issue reported there.

The basic problem seems to be that if the reporter runs:

```java
byte[] data = ...
Glide.with(context)
  .load(data)
  .into(imageView);
```

And then runs:

```java
byte[] otherData = ...
Glide.with(context)
  .load(data)
  .into(imageView);
```

The image displayed in ``imageView`` doesn't change even though the two ``byte[]`` passed in to Glide contain different data.

We can pretty easily replicate this by creating two ``byte[]`` with two different images, loading them into an ImageView one after another, and asserting that the ``Drawable``s set on the ImageView are different.

##### Creating the test method

First let's create our test method with a reasonable name:

```java
@Test
public void intoImageView_withDifferentByteArrays_loadsDifferentImages() {
  // TODO: fill this in.
}
```

Since we're going to need an ``ImageView`` to load in to, we might as well create that as well:

```java
@Test
public void intoImageView_withDifferentByteArrays_loadsDifferentImages() {
  final ImageView imageView = new ImageView(context);
  imageView.setLayoutParams(new LayoutParams(/*w=*/ 100, /*h=*/ 100));
}
```

##### Obtaining test data

Next we're going to need the actual data we're going to be loading. Glide's instrumentation tests include a standard test image that we can use, so that will make up our first image. To do so, we'll need to write a function to load the bytes of that image:

```java
private byte[] loadCanonicalBytes() throws IOException {
  int resourceId = ResourceIds.raw.canonical;
  Resources resources = context.getResources();
  InputStream is = resources.openRawResource(resourceId);
  return ByteStreams.toByteArray(is);
}
```

Next we'll need to write a function that provides the bytes of a different image. We could add another resource to `instrumentation/src/main/res/raw` or `instrumentation/src/main/res/drawable` and re-use our existing function, but we can also just modify a pixel of the pixel of our canonical image with another function:

```java
private byte[] getModifiedBytes() throws IOException {
  byte[] canonicalBytes = getCanonicalBytes();
  BitmapFactory.Options options = new BitmapFactory.Options();
  options.inMutable = true;
  Bitmap bitmap = 
      BitmapFactory.decodeByteArray(canonicalBytes, 0 ,canonicalBytes.length, options);
  bitmap.setPixel(0, 0, Color.TRANSPARENT);
  ByteArrayOutputStream os = new ByteArrayOutputStream();
  bitmap.compress(CompressFormat.PNG, /*quality=*/ 100, os);
  return os.toByteArray();
}
```

##### Running Glide


Now all that's left is to write the two load lines above:

```java
@Test
public void intoImageView_withDifferentByteArrays_loadsDifferentImages() throws IOException {
  final ImageView imageView = new ImageView(context);
  imageView.setLayoutParams(new LayoutParams(/*w=*/ 100, /*h=*/ 100));

  final byte[] canonicalBytes = getCanonicalBytes();
  final byte[] modifiedBytes = getModifiedBytes();

  concurrency.loadOnMainThread(Glide.with(context).load(canonicalBytes), imageView);
  Bitmap firstBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

  concurrency.loadOnMainThread(Glide.with(context).load(modifiedBytes), imageView);
  Bitmap secondBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
}
```

We're using `ConcurrencyHelper` here to run the load for Glide on the main thread into ``imageView`` and wait for it to finish. If we just used `into()` directly, the load would happen asynchronously and might not have finished by the next line where we try to retrieve the ``Bitmap`` from ``imageView``. In turn that would throw an exception because we'd end up calling ``getBitmap()`` on a ``null`` ``Drawable``. 

Finally, we need need to add our assertion that the two Bitmaps do in fact contain different data:

##### Asserting on our output

```java
BitmapSubject.assertThat(firstBitmap).isNotSameAs(secondBitmap);
```

`BitmapSubject` is a helper class in Glide that let's you make basic assertions when comparing ``Bitmap``s in instrumentation tests.  

##### All together...
We've now written a a test that generates some test data, runs a couple of methods in Glide, obtains the output of those Glide methods, and then compares the output to ensure that it matches our expections.

Our complete test class looks like this:

```java
package com.bumptech.glide;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.widget.AbsListView.LayoutParams;
import android.widget.ImageView;
import com.bumptech.glide.test.BitmapSubject;
import com.bumptech.glide.test.ConcurrencyHelper;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.test.TearDownGlide;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class Issue2638Test {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private Context context;

  @Before
  public void setUp() {
    context = InstrumentationRegistry.getTargetContext();
  }

  @Test
  public void intoImageView_withDifferentByteArrays_loadsDifferentImages()
      throws IOException, ExecutionException, InterruptedException {
    final ImageView imageView = new ImageView(context);
    imageView.setLayoutParams(new LayoutParams(/*w=*/ 100, /*h=*/ 100));

    final byte[] canonicalBytes = getCanonicalBytes();
    final byte[] modifiedBytes = getModifiedBytes();

    Glide.with(context)
        .load(canonicalBytes)
        .submit()
        .get();

    concurrency.loadOnMainThread(Glide.with(context).load(canonicalBytes), imageView);
    Bitmap firstBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

    concurrency.loadOnMainThread(Glide.with(context).load(modifiedBytes), imageView);
    Bitmap secondBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

    BitmapSubject.assertThat(firstBitmap).isNotSameAs(secondBitmap);
  }

  private byte[] getModifiedBytes() throws IOException {
    byte[] canonicalBytes = getCanonicalBytes();
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inMutable = true;
    Bitmap bitmap =
        BitmapFactory.decodeByteArray(canonicalBytes, 0, canonicalBytes.length, options);
    bitmap.setPixel(0, 0, Color.TRANSPARENT);
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    bitmap.compress(CompressFormat.PNG, /*quality=*/ 100, os);
    return os.toByteArray();
  }

  private byte[] getCanonicalBytes() throws IOException {
    int resourceId = ResourceIds.raw.canonical;
    Resources resources = context.getResources();
    InputStream is = resources.openRawResource(resourceId);
    return ByteStreams.toByteArray(is);
  }
}
```

All that's left to do is run the test and see if it works.

## Running the instrumentation test
Now that you have a test case, you can run it by:

1. Right click on the test file name, either in the project window or the in the tab above your editor
2. Click `Run 'IssueXyzTest'`
3. If a window opens, titled `edit configuration`:
   1. In the `General` tab
   2. Click `Target` and select `Emulator`
   3. Click `Run`
4. If a list of devices opens:
   1. Under `Available Virtual Devices`:
   2. Click any emulator (preferably X86 and API 26)
   3. Click `Ok`
 
You'll see the emulator start and may have to wait 30 seconds or a minute for it to finish starting.

After the emulator starts, you'll see the results of the test in a window below the editor in Android Studio that says either `All Tests Passed`  or `N tests failed` with an exception message.

Once you're finished iterating on your instrumentation tests, you should also check for style issues or common bugs by running:

```sh
./gradlew build
```

**It's ok if your test(s) pass!** 

Please send pull requests for passing tests as well as failing tests. If nothing else, passing tests can help us exclude cases where your bug can't be reproduced so we can focus on other cases where the bug can be reproduced. We might also be able to suggest tweaks or other variations you can that might cause the tests to fail and reveal the bug.

## Creating a pull request.
Now that you have your test case written, you'll need to upload it to your fork of Glide and send a pull request.

First, start by committing your new test file:

```sh
git add intrumentation/src/androidTest/java/com/bumptech/glide/IssueXyzTest.java
git commit -m "Adding test case for issue XYZ"
```

If you had multiple files to add, you can use ``git add .``, but be careful doing so because you can end up accidentally adding files you don't want to commit.

Next, push your modifications to your fork of Glide on GitHub:

```sh
git push origin master
```

Then, create a pull request by:
1. Opening your fork on GitHub (``https://github.com/<your_username>/glide``)
2. Click the `New pull request` button.
3. Click the big green `Create pull request` button
4. Add a Title (Tests for IssueXyz)
5. Fill out as much of the pull request template as possible
6. Click 'Create Pull Request`

That's it! Your pull request will go out and we'll look at it as soon as we're able to.
