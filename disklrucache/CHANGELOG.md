Change Log
==========

Version 2.0.2 *(2013-06-18)*
----------------------------

 * Fix: Prevent exception trying to delete a non-existent file.


Version 2.0.1 *(2013-04-27)*
----------------------------

 * Fix: Do not throw runtime exceptions for racy file I/O.
 * Fix: Synchronize calls to `isClosed`.


Version 2.0.0 *(2013-04-13)*
----------------------------

The package name is now `com.jakewharton.disklrucache`.

 * New: Automatically flush the cache when an edit is completed.
 * Fix: Ensure file handles are not held when a file is not found.
 * Fix: Correct journal rebuilds on Windows.
 * Fix: Ensure file writer uses the appropriate encoding.


Version 1.3.1 *(2013-01-02)*
----------------------------

 * Fix: Correct logic around detecting whether a journal rebuild is required.
   *(Thanks Jonathan Gerbaud)*


Version 1.3.0 *(2012-12-24)*
----------------------------

 * Re-allow dash in cache key (now `[a-z0-9_-]{1,64}`).
 * New: `getLength` method on `Snapshot`. *(Thanks Edward Dale)*
 * Performance improvements reading journal lines.


Version 1.2.1 *(2012-10-08)*
----------------------------

 * Fix: Ensure library references Java 5-compatible version of
   `Arrays.copyOfRange`. *(Thanks Edward Dale)*


Version 1.2.0 *(2012-09-30)*
----------------------------

 * New API for cache size adjustment.
 * Keys are now enforced to match `[a-z0-9_]{1,64}` *(Thanks Brian Langel)*
 * Fix: Cache will gracefully recover if directory is deleted at runtime.


Version 1.1.0 *(2012-01-07)*
----------------------------

 * New API for editing an existing snapshot. *(Thanks Jesse Wilson)*


Version 1.0.0 *(2012-01-04)*
----------------------------

Initial version.
