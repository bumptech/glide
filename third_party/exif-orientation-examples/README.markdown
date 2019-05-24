EXIF Orientation-flag example images
====================================

Example images using each of the EXIF orientation flags (1-to-8), in both landscape and portrait orientations.

[See here](http://www.daveperrett.com/articles/2012/07/28/exif-orientation-handling-is-a-ghetto/) for more information.


Generating your own images
--------------------------

If you would like to generate test images based on your own photos, you can use the `generate.rb` script included in the `generator` folder.

The instructions below assume you are running on OSX - if not, you will need to install the Ghostscript fonts (`brew install gs`) some other way.

To install the dependencies:

```
> brew install gs
> cd generator
> gem install bundler
> bundle install
```

To generate test images:

```
> cd generator
> ./generate path/to/image.jpg
```

This will create images `image_1.jpg` through to `image_8.jpg`.


Re-generating sample images
---------------------------

Simply run `make` to regenerate the included sample images. This will download random portrait and landscape orientation images from [unsplash.com](https://unsplash.com/) and generate sample images for each of them.

Generating these images depends on having the generator dependencies installed - see the *Generating your own images* section for instructions on installing dependencies.


Credits
-------

* The sample landscape image is by [Pierre Bouillot](https://unsplash.com/photos/v15iOM6pWgI).
* The sample portrait image is by [John Salvino](https://unsplash.com/photos/1PPpwrTNkJI).


Change history
--------------

* **Version 2.0.0 (2017-08-05)** : Add a script to generate example images from the command line.
* **Version 1.0.2 (2017-03-06)** : Remove Apple Copyrighted ICC profile from orientations 2-8 (thanks @mans0954!).
* **Version 1.0.1 (2013-03-10)** : Add MIT license and some contact details.
* **Version 1.0.0 (2012-07-28)** : 1.0 release.


Contributing
------------

Once you've made your commits:

1. [Fork](http://help.github.com/fork-a-repo/) exif-orientation-examples
2. Create a topic branch - `git checkout -b my_branch`
3. Push to your branch - `git push origin my_branch`
4. Create a [Pull Request](http://help.github.com/pull-requests/) from your branch
5. That's it!


Author
------

Dave Perrett :: hello@daveperrett.com :: [@daveperrett](http://twitter.com/daveperrett)


Copyright
---------

These images are licensed under the [MIT License](http://opensource.org/licenses/MIT).

Copyright (c) 2010 Dave Perrett. See [License](https://github.com/recurser/exif-orientation-examples/blob/master/LICENSE) for details.
