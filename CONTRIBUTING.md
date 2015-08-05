# Contributing
Contributions of all types are welcome.
We use GitHub as our bug and feature tracker both for code and for other aspects of the library (documentation, the wiki, etc.).


## Asking Questions
The best way to ask general questions is to send an email to our [mailing list][2], or join [#glide-library on freenode.org][3].


## Filing issues
When in doubt, file an issue. We'd rather close a few duplicate issues than let a problem go unnoticed.
Similarly if you support a particular feature request, feel free to let us know by commenting on the issue.

To file a new issue, please use our issue template and fill out the template as much as possible (remove irrelevant parts).

<pre>**Glide Version/Integration library (if any)**:
**Device/Android Version**:
**Issue details/Repro steps/Use case background**:

**Glide load line**:
```java
Glide.with(...).....load(...).....into(...);
```

**Layout XML**:
```xml
&lt;...Layout&gt;
    &lt;ImageView android:scaleType="..." ... /&gt;
&lt;/..Layout&gt;
```

**Stack trace / LogCat**:
```ruby
paste stack trace here
```
</pre>

You can save [this as a bookmark or just click it][1] to create a new issue.
The more information you can provide, the more likely we are to be able help.


## Contributing code
Pull requests are welcome for all parts of the codebase, especially the integration libraries.
You can find instructions on building the project in [README.md][5].
Our code style is defined in Intellij project files in the repo and also by our Checkstyle config.
If you'd like to submit code, but can't get the style checks to pass, feel free to put up your pull request anyway and we can help you fix the style issues.
If you'd like to contribute code, you will need to sign [Google's individual contributor license agreement][4] which will be asked when you create the PR by [googlebot](https://github.com/googlebot) should you forget it.


[1]: https://github.com/bumptech/glide/issues/new?body=**Glide%20Version/Integration%20library%20%28if%20any%29**%3A%0A**Device/Android%20Version**%3A%0A**Issue%20details/Repro%20steps/Use%20case%20background**%3A%0A%0A**Glide%20load%20line**%3A%0A%60%60%60java%0AGlide.with%28...%29.....load%28...%29.....into%28...%29%3B%0A%60%60%60%0A%0A**Layout%20XML**%3A%0A%60%60%60xml%0A%3C...Layout%3E%0A%20%20%20%20%3CImageView%20android%3AscaleType%3D%22...%22%20...%20/%3E%0A%3C/..Layout%3E%0A%60%60%60%0A%0A**Stack%20trace%20/%20LogCat**%3A%0A%60%60%60ruby%0Apaste%20stack%20trace%20here%0A%60%60%60
[2]: https://groups.google.com/forum/#!forum/glidelibrary
[3]: http://webchat.freenode.net/?channels=glide-library
[4]: https://developers.google.com/open-source/cla/individual
[5]: /bumptech/glide