package com.bumptech.glide.load.model;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.core.util.Pools.Pool;
import com.bumptech.glide.Registry.NoModelLoaderAvailableException;
import com.bumptech.glide.tests.Util;
import com.bumptech.glide.util.pool.FactoryPools;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// containsExactly produces a spurious warning.
@SuppressWarnings("ResultOfMethodCallIgnored")
@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class MultiModelLoaderFactoryTest {
  @Mock private ModelLoaderFactory<String, String> firstFactory;
  @Mock private ModelLoader<String, String> firstModelLoader;
  @Mock private MultiModelLoaderFactory.Factory multiModelLoaderFactory;
  @Mock private ModelLoaderFactory<String, String> secondFactory;
  @Mock private ModelLoader<String, String> secondModelLoader;

  private Pool<List<Throwable>> throwableListPool;
  private MultiModelLoaderFactory multiFactory;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    throwableListPool = FactoryPools.threadSafeList();

    multiFactory = new MultiModelLoaderFactory(throwableListPool, multiModelLoaderFactory);
    when(firstFactory.build(eq(multiFactory))).thenReturn(firstModelLoader);
    when(secondFactory.build(eq(multiFactory))).thenReturn(secondModelLoader);
  }

  @Test
  public void testAppend_addsModelLoaderForModelClass() {
    multiFactory.append(String.class, String.class, firstFactory);

    List<ModelLoader<String, ?>> modelLoaders = multiFactory.build(String.class);
    assertThat(modelLoaders).containsExactly(firstModelLoader);
  }

  @Test
  public void testAppend_addsModelLoaderForModelAndDataClass() {
    multiFactory.append(String.class, String.class, firstFactory);

    ModelLoader<String, String> modelLoader = multiFactory.build(String.class, String.class);
    assertThat(modelLoader).isEqualTo(firstModelLoader);
  }

  @Test
  public void testPrepend_addsModelLoaderForModelClass() {
    multiFactory.prepend(String.class, String.class, firstFactory);

    List<ModelLoader<String, ?>> modelLoaders = multiFactory.build(String.class);
    assertThat(modelLoaders).containsExactly(firstModelLoader);
  }

  @Test
  public void testPrepend_addsModelLoaderForModelAndDataClass() {
    multiFactory.prepend(String.class, String.class, firstFactory);

    ModelLoader<String, String> modelLoader = multiFactory.build(String.class, String.class);
    assertThat(modelLoader).isEqualTo(firstModelLoader);
  }

  @Test
  public void testReplace_addsModelLoaderForModelClass() {
    multiFactory.replace(String.class, String.class, firstFactory);

    List<ModelLoader<String, ?>> modelLoaders = multiFactory.build(String.class);
    assertThat(modelLoaders).containsExactly(firstModelLoader);
  }

  @Test
  public void testReplace_addsModelLoaderForModelAndDataClasses() {
    multiFactory.replace(String.class, String.class, firstFactory);

    ModelLoader<String, String> modelLoader = multiFactory.build(String.class, String.class);
    assertThat(modelLoader).isEqualTo(firstModelLoader);
  }

  @Test
  public void testReplace_returnsPreviouslyRegisteredFactories_withModelAndDataClasses() {
    ModelLoaderFactory<String, String> firstOtherFactory = mockFactory();
    ModelLoaderFactory<String, String> secondOtherFactory = mockFactory();
    multiFactory.append(String.class, String.class, firstOtherFactory);
    multiFactory.append(String.class, String.class, secondOtherFactory);

    List<ModelLoaderFactory<? extends String, ? extends String>> removed =
        multiFactory.replace(String.class, String.class, firstFactory);
    assertThat(removed).containsExactly(firstOtherFactory, secondOtherFactory);
  }

  @Test
  public void testReplace_removesPreviouslyRegisteredFactories_withModelAndDataClasses() {
    appendFactoryFor(String.class, String.class);
    appendFactoryFor(String.class, String.class);

    multiFactory.replace(String.class, String.class, firstFactory);

    List<ModelLoader<String, ?>> modelLoaders = multiFactory.build(String.class);
    assertThat(modelLoaders).containsExactly(firstModelLoader);
  }

  @Test
  public void testRemove_returnsPreviouslyRegisteredFactories_withModelAndDataClasses() {
    ModelLoaderFactory<String, String> other = mockFactory();
    multiFactory.append(String.class, String.class, other);
    multiFactory.append(String.class, String.class, firstFactory);

    List<ModelLoaderFactory<? extends String, ? extends String>> removed =
        multiFactory.remove(String.class, String.class);
    assertThat(removed).containsExactly(firstFactory, other);
  }

  @Test
  public void testRemove_removesPreviouslyRegisteredFactories_withModelAndDataClasses() {
    appendFactoryFor(String.class, String.class);
    appendFactoryFor(String.class, String.class);

    multiFactory.remove(String.class, String.class);

    List<ModelLoader<String, ?>> modelLoaders = multiFactory.build(String.class);
    assertThat(modelLoaders).isEmpty();
  }

  @Test
  public void testBuild_withModelClass_returnsMultipleModelLoaders_ofGivenModelAndDataClasses() {
    ModelLoader<String, String> otherLoader = appendFactoryFor(String.class, String.class);
    multiFactory.append(String.class, String.class, firstFactory);

    List<ModelLoader<String, ?>> modelLoaders = multiFactory.build(String.class);
    assertThat(modelLoaders).containsExactly(otherLoader, firstModelLoader);
  }

  @Test
  public void
      testBuild_withModelClass_returnsMultipleModelLoaders_ofGivenModelClassWithDifferentDataClasses() {
    ModelLoader<String, Integer> otherLoader = appendFactoryFor(String.class, Integer.class);
    multiFactory.append(String.class, String.class, firstFactory);

    List<ModelLoader<String, ?>> modelLoaders = multiFactory.build(String.class);
    assertThat(modelLoaders).containsExactly(otherLoader, firstModelLoader);
  }

  @SuppressWarnings("TruthIncompatibleType")
  @Test
  public void testBuild_withModelClass_excludesModelLoadersForOtherModelClasses() {
    multiFactory.append(String.class, String.class, firstFactory);
    List<ModelLoader<Integer, ?>> modelLoaders = multiFactory.build(Integer.class);
    assertThat(modelLoaders)
        .doesNotContain(
            /* expected: ModelLoader<Integer, ?>, actual: ModelLoader<String, String> */ firstModelLoader);
  }

  @Test
  public void
      testBuild_withModelAndDataClasses_returnsMultipleModelLoaders_ofGivenModelAndDataClasses() {
    ModelLoader<String, String> otherLoader = appendFactoryFor(String.class, String.class);
    multiFactory.append(String.class, String.class, firstFactory);

    List<ModelLoader<String, String>> modelLoaders = buildModelLoaders(String.class, String.class);
    assertThat(modelLoaders).containsExactly(otherLoader, firstModelLoader);
  }

  @Test
  public void testBuild_withModelAndDataClasses_excludesModelLoadersForOtherDataClasses() {
    multiFactory.append(String.class, String.class, firstFactory);

    assertThrows(
        NoModelLoaderAvailableException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            multiFactory.build(String.class, Integer.class);
          }
        });
  }

  @Test
  public void testBuild_withModelAndDataClasses_excludesModelLoadersForOtherModelClasses() {
    multiFactory.append(String.class, String.class, firstFactory);

    assertThrows(
        NoModelLoaderAvailableException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            multiFactory.build(Integer.class, String.class);
          }
        });
  }

  @Test
  public void testBuild_withModelClass_doesNotMatchSubclassesOfModelClass() {
    ModelLoader<String, Object> subclass = appendFactoryFor(String.class, Object.class);
    List<ModelLoader<Object, ?>> modelLoaders = multiFactory.build(Object.class);
    assertThat(modelLoaders).doesNotContain(subclass);
  }

  @Test
  public void testBuild_withModelClass_matchesSuperclassesOfModelClass() {
    ModelLoader<Object, Object> superclass = appendFactoryFor(Object.class, Object.class);
    List<ModelLoader<String, ?>> modelLoaders = multiFactory.build(String.class);
    assertThat(modelLoaders).contains(superclass);
  }

  @Test
  public void testBuild_withModelAndDataClass_doesNotMatchSubclassesOfModelClass() {
    appendFactoryFor(String.class, Object.class);

    assertThrows(
        NoModelLoaderAvailableException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            multiFactory.build(Object.class, Object.class);
          }
        });
  }

  @Test
  public void testBuild_withModelAndDataClass_doesNotMatchSubclassesOfDataClass() {
    appendFactoryFor(Object.class, String.class);
    assertThrows(
        NoModelLoaderAvailableException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            multiFactory.build(Object.class, Object.class);
          }
        });
  }

  @Test
  public void testBuild_withModelAndDataClass_doesMatchSuperclassesOfModelClass() {
    ModelLoader<Object, Object> firstSuperClass = appendFactoryFor(Object.class, Object.class);
    ModelLoader<Object, Object> secondSuperClass = appendFactoryFor(Object.class, Object.class);
    List<ModelLoader<String, Object>> modelLoaders = buildModelLoaders(String.class, Object.class);
    assertThat(modelLoaders).containsExactly(firstSuperClass, secondSuperClass);
  }

  @Test
  public void testBuild_withModelAndDataClass_matchesSuperclassesOfDataClass() {
    ModelLoader<Object, Object> firstSuperClass = appendFactoryFor(Object.class, Object.class);
    ModelLoader<Object, Object> secondSuperClass = appendFactoryFor(Object.class, Object.class);
    List<ModelLoader<Object, String>> modelLoaders = buildModelLoaders(Object.class, String.class);
    assertThat(modelLoaders).containsExactly(firstSuperClass, secondSuperClass);
  }

  @Test
  public void testBuild_withModelAndDataClass_matchesSuperclassOfModelAndDataClass() {
    ModelLoader<Object, Object> firstSuperclass = appendFactoryFor(Object.class, Object.class);
    ModelLoader<Object, Object> secondSuperclass = appendFactoryFor(Object.class, Object.class);
    List<ModelLoader<String, String>> modelLoaders = buildModelLoaders(String.class, String.class);
    assertThat(modelLoaders).containsExactly(firstSuperclass, secondSuperclass);
  }

  @Test
  public void testBuild_respectsAppendOrder() {
    ModelLoader<String, String> first = appendFactoryFor(String.class, String.class);
    ModelLoader<String, String> second = appendFactoryFor(String.class, String.class);
    ModelLoader<String, String> third = appendFactoryFor(String.class, String.class);
    List<ModelLoader<String, String>> modelLoaders = buildModelLoaders(String.class, String.class);
    assertThat(modelLoaders).containsExactly(first, second, third).inOrder();
  }

  @Test
  public void testBuild_respectsPrependOrder() {
    ModelLoader<String, String> first = prependFactoryFor(String.class, String.class);
    ModelLoader<String, String> second = prependFactoryFor(String.class, String.class);
    ModelLoader<String, String> third = prependFactoryFor(String.class, String.class);
    List<ModelLoader<String, String>> modelLoaders = buildModelLoaders(String.class, String.class);
    assertThat(modelLoaders).containsExactly(third, second, first).inOrder();
  }

  private <X, Y> List<ModelLoader<X, Y>> buildModelLoaders(
      Class<X> modelClass, Class<Y> dataClass) {
    ArgumentCaptor<List<ModelLoader<X, Y>>> captor = Util.cast(ArgumentCaptor.forClass(List.class));
    multiFactory.build(modelClass, dataClass);
    verify(multiModelLoaderFactory).build(captor.capture(), eq(throwableListPool));

    List<ModelLoader<X, Y>> captured = captor.getValue();
    List<ModelLoader<X, Y>> result = new ArrayList<>(captured.size());
    result.addAll(captured);
    return result;
  }

  private <X, Y> ModelLoader<X, Y> appendFactoryFor(Class<X> modelClass, Class<Y> dataClass) {
    return registerFactoryFor(modelClass, dataClass, true /*append*/);
  }

  private <X, Y> ModelLoader<X, Y> prependFactoryFor(Class<X> modelClass, Class<Y> dataClass) {
    return registerFactoryFor(modelClass, dataClass, false /*append*/);
  }

  private <X, Y> ModelLoader<X, Y> registerFactoryFor(
      Class<X> modelClass, Class<Y> dataClass, boolean append) {
    ModelLoaderFactory<X, Y> factory = mockFactory();
    @SuppressWarnings("unchecked")
    ModelLoader<X, Y> loader = mock(ModelLoader.class);
    when(factory.build(eq(multiFactory))).thenReturn(loader);
    if (append) {
      multiFactory.append(modelClass, dataClass, factory);
    } else {
      multiFactory.prepend(modelClass, dataClass, factory);
    }
    return loader;
  }

  @SuppressWarnings("unchecked")
  private static <X, Y> ModelLoaderFactory<X, Y> mockFactory() {
    return mock(ModelLoaderFactory.class);
  }
}
