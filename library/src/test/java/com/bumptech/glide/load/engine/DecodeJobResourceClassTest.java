package com.bumptech.glide.load.engine;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry.NoResultEncoderAvailableException;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.MockGlideExecutor;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.signature.ObjectKey;
import com.bumptech.glide.util.ByteBufferUtil;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests {@link DecodeJob} {@link Resource#getResourceClass()} lookup under experiment gating. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public final class DecodeJobResourceClassTest {

  private static final int SIZE = 100;

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();
  private final Context context = ApplicationProvider.getApplicationContext();
  @Nullable private ExecutorService executorService;

  @After
  public void tearDown() throws InterruptedException {
    // Drain deferred disk-cache writes cleanly before tearing down the Glide singleton.
    if (executorService != null) {
      executorService.shutdown();
      if (!executorService.awaitTermination(5, SECONDS)) {
        executorService.shutdownNow();
      }
    }
    Glide.tearDown();
  }

  @Test
  public void load_withDefaultExperiment_callsDecodeGet() throws Exception {
    initGlideWithDefaultExperiments();
    BaseAsset decoded = new BaseAsset();
    CountingResource resource = new CountingResource(decoded, BaseAsset.class);
    registerDecoder(() -> resource);

    LoadCapture capture =
        executeRequest(newRequest().diskCacheStrategy(DiskCacheStrategy.NONE).dontTransform());

    assertThat(capture.failure).isNull();
    assertThat(capture.resource).isSameInstanceAs(decoded);
    assertThat(resource.getCount.get()).isEqualTo(2);
  }

  @Test
  public void load_whenExperimentEnabledThenDisabled_callsDecodeGet() throws Exception {
    initGlide(
        newBuilder()
            .experimentalSetUseResourceClassForDecoding(true)
            .experimentalSetUseResourceClassForDecoding(false));
    BaseAsset decoded = new BaseAsset();
    CountingResource resource = new CountingResource(decoded, BaseAsset.class);
    registerDecoder(() -> resource);

    LoadCapture capture =
        executeRequest(newRequest().diskCacheStrategy(DiskCacheStrategy.NONE).dontTransform());

    assertThat(capture.failure).isNull();
    assertThat(capture.resource).isSameInstanceAs(decoded);
    assertThat(resource.getCount.get()).isEqualTo(2);
  }

  @Test
  public void load_withNoTransformation_whenExperimentEnabled_skipsDecodeGet() throws Exception {
    initGlide(/* useResourceClass= */ true);
    BaseAsset decoded = new BaseAsset();
    CountingResource resource = new CountingResource(decoded, BaseAsset.class);
    registerDecoder(() -> resource);

    LoadCapture capture =
        executeRequest(newRequest().diskCacheStrategy(DiskCacheStrategy.NONE).dontTransform());

    assertThat(capture.failure).isNull();
    assertThat(capture.resource).isSameInstanceAs(decoded);
    assertThat(resource.getCount.get()).isEqualTo(1);
  }

  @Test
  public void load_withNoTransformation_whenExperimentDisabled_callsDecodeGet() throws Exception {
    initGlide(/* useResourceClass= */ false);
    BaseAsset decoded = new BaseAsset();
    CountingResource resource = new CountingResource(decoded, BaseAsset.class);
    registerDecoder(() -> resource);

    LoadCapture capture =
        executeRequest(newRequest().diskCacheStrategy(DiskCacheStrategy.NONE).dontTransform());

    assertThat(capture.failure).isNull();
    assertThat(capture.resource).isSameInstanceAs(decoded);
    assertThat(resource.getCount.get()).isEqualTo(2);
  }

  @Test
  public void transformationLookup_whenExperimentEnabled_usesDeclaredClass() throws Exception {
    CountingTransformation<BaseAsset> declaredTransform = new CountingTransformation<>(1);
    CountingTransformation<SubAsset> subclassTransform = new CountingTransformation<>(2);

    CountingResource resource =
        runTransformationLookup(/* useResourceClass= */ true, declaredTransform, subclassTransform);

    assertThat(declaredTransform.transformCount.get()).isEqualTo(1);
    assertThat(subclassTransform.transformCount.get()).isEqualTo(0);
    assertThat(resource.getCount.get()).isEqualTo(1);
  }

  @Test
  public void transformationLookup_whenExperimentDisabled_usesRuntimeClass() throws Exception {
    CountingTransformation<BaseAsset> declaredTransform = new CountingTransformation<>(1);
    CountingTransformation<SubAsset> subclassTransform = new CountingTransformation<>(2);

    CountingResource resource =
        runTransformationLookup(
            /* useResourceClass= */ false, declaredTransform, subclassTransform);

    assertThat(declaredTransform.transformCount.get()).isEqualTo(0);
    assertThat(subclassTransform.transformCount.get()).isEqualTo(1);
    assertThat(resource.getCount.get()).isEqualTo(2);
  }

  @Test
  public void resourceCacheKey_whenExperimentEnabled_hitsResourceDiskCache() throws Exception {
    List<CountingResource> decoded = new CopyOnWriteArrayList<>();

    LoadCapture second = runTwoResourceCacheLoads(/* useResourceClass= */ true, decoded);

    assertThat(second.failure).isNull();
    assertThat(second.dataSource).isEqualTo(DataSource.RESOURCE_DISK_CACHE);
    assertThat(decoded).hasSize(2);
    assertThat(decoded.get(0).getCount.get()).isEqualTo(1);
    assertThat(decoded.get(1).getCount.get()).isEqualTo(1);
  }

  @Test
  public void resourceCacheKey_whenExperimentDisabled_missesWhenClassesDiffer() throws Exception {
    List<CountingResource> decoded = new CopyOnWriteArrayList<>();

    LoadCapture second = runTwoResourceCacheLoads(/* useResourceClass= */ false, decoded);

    assertThat(second.failure).isNull();
    assertThat(second.dataSource).isEqualTo(DataSource.LOCAL);
    assertThat(decoded).hasSize(2);
    assertThat(decoded.get(0).getCount.get()).isEqualTo(2);
    assertThat(decoded.get(1).getCount.get()).isEqualTo(2);
  }

  @Test
  public void load_whenEncoderMissingAndExperimentEnabled_reportsDeclaredClass() throws Exception {
    CountingResource resource = new CountingResource(new SubAsset(), BaseAsset.class);

    Throwable rootCause =
        runMissingEncoderLoad(
            /* useResourceClass= */ true, resource, new CountingTransformation<>(4));

    assertThat(rootCause).isInstanceOf(NoResultEncoderAvailableException.class);
    assertThat(rootCause).hasMessageThat().contains(BaseAsset.class.getName());
    assertThat(resource.getCount.get()).isEqualTo(0);
  }

  @Test
  public void load_whenEncoderMissingAndExperimentDisabled_reportsRuntimeClass() throws Exception {
    CountingResource resource = new CountingResource(new SubAsset(), BaseAsset.class);

    Throwable rootCause =
        runMissingEncoderLoad(
            /* useResourceClass= */ false, resource, new CountingTransformation<>(4));

    assertThat(rootCause).isInstanceOf(NoResultEncoderAvailableException.class);
    assertThat(rootCause).hasMessageThat().contains(SubAsset.class.getName());
    assertThat(resource.getCount.get()).isEqualTo(2);
  }

  @Test
  public void load_whenEncoderMissingAfterTransform_reportsTransformedDeclaredClass()
      throws Exception {
    CountingResource decoded = new CountingResource(new SubAsset(), BaseAsset.class);
    CountingResource transformed =
        new CountingResource(new TransformedAsset(), TransformedAsset.class);

    Throwable rootCause =
        runMissingEncoderLoad(
            /* useResourceClass= */ true, decoded, new CountingTransformation<>(6, transformed));

    assertThat(rootCause).isInstanceOf(NoResultEncoderAvailableException.class);
    assertThat(rootCause).hasMessageThat().contains(TransformedAsset.class.getName());
    assertThat(decoded.getCount.get()).isEqualTo(0);
    assertThat(transformed.getCount.get()).isEqualTo(0);
  }

  @Test
  public void load_whenGetResourceClassReturnsNull_fallsBackToGet() throws Exception {
    initGlide(/* useResourceClass= */ true);
    BaseAsset decoded = new BaseAsset();
    CountingResource nullClassResource = new CountingResource(decoded, /* declaredClass= */ null);
    CountingResource replacementResource = new CountingResource(decoded, BaseAsset.class);
    registerDecoder(() -> nullClassResource);

    // Replace the null-class resource so Registry.isResourceEncoderAvailable does not throw NPE.
    LoadCapture capture =
        executeRequest(
            newRequest()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .optionalTransform(
                    BaseAsset.class, new CountingTransformation<>(5, replacementResource)));

    assertThat(capture.failure).isNull();
    assertThat(capture.resource).isSameInstanceAs(decoded);
    assertThat(nullClassResource.getCount.get()).isEqualTo(1);
    assertThat(replacementResource.getCount.get()).isEqualTo(1);
  }

  private CountingResource runTransformationLookup(
      boolean useResourceClass,
      CountingTransformation<BaseAsset> declaredTransform,
      CountingTransformation<SubAsset> subclassTransform)
      throws Exception {
    initGlide(useResourceClass);
    CountingResource resource = new CountingResource(new SubAsset(), BaseAsset.class);
    registerDecoder(() -> resource);

    LoadCapture capture =
        executeRequest(
            newRequest()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .optionalTransform(BaseAsset.class, declaredTransform)
                .optionalTransform(SubAsset.class, subclassTransform));
    assertThat(capture.failure).isNull();
    return resource;
  }

  private LoadCapture runTwoResourceCacheLoads(
      boolean useResourceClass, List<CountingResource> decoded) throws Exception {
    initGlide(useResourceClass);
    registerDecoder(() -> {
      CountingResource resource = new CountingResource(new SubAsset(), BaseAsset.class);
      decoded.add(resource);
      return resource;
    });
    Glide.get(context).getRegistry().prepend(BaseAsset.class, new StubTransformedEncoder());
    // ResourceCacheGenerator queries declared BaseAsset.class; when enabled, write uses
    // BaseAsset.class (hit), whereas disabled writes runtime SubAsset.class (miss).
    RequestBuilder<BaseAsset> request =
        newRequest()
            .signature(new ObjectKey("cache"))
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .optionalTransform(BaseAsset.class, new CountingTransformation<>(3));

    LoadCapture first = executeRequest(request);
    assertThat(first.failure).isNull();
    assertThat(first.dataSource).isEqualTo(DataSource.LOCAL);
    Glide.with(context).clear(first);
    return executeRequest(request);
  }

  private Throwable runMissingEncoderLoad(
      boolean useResourceClass,
      CountingResource resource,
      CountingTransformation<BaseAsset> transformation)
      throws Exception {
    initGlide(useResourceClass);
    registerDecoder(() -> resource);

    LoadCapture result =
        executeRequest(
            newRequest()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .optionalTransform(BaseAsset.class, transformation));
    assertThat(result.failure).isNotNull();
    assertThat(result.failure.getRootCauses()).isNotEmpty();
    return result.failure.getRootCauses().get(0);
  }

  private void initGlide(boolean useResourceClass) throws IOException {
    initGlide(newBuilder().experimentalSetUseResourceClassForDecoding(useResourceClass));
  }

  private void initGlide(GlideBuilder builder) {
    Glide.init(context, builder);
  }

  private void initGlideWithDefaultExperiments() throws IOException {
    initGlide(newBuilder());
  }

  private GlideBuilder newBuilder() throws IOException {
    // Share one single-thread executor so load 1 finishes disk encoding before load 2 starts, and
    // suppress stderr noise from DecodeJob rethrowing expected NoResultEncoderAvailableException.
    File cacheDir = temporaryFolder.newFolder();
    executorService =
        Executors.newSingleThreadExecutor(runnable -> {
          Thread thread = new Thread(runnable);
          thread.setUncaughtExceptionHandler((t, e) -> {});
          return thread;
        });
    GlideExecutor executor = MockGlideExecutor.newTestExecutor(executorService);
    return new GlideBuilder()
        .setSourceExecutor(executor)
        .setDiskCacheExecutor(executor)
        .setDiskCache(() -> DiskLruCacheWrapper.create(cacheDir, 10L * 1024L * 1024L));
  }

  private static byte[] newPayload() {
    return new byte[] {1, 2, 3, 4};
  }

  private RequestBuilder<BaseAsset> newRequest() {
    return Glide.with(context).as(BaseAsset.class).load(newPayload()).skipMemoryCache(true);
  }

  private void registerDecoder(ResourceFactory factory) {
    Glide.get(context)
        .getRegistry()
        .prepend(
            ByteBuffer.class,
            BaseAsset.class,
            new ResourceDecoder<ByteBuffer, BaseAsset>() {
              @Override
              public boolean handles(@NonNull ByteBuffer source, @NonNull Options options) {
                return true;
              }

              @Override
              public Resource<BaseAsset> decode(
                  @NonNull ByteBuffer source, int width, int height, @NonNull Options options) {
                return factory.create();
              }
            });
  }

  private static LoadCapture executeRequest(RequestBuilder<BaseAsset> request)
      throws InterruptedException {
    LoadCapture capture = new LoadCapture();
    request.into(capture, capture, Runnable::run);
    assertThat(capture.latch.await(5, SECONDS)).isTrue();
    return capture;
  }

  private interface ResourceFactory {
    Resource<BaseAsset> create();
  }

  private static final class LoadCapture extends CustomTarget<BaseAsset>
      implements RequestListener<BaseAsset> {
    final CountDownLatch latch = new CountDownLatch(1);
    @Nullable BaseAsset resource;
    @Nullable DataSource dataSource;
    @Nullable GlideException failure;

    LoadCapture() {
      super(SIZE, SIZE);
    }

    @Override
    public boolean onLoadFailed(
        @Nullable GlideException e,
        Object model,
        Target<BaseAsset> target,
        boolean isFirstResource) {
      failure = e;
      latch.countDown();
      return false;
    }

    @Override
    public boolean onResourceReady(
        BaseAsset resource,
        Object model,
        Target<BaseAsset> target,
        DataSource dataSource,
        boolean isFirstResource) {
      this.dataSource = dataSource;
      return false;
    }

    @Override
    public void onResourceReady(
        @NonNull BaseAsset resource, @Nullable Transition<? super BaseAsset> transition) {
      this.resource = resource;
      latch.countDown();
    }

    @Override
    public void onLoadCleared(@Nullable Drawable placeholder) {}
  }

  private static class BaseAsset {}

  private static final class SubAsset extends BaseAsset {}

  private static final class TransformedAsset extends BaseAsset {}

  private static final class CountingResource implements Resource<BaseAsset> {
    private final BaseAsset value;
    @Nullable private final Class<? extends BaseAsset> declaredClass;
    final AtomicInteger getCount = new AtomicInteger();

    CountingResource(BaseAsset value, @Nullable Class<? extends BaseAsset> declaredClass) {
      this.value = value;
      this.declaredClass = declaredClass;
    }

    // Intentionally omits @NonNull to test defensive null handling in getResourceSubClass.
    @Override
    @SuppressWarnings("unchecked")
    public Class<BaseAsset> getResourceClass() {
      return (Class<BaseAsset>) declaredClass;
    }

    @NonNull
    @Override
    public BaseAsset get() {
      getCount.incrementAndGet();
      return value;
    }

    @Override
    public int getSize() {
      return 1;
    }

    @Override
    public void recycle() {}
  }

  private static final class StubTransformedEncoder implements ResourceEncoder<BaseAsset> {
    @NonNull
    @Override
    public EncodeStrategy getEncodeStrategy(@NonNull Options options) {
      return EncodeStrategy.TRANSFORMED;
    }

    @Override
    public boolean encode(
        @NonNull Resource<BaseAsset> data, @NonNull File file, @NonNull Options options) {
      try {
        ByteBufferUtil.toFile(ByteBuffer.wrap(newPayload()), file);
        return true;
      } catch (IOException e) {
        return false;
      }
    }
  }

  private static final class CountingTransformation<T> implements Transformation<T> {
    private final byte keyByte;
    @Nullable private final Resource<T> replacement;
    final AtomicInteger transformCount = new AtomicInteger();

    CountingTransformation(int keyByte) {
      this(keyByte, /* replacement= */ null);
    }

    CountingTransformation(int keyByte, @Nullable Resource<T> replacement) {
      this.keyByte = (byte) keyByte;
      this.replacement = replacement;
    }

    @NonNull
    @Override
    public Resource<T> transform(
        @NonNull Context context, @NonNull Resource<T> resource, int outWidth, int outHeight) {
      transformCount.incrementAndGet();
      return replacement != null ? replacement : resource;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
      messageDigest.update(keyByte);
    }
  }
}
