package com.bumptech.glide.module;

import android.content.Context;

import com.bumptech.glide.Glide;

/**
 * An interface allowing lazy registration of {@link com.bumptech.glide.load.model.ModelLoader ModelLoaders}.
 *
 * <p>
 *     To use this interface:
 *     <ol>
 *         <li>
 *             Implement the GlideModule interface in a class with public visibility, calling
 *             {@link com.bumptech.glide.Glide#register(Class, Class, com.bumptech.glide.load.model.ModelLoaderFactory)}
 *             for each {@link com.bumptech.glide.load.model.ModelLoader} you'd like to register:
 *             <pre>
 *                  <code>
 *                      public class FlickrGlideModule implements GlideModule {
 *                          {@literal @}Override
 *                          public void initialize(Context context, Glide glide) {
 *                              glide.register(Model.class, Data.class, new MyModelLoader());
 *                          }
 *                      }
 *                  </code>
 *             </pre>
 *         </li>
 *         <li>
 *              Add your implementation to your list of keeps in your proguard.cfg file:
 *              <pre>
 *                  {@code
 *                      -keepnames class * com.bumptech.glide.samples.flickr.FlickrGlideModule
 *                  }
 *              </pre>
 *         </li>
 *         <li>
 *             Add a metadata tag to your AndroidManifest.xml with your GlideModule implementation's fully qualified
 *             classname as the key, and {@code GlideModule} as the value:
 *             <pre>
 *                 {@code
 *                      <meta-data
 *                          android:name="com.bumptech.glide.samples.flickr.FlickrGlideModule"
 *                          android:value="GlideModule" />
 *                 }
 *             </pre>
 *         </li>
 *     </ol>
 * </p>
 *
 * <p>
 *     All implementations must be publicly visible and contain only an empty constructor so they can be instantiated
 *     via reflection when Glide is lazily initialized.
 * </p>
 */
public interface GlideModule {
    void initialize(Context context, Glide glide);
}
