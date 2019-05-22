package com.bumptech.glide.module;

import com.bumptech.glide.Registry;

/**
 * An interface allowing lazy configuration of Glide including setting options using {@link
 * com.bumptech.glide.GlideBuilder} and registering {@link com.bumptech.glide.load.model.ModelLoader
 * ModelLoaders}.
 *
 * <p>To use this interface:
 *
 * <ol>
 *   <li>Implement the GlideModule interface in a class with public visibility, calling {@link
 *       Registry#prepend(Class, Class, com.bumptech.glide.load.ResourceDecoder)} for each {@link
 *       com.bumptech.glide.load.model.ModelLoader} you'd like to register:
 *       <pre>
 *                  <code>
 *                      public class FlickrGlideModule implements GlideModule {
 *                          {@literal @}Override
 *                          public void applyOptions(Context context, GlideBuilder builder) {
 *                              builder.setDecodeFormat(DecodeFormat.ALWAYS_ARGB_8888);
 *                          }
 *
 *                          {@literal @}Override
 *                          public void registerComponents(Context context, Glide glide) {
 *                              glide.register(Model.class, Data.class, new MyModelLoader());
 *                          }
 *                      }
 *                  </code>
 *             </pre>
 *   <li>Add your implementation to your list of keeps in your proguard.cfg file:
 *       <pre>{@code
 * -keepnames class * com.bumptech.glide.samples.flickr.FlickrGlideModule
 * }</pre>
 *   <li>Add a metadata tag to your AndroidManifest.xml with your GlideModule implementation's fully
 *       qualified classname as the key, and {@code GlideModule} as the value:
 *       <pre>{@code
 * <meta-data
 *     android:name="com.bumptech.glide.samples.flickr.FlickrGlideModule"
 *     android:value="GlideModule" />
 * }</pre>
 * </ol>
 *
 * <p>All implementations must be publicly visible and contain only an empty constructor so they can
 * be instantiated via reflection when Glide is lazily initialized.
 *
 * <p>There is no defined order in which modules are called, so projects should be careful to avoid
 * applying conflicting settings in different modules. If an application depends on libraries that
 * have conflicting modules, the application should consider avoiding the library modules and
 * instead providing their required dependencies in a single application module.
 *
 * @deprecated Libraries should use {@link LibraryGlideModule} and Applications should use {@link
 *     AppGlideModule}.
 */
@Deprecated
public interface GlideModule extends RegistersComponents, AppliesOptions {}
