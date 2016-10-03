# Contributing
Contributions of all types are welcome.
We use GitHub as our bug and feature tracker both for code and for other aspects of the library (documentation, the wiki, etc.).


## Asking Questions
The best way to ask general questions is to send an email to our [mailing list][2], or join [#glide-library on freenode.org][3].


## Filing issues
When in doubt, file an issue. We'd rather close a few duplicate issues than let a problem go unnoticed.
Similarly if you support a particular feature request, feel free to let us know by commenting on the issue or [subscribing][6] to the issue.

To file a new issue, please use our issue template and fill out the template as much as possible (remove irrelevant parts).
The more information you can provide, the more likely we are to be able help.


## Contributing code
Pull requests are welcome for all parts of the codebase, especially the integration libraries.
You can find instructions on building the project in [README.md][5].
Our code style is defined in Intellij project files in the repo and also by our Checkstyle config.
If you'd like to submit code, but can't get the style checks to pass, feel free to put up your pull request anyway and we can help you fix the style issues.
If you'd like to contribute code, you will need to sign [Google's individual contributor license agreement][4] which will be asked when you create the PR by [googlebot](https://github.com/googlebot) should you forget it.

## Labels
Labels on issues are managed by contributors, you don't have to worry about them. Here's a list of what they mean:

 * **bug**: feature that should work, but doesn't
 * **enhancement**: minor tweak/addition to existing behavior
 * **feature**: new behavior, bigger than enhancement, it gives more bang to Glide
 * **question**: no need to modify Glide to fix the issue, usually a usage problem
 * **reproducible**: has enough information to very easily reproduce, mostly in form of a small project in a GitHub repo
 * **repro-needed**: we need some code to be able to reproduce and debug locally, otherwise there's not much we can do
 * **duplicate**: there's another issue which already covers/tracks this
 * **wontfix**: working as intended, or won't be fixed due to compatibility or other reasons
 * **invalid**: there isn't enough information to make a verdict, or unrelated to Glide
 * **non-library**: issue is not in the core library code, but rather in documentation, samples, build process, releases
 * **v4**: problem originated in v4, or question about v4 (while v3 is in wide use)

*bug + enhancement: feature that doesn't work, but it's an edge case that either has a workaround or doesn't affect many users*


[1]: https://github.com/bumptech/glide/issues/new?body=**Glide%20Version**%3A%0A**Integration%20libraries**%3A%0A**Device/Android%20Version**%3A%0A**Issue%20details%20/%20Repro%20steps%20/%20Use%20case%20background**%3A%0A%0A**Glide%20load%20line**%3A%0A%60%60%60java%0AGlide.with%28...%29.....load%28...%29.....into%28...%29%3B%0A%60%60%60%0A%0A**Layout%20XML**%3A%0A%60%60%60xml%0A%3C...Layout%3E%0A%20%20%20%20%3CImageView%20android%3AscaleType%3D%22...%22%20...%20/%3E%0A%3C/..Layout%3E%0A%60%60%60%0A%0A**Stack%20trace%20/%20LogCat**%3A%0A%60%60%60ruby%0Apaste%20stack%20trace%20here%0A%60%60%60
[2]: https://groups.google.com/forum/#!forum/glidelibrary
[3]: http://webchat.freenode.net/?channels=glide-library
[4]: https://developers.google.com/open-source/cla/individual
[5]: https://github.com/bumptech/glide
[6]: https://help.github.com/articles/subscribing-to-conversations/
