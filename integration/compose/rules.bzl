"""
Workaround for the lack of kt_android_library_test_rule (b/243549140).
"""

load("//tools/build_defs/kotlin:rules.bzl", "kt_android_library")
load("//tools/build_defs/android:rules.bzl", "android_library_test")

def kt_android_library_test(name, size, srcs, custom_package, manifest, manifest_values, deps, target_devices, test_class):
    """A simple equivalent of android_library_test that works with Kotlin.

    This is not well generalized. A better solution is b/243549140, which would
    mean adding a real kt_android_library_test to Android's test_macros:
    http://google3/tools/build_defs/android/dev/test_macros.bzl;l=17;rcl=470614953

    While this is only used in one place and we could theoretically move a bunch
    of constants out of the test rule into this one, it seems better not to do
    so. Leaving the constant values in the calling BUILD file should make a
    migration to a real kt_android_library_test rule easier in the future.

    Args:
      name: The test name
      size: The test size, probably large
      srcs: The test library source set
      custom_package: The test library and android_library_test package
      manifest: The android_library_test manifest
      manifest_values: The android_library_test manifest values
      deps: the test library and android_library_test dependencies
      target_devices: the target devices passed to android_library_test
      test_class: the test class for the android_library_test
    """
    library_attrs = {}
    library_attrs["srcs"] = srcs
    library_attrs["deps"] = deps
    library_attrs["testonly"] = 1
    library_attrs["custom_package"] = custom_package

    libname = name + "_lib"

    test_attrs = {}
    test_attrs["deps"] = [":" + libname]
    test_attrs["size"] = size
    test_attrs["manifest"] = manifest
    test_attrs["multidex"] = "legacy"

    test_attrs["target_devices"] = target_devices
    test_attrs["manifest"] = manifest
    test_attrs["manifest_values"] = manifest_values
    test_attrs["test_class"] = test_class

    kt_android_library(libname, **library_attrs)
    android_library_test(name, **test_attrs)
