package com.bumptech.glide.load.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;
import com.bumptech.glide.Registry.NoModelLoaderAvailableException;
import com.bumptech.glide.util.pool.FactoryPools;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ModelLoaderRegistryTest {
  private static final String MOCK_MODEL_LOADER_NAME = "MockModelLoader";

  private ModelLoaderRegistry registry;

  @Before
  public void setUp() {
    registry = new ModelLoaderRegistry(FactoryPools.<Throwable>threadSafeList());
  }

  @Test
  public void getModelLoaders_withNoRegisteredModelLoader_throws() {
    final Object model = new Object();
    NoModelLoaderAvailableException thrown =
        assertThrows(
            NoModelLoaderAvailableException.class,
            new ThrowingRunnable() {
              @Override
              public void run() {
                registry.getModelLoaders(model);
              }
            });

    assertThat(thrown)
        .hasMessageThat()
        .contains(
            "Failed to find any ModelLoaders registered for model class: " + model.getClass());
  }

  @Test
  public void getModelLoaders_withRegisteredModelLoader_thatDoesNotHandleModelInstance_throws() {
    final Object model = new Object();
    final ModelLoader<Object, Object> modelLoader = mockModelLoader();
    when(modelLoader.handles(model)).thenReturn(false);
    appendModelLoader(modelLoader);

    NoModelLoaderAvailableException thrown =
        assertThrows(
            NoModelLoaderAvailableException.class,
            new ThrowingRunnable() {
              @Override
              public void run() {
                registry.getModelLoaders(model);
              }
            });

    assertThat(thrown)
        .hasMessageThat()
        .contains(
            "Found ModelLoaders for model class: [MockModelLoader], but none that handle this"
                + " specific model instance: java.lang.Object");
  }

  @Test
  public void getModelLoaders_withRegisteredModelLoader_handlesModel_returnsModelLoader() {
    final Object model = new Object();
    final ModelLoader<Object, Object> modelLoader = mockModelLoader();
    when(modelLoader.handles(model)).thenReturn(true);
    appendModelLoader(modelLoader);

    assertThat(registry.getModelLoaders(model)).containsExactly(modelLoader);
  }

  @Test
  public void
      getModelLoaders_withRegisteredModelLoaders_onlyOneHandlesModel_returnsHandlingModelLoader() {
    final Object model = new Object();

    ModelLoader<Object, Object> handlingModelLoader = mockModelLoader();
    when(handlingModelLoader.handles(model)).thenReturn(true);
    appendModelLoader(handlingModelLoader);

    ModelLoader<Object, Object> notHandlingModelLoader = mockModelLoader();
    when(notHandlingModelLoader.handles(model)).thenReturn(false);
    appendModelLoader(notHandlingModelLoader);

    assertThat(registry.getModelLoaders(model)).containsExactly(handlingModelLoader);
  }

  private void appendModelLoader(final ModelLoader<Object, Object> modelLoader) {
    registry.append(
        Object.class,
        Object.class,
        new ModelLoaderFactory<Object, Object>() {
          @NonNull
          @Override
          public ModelLoader<Object, Object> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return modelLoader;
          }

          @Override
          public void teardown() {}
        });
  }

  @SuppressWarnings("unchecked")
  private static ModelLoader<Object, Object> mockModelLoader() {
    return mock(ModelLoader.class, MOCK_MODEL_LOADER_NAME);
  }
}
