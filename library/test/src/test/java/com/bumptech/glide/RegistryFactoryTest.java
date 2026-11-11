package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoader.LoadData;
import com.bumptech.glide.load.model.UnitModelLoader;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.tests.TearDownGlide;
import com.bumptech.glide.util.GlideSuppliers.GlideSupplier;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(sdk = Build.VERSION_CODES.Q)
public class RegistryFactoryTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();
  private final Context context = ApplicationProvider.getApplicationContext();

  private static final class TestException extends RuntimeException {
    private static final long serialVersionUID = 2334956185897161236L;
  }

  @Test
  public void create_whenCalledTwiceWithThrowingModule_throwsOriginalException() {
    AppGlideModule throwingAppGlideModule =
        new AppGlideModule() {
          @Override
          public void registerComponents(
              @NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
            throw new TestException();
          }
        };

    Glide glide = Glide.get(context);
    GlideSupplier<Registry> registrySupplier =
        RegistryFactory.lazilyCreateAndInitializeRegistry(
            glide, /* manifestModules= */ ImmutableList.of(), throwingAppGlideModule);

    assertThrows(
        TestException.class,
        new ThrowingRunnable() {
          @Override
          public void run() {
            registrySupplier.get();
          }
        });

    assertThrows(
        TestException.class,
        new ThrowingRunnable() {
          @Override
          public void run() {
            registrySupplier.get();
          }
        });
  }

  @Test
  public void lazilyCreate_whenImageDecoderEnabled_localGifLoadsAsAnimated() throws Exception {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      return;
    }
    GlideBuilder builder = new GlideBuilder();
    builder.setImageDecoderEnabledForBitmaps(true);
    builder.setUriImageDecoderEnabled(true);
    Glide.init(context, builder);

    ContentValues values = new ContentValues();
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/gif");
    Uri gifUri =
        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    try (OutputStream os = context.getContentResolver().openOutputStream(gifUri)) {
      os.write(TINY_GIF);
    }

    // Load the GIF as a Drawable on a background thread.
    Future<Drawable> future =
        Executors.newSingleThreadExecutor()
            .submit(() -> Glide.with(context).asDrawable().load(gifUri).submit().get());

    Drawable drawable = future.get(5, TimeUnit.SECONDS);

    assertThat(drawable).isInstanceOf(GifDrawable.class);
  }

  @Test
  public void imageDecoderDecodesTinyGif() throws Exception {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      return;
    }
    ContentValues values = new ContentValues();
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/gif");
    Uri gifUri =
        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    try (OutputStream os = context.getContentResolver().openOutputStream(gifUri)) {
      os.write(TINY_GIF);
    }

    android.graphics.ImageDecoder.Source source =
        android.graphics.ImageDecoder.createSource(context.getContentResolver(), gifUri);
    try {
      Bitmap bitmap = android.graphics.ImageDecoder.decodeBitmap(source);
      System.out.println("ImageDecoder succeeded: " + bitmap);
    } catch (Throwable t) {
      System.out.println("ImageDecoder failed!");
      t.printStackTrace();
      throw new RuntimeException(t);
    }
  }

  @Test
  public void fileModelLoaders_whenFileDescriptorImageDecoderDisabled_prefersInputStream() {
    initGlide(
        /* isImageDecoderEnabledForBitmaps= */ true,
        /* isFileDescriptorImageDecoderEnabled= */ false);

    List<Class<?>> dataClasses =
        getFetcherDataClasses(Glide.get(context).getRegistry(), new File("/tmp/image.jpg"));

    assertThat(dataClasses)
        .containsExactly(
            ByteBuffer.class, InputStream.class, ParcelFileDescriptor.class, File.class)
        .inOrder();
  }

  @Test
  public void fileModelLoaders_whenFileDescriptorImageDecoderEnabled_prefersParcelFileDescriptor() {
    initGlide(
        /* isImageDecoderEnabledForBitmaps= */ true,
        /* isFileDescriptorImageDecoderEnabled= */ true);

    List<Class<?>> dataClasses =
        getFetcherDataClasses(Glide.get(context).getRegistry(), new File("/tmp/image.jpg"));

    assertThat(dataClasses)
        .containsExactly(
            ByteBuffer.class, ParcelFileDescriptor.class, InputStream.class, File.class)
        .inOrder();
  }

  @Test
  public void fileModelLoaders_withoutImageDecoderForBitmaps_prefersInputStream() {
    initGlide(
        /* isImageDecoderEnabledForBitmaps= */ false,
        /* isFileDescriptorImageDecoderEnabled= */ true);

    List<Class<?>> dataClasses =
        getFetcherDataClasses(Glide.get(context).getRegistry(), new File("/tmp/image.jpg"));

    assertThat(dataClasses)
        .containsExactly(
            ByteBuffer.class, InputStream.class, ParcelFileDescriptor.class, File.class)
        .inOrder();
  }

  @Test
  public void stringModelLoaders_whenFileDescriptorImageDecoderDisabled_prefersInputStream() {
    initGlide(
        /* isImageDecoderEnabledForBitmaps= */ true,
        /* isFileDescriptorImageDecoderEnabled= */ false);

    List<Class<?>> dataClasses =
        getFetcherDataClasses(Glide.get(context).getRegistry(), "/tmp/image.jpg");

    assertThat(dataClasses)
        .containsExactly(InputStream.class, ParcelFileDescriptor.class, AssetFileDescriptor.class)
        .inOrder();
  }

  @Test
  public void
      stringModelLoaders_whenFileDescriptorImageDecoderEnabled_prefersFileDescriptorsOverInputStream() {
    initGlide(
        /* isImageDecoderEnabledForBitmaps= */ true,
        /* isFileDescriptorImageDecoderEnabled= */ true);

    List<Class<?>> dataClasses =
        getFetcherDataClasses(Glide.get(context).getRegistry(), "/tmp/image.jpg");

    assertThat(dataClasses)
        .containsExactly(ParcelFileDescriptor.class, AssetFileDescriptor.class, InputStream.class)
        .inOrder();
  }

  @Test
  public void contentUriModelLoaders_whenFileDescriptorImageDecoderDisabled_prefersInputStream() {
    initGlide(
        /* isImageDecoderEnabledForBitmaps= */ true,
        /* isFileDescriptorImageDecoderEnabled= */ false);
    Uri contentUri = Uri.parse("content://com.example.provider/images/1");

    List<Class<?>> dataClasses =
        getFetcherDataClasses(Glide.get(context).getRegistry(), contentUri);

    assertThat(dataClasses)
        .containsExactly(
            InputStream.class,
            ParcelFileDescriptor.class,
            AssetFileDescriptor.class,
            contentUri.getClass())
        .inOrder();
  }

  @Test
  public void
      contentUriModelLoaders_whenFileDescriptorImageDecoderEnabled_prefersFileDescriptorsOverInputStream() {
    initGlide(
        /* isImageDecoderEnabledForBitmaps= */ true,
        /* isFileDescriptorImageDecoderEnabled= */ true);
    Uri contentUri = Uri.parse("content://com.example.provider/images/1");

    List<Class<?>> dataClasses =
        getFetcherDataClasses(Glide.get(context).getRegistry(), contentUri);

    assertThat(dataClasses)
        .containsExactly(
            ParcelFileDescriptor.class,
            AssetFileDescriptor.class,
            InputStream.class,
            contentUri.getClass())
        .inOrder();
  }

  @Test
  public void
      mediaStoreUriModelLoaders_whenFileDescriptorImageDecoderDisabled_prefersInputStream() {
    initGlide(
        /* isImageDecoderEnabledForBitmaps= */ true,
        /* isFileDescriptorImageDecoderEnabled= */ false);
    Uri mediaStoreUri =
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath("1").build();

    List<Class<?>> dataClasses =
        getFetcherDataClasses(Glide.get(context).getRegistry(), mediaStoreUri);

    assertThat(dataClasses)
        .containsExactly(
            InputStream.class,
            ParcelFileDescriptor.class,
            InputStream.class,
            ParcelFileDescriptor.class,
            AssetFileDescriptor.class,
            File.class,
            mediaStoreUri.getClass())
        .inOrder();
  }

  @Test
  public void
      mediaStoreUriModelLoaders_whenFileDescriptorImageDecoderEnabled_prefersFileDescriptorsOverInputStream() {
    initGlide(
        /* isImageDecoderEnabledForBitmaps= */ true,
        /* isFileDescriptorImageDecoderEnabled= */ true);
    Uri mediaStoreUri =
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath("1").build();

    List<Class<?>> dataClasses =
        getFetcherDataClasses(Glide.get(context).getRegistry(), mediaStoreUri);

    assertThat(dataClasses)
        .containsExactly(
            ParcelFileDescriptor.class,
            InputStream.class,
            ParcelFileDescriptor.class,
            AssetFileDescriptor.class,
            InputStream.class,
            File.class,
            mediaStoreUri.getClass())
        .inOrder();
  }

  @Test
  public void assetUriModelLoaders_whenFileDescriptorImageDecoderDisabled_prefersInputStream() {
    initGlide(
        /* isImageDecoderEnabledForBitmaps= */ true,
        /* isFileDescriptorImageDecoderEnabled= */ false);
    Uri assetUri = Uri.parse("file:///android_asset/image.jpg");

    List<Class<?>> dataClasses = getFetcherDataClasses(Glide.get(context).getRegistry(), assetUri);

    assertThat(dataClasses).containsAtLeast(InputStream.class, AssetFileDescriptor.class).inOrder();
  }

  @Test
  public void
      assetUriModelLoaders_whenFileDescriptorImageDecoderEnabled_prefersAssetFileDescriptor() {
    initGlide(
        /* isImageDecoderEnabledForBitmaps= */ true,
        /* isFileDescriptorImageDecoderEnabled= */ true);
    Uri assetUri = Uri.parse("file:///android_asset/image.jpg");

    List<Class<?>> dataClasses = getFetcherDataClasses(Glide.get(context).getRegistry(), assetUri);

    assertThat(dataClasses).containsAtLeast(AssetFileDescriptor.class, InputStream.class).inOrder();
  }

  @Test
  public void resourceUriModelLoaders_whenFileDescriptorImageDecoderDisabled_prefersInputStream() {
    initGlide(
        /* isImageDecoderEnabledForBitmaps= */ true,
        /* isFileDescriptorImageDecoderEnabled= */ false);
    Uri resourceUri = Uri.parse("android.resource://" + context.getPackageName() + "/1");

    List<Class<?>> dataClasses =
        getFetcherDataClasses(Glide.get(context).getRegistry(), resourceUri);

    assertThat(dataClasses)
        .containsExactly(
            InputStream.class,
            AssetFileDescriptor.class,
            InputStream.class,
            ParcelFileDescriptor.class,
            AssetFileDescriptor.class,
            resourceUri.getClass())
        .inOrder();
  }

  @Test
  public void
      resourceUriModelLoaders_whenFileDescriptorImageDecoderEnabled_prefersAssetFileDescriptor() {
    initGlide(
        /* isImageDecoderEnabledForBitmaps= */ true,
        /* isFileDescriptorImageDecoderEnabled= */ true);
    Uri resourceUri = Uri.parse("android.resource://" + context.getPackageName() + "/1");

    List<Class<?>> dataClasses =
        getFetcherDataClasses(Glide.get(context).getRegistry(), resourceUri);

    assertThat(dataClasses)
        .containsExactly(
            AssetFileDescriptor.class,
            InputStream.class,
            ParcelFileDescriptor.class,
            AssetFileDescriptor.class,
            InputStream.class,
            resourceUri.getClass())
        .inOrder();
  }

  @Test
  public void
      assetFileDescriptorLoadPath_whenFileDescriptorImageDecoderDisabled_usesFallbackDecoder()
          throws Exception {
    initGlide(
        /* isImageDecoderEnabledForBitmaps= */ true,
        /* isFileDescriptorImageDecoderEnabled= */ false);
    Bitmap decoded = decodePngAssetFileDescriptor(new RequestOptions());

    assertThat(decoded.getWidth()).isEqualTo(1);
    assertThat(decoded.getHeight()).isEqualTo(1);
  }

  @Test
  public void
      assetFileDescriptorLoadPath_whenFileDescriptorImageDecoderEnabled_usesImageDecoderAheadOfFallback()
          throws Exception {
    initGlide(
        /* isImageDecoderEnabledForBitmaps= */ true,
        /* isFileDescriptorImageDecoderEnabled= */ true);
    Bitmap decoded = decodePngAssetFileDescriptor(new RequestOptions());

    assertThat(decoded.getWidth()).isEqualTo(10);
    assertThat(decoded.getHeight()).isEqualTo(10);
  }

  @Test
  public void
      parcelFileDescriptorLoadPath_whenFileDescriptorImageDecoderDisabled_usesFallbackDecoder()
          throws Exception {
    initGlide(
        /* isImageDecoderEnabledForBitmaps= */ true,
        /* isFileDescriptorImageDecoderEnabled= */ false);
    Bitmap decoded = decodePngParcelFileDescriptor(new RequestOptions());

    assertThat(decoded.getWidth()).isEqualTo(1);
    assertThat(decoded.getHeight()).isEqualTo(1);
  }

  @Test
  public void
      parcelFileDescriptorLoadPath_whenFileDescriptorImageDecoderEnabled_usesImageDecoderAheadOfFallback()
          throws Exception {
    initGlide(
        /* isImageDecoderEnabledForBitmaps= */ true,
        /* isFileDescriptorImageDecoderEnabled= */ true);
    Bitmap decoded = decodePngParcelFileDescriptor(new RequestOptions());

    assertThat(decoded.getWidth()).isEqualTo(10);
    assertThat(decoded.getHeight()).isEqualTo(10);
  }

  @Test
  public void
      parcelFileDescriptorLoadPath_whenExplicitVideoFrameRequested_fallsBackAfterImageDecoder()
          throws Exception {
    initGlide(
        /* isImageDecoderEnabledForBitmaps= */ true,
        /* isFileDescriptorImageDecoderEnabled= */ true);
    Bitmap decoded = decodePngParcelFileDescriptor(new RequestOptions().frame(1000L));

    assertThat(decoded.getWidth()).isEqualTo(1);
    assertThat(decoded.getHeight()).isEqualTo(1);
  }

  private void initGlide(
      boolean isImageDecoderEnabledForBitmaps, boolean isFileDescriptorImageDecoderEnabled) {
    Glide.init(
        context,
        new GlideBuilder()
            .setImageDecoderEnabledForBitmaps(isImageDecoderEnabledForBitmaps)
            .setFileDescriptorImageDecoderEnabled(isFileDescriptorImageDecoderEnabled));
  }

  private static <T> List<Class<?>> getFetcherDataClasses(Registry registry, T model) {
    Options options = new Options();
    List<Class<?>> dataClasses = new ArrayList<>();
    for (ModelLoader<T, ?> loader : registry.getModelLoaders(model)) {
      // Use 1000x1000 so thumbnail-only MediaStore loaders do not short-circuit full-size Uris.
      LoadData<?> loadData =
          loader.buildLoadData(model, /* width= */ 1000, /* height= */ 1000, options);
      if (loadData != null) {
        dataClasses.add(loadData.fetcher.getDataClass());
      }
    }
    return dataClasses;
  }

  private Bitmap decodePngParcelFileDescriptor(RequestOptions extraOptions) throws Exception {
    File pngFile = createTenByTenPngFile();
    Glide.get(context)
        .getRegistry()
        .prepend(
            ParcelFileDescriptor.class,
            ParcelFileDescriptor.class,
            UnitModelLoader.Factory.getInstance())
        .append(
            Registry.BUCKET_BITMAP,
            ParcelFileDescriptor.class,
            Bitmap.class,
            new OneByOneFallbackBitmapDecoder<>());
    RequestOptions requestOptions =
        new RequestOptions()
            .apply(extraOptions)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true);
    try (ParcelFileDescriptor pfd =
        ParcelFileDescriptor.open(pngFile, ParcelFileDescriptor.MODE_READ_ONLY)) {
      return Executors.newSingleThreadExecutor()
          .submit(() ->
              Glide.with(context)
                  .asBitmap()
                  .load(pfd)
                  .apply(requestOptions)
                  .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                  .get())
          .get(5, TimeUnit.SECONDS);
    }
  }

  private Bitmap decodePngAssetFileDescriptor(RequestOptions extraOptions) throws Exception {
    File pngFile = createTenByTenPngFile();
    Glide.get(context)
        .getRegistry()
        .prepend(
            AssetFileDescriptor.class,
            AssetFileDescriptor.class,
            UnitModelLoader.Factory.getInstance())
        .append(
            Registry.BUCKET_BITMAP,
            AssetFileDescriptor.class,
            Bitmap.class,
            new OneByOneFallbackBitmapDecoder<>());
    RequestOptions requestOptions =
        new RequestOptions()
            .apply(extraOptions)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true);
    try (ParcelFileDescriptor pfd =
            ParcelFileDescriptor.open(pngFile, ParcelFileDescriptor.MODE_READ_ONLY);
        AssetFileDescriptor afd =
            new AssetFileDescriptor(pfd, /* startOffset= */ 0, pngFile.length())) {
      return Executors.newSingleThreadExecutor()
          .submit(() ->
              Glide.with(context)
                  .asBitmap()
                  .load(afd)
                  .apply(requestOptions)
                  .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                  .get())
          .get(5, TimeUnit.SECONDS);
    }
  }

  private static final class OneByOneFallbackBitmapDecoder<T>
      implements ResourceDecoder<T, Bitmap> {
    @Override
    public boolean handles(@NonNull T source, @NonNull Options options) {
      return true;
    }

    @Override
    public Resource<Bitmap> decode(
        @NonNull T source, int width, int height, @NonNull Options opts) {
      return BitmapResource.obtain(
          Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), new BitmapPoolAdapter());
    }
  }

  private File createTenByTenPngFile() throws IOException {
    File file = temporaryFolder.newFile("ten_by_ten.png");
    Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    bitmap.eraseColor(Color.BLUE);
    try (FileOutputStream out = new FileOutputStream(file)) {
      assertThat(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)).isTrue();
    }
    return file;
  }

  private static final byte[] TINY_GIF =
      new byte[] {
        0x47,
        0x49,
        0x46,
        0x38,
        0x39,
        0x61,
        0x01,
        0x00,
        0x01,
        0x00,
        (byte) 0x80,
        0x00,
        0x00,
        (byte) 0xff,
        (byte) 0xff,
        (byte) 0xff,
        0x00,
        0x00,
        0x00,
        0x21,
        (byte) 0xf9,
        0x04,
        0x01,
        0x00,
        0x00,
        0x00,
        0x00,
        0x2c,
        0x00,
        0x00,
        0x00,
        0x00,
        0x01,
        0x00,
        0x01,
        0x00,
        0x00,
        0x02,
        0x02,
        0x44,
        0x01,
        0x00,
        0x3b
      };
}
