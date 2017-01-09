---
layout: page
title: "Contributing"
category: dev
date: 2016-10-13 08:29:39
disqus: 1
order: 1
---
### Source

Contributions to Glide's source are welcome!

#### Obtaining and uploading changes.

To make contributions to Glide's source:

1. Clone [Glide repo][1] from GitHub.
2. Create a new branch to make your changes: ``git checkout -b <branch_name>``.
3. Make your contributions.
4. Send a [pull request][2] on GitHub.


#### Building the project.

To build the project, you typically need to run a single gradle command from the project root directory:

``./gradlew build``


#### Testing changes

##### Tests

All of Glide's tests, including unit tests and code analysis, are run when you build the project.

You can run just the tests using: ``./gradlew test``.

##### Sample projects

Glide's test are not completely comprehensive and do not effectively test for performance regressions. To verify your changes work and do not negatively affect performance (or improve performance!), it's also a good idea to try running one or more of Glide's sample projects.

Glide's sample projects are located in samples/. Sample projects can be built and installed onto a device or emulator using gradle:

``./gradlew :samples:<sample_name>:run``

For example, to run the Flickr demo:

``./gradlew :samples:flickr:run``

### Documentation

#### Obtaining and uploading changes

To make contributions to this documentation site:

1. Clone the [Glide repo][1].
2. Checkout the gh-pages branch: ``git checkout -t bump/gh-pages``.
3. Make your contributions.
4. Send a [pull request][2] on GitHub.

#### Modifying existing pages

The pages you see on the docs page are located in the ``_pages`` folder and can be modified in place.

#### Adding a new page
New pages can be added using ``./bin/jekyll-page <page_name> <category>``. ``<page_name>`` is the title of the page, ``<category>`` matches one of the sections in the left hand nav. Typically ``<category>`` should be ``doc`` so that the page is placed under the ``Documentation`` section.

When adding a new page, make sure you add ``disqus: 1`` and ``order: <n>`` to the header. The number you give to order is used to order the pages within the subsection, with 0 being the first page. To just add the page at the end (a reasonable default), find the order value for the last page in the section and use that value + 1 for your new page.

The final header will look something like this: 
```
---
layout: page
title: "Targets"
category: doc
date: 2015-05-26 07:03:23
order: 6
disqus: 1
---
```

#### Viewing your local changes

To view your local changes, you will need to install jekyll and the gems used by the docs page:

``sudo gem install jekyll redcarpet pygments.rb``

Then you can run jekyll locally:

``jekyll serve --watch``

Finally you can view a version of the site with your local changes: ``http://127.0.0.1:4000/glide/``. The exact address will be printed out by jekyll.

[1]: https://github.com/bumptech/glide
[2]: https://help.github.com/articles/creating-a-pull-request/
