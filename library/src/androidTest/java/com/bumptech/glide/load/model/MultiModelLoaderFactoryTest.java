package com.bumptech.glide.load.model;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class MultiModelLoaderFactoryTest {

    @Mock ModelLoaderFactory<String, String> singleFactory;
    @Mock ModelLoader<String, String> modelLoader;
    @Mock MultiModelLoaderFactory.Factory multiModelLoaderFactory;

    private MultiModelLoaderFactory multiFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        multiFactory = new MultiModelLoaderFactory(RuntimeEnvironment.application, multiModelLoaderFactory);
        when(singleFactory.build(anyContext(), eq(multiFactory))).thenReturn(modelLoader);
    }

    @Test
    public void testAppend_addsModelLoaderForModelClass() {
        multiFactory.append(String.class, String.class, singleFactory);

        List<ModelLoader<String, ?>> modelLoaders = multiFactory.build(String.class);
        assertThat(modelLoaders).containsExactly(modelLoader);
    }

    @Test
    public void testAppend_addsModelLoaderForModelAndDataClass() {
        multiFactory.append(String.class, String.class, singleFactory);

        List<ModelLoader<String, String>> modelLoaders = buildModelLoaders(String.class, String.class);
        assertThat(modelLoaders).containsExactly(modelLoader);
    }

    @Test
    public void testPrepend_addsModelLoaderForModelClass() {
        multiFactory.prepend(String.class, String.class, singleFactory);

        List<ModelLoader<String, ?>> modelLoaders = multiFactory.build(String.class);
        assertThat(modelLoaders).containsExactly(modelLoader);
    }

    @Test
    public void testPrepend_addsModelLoaderForModelAndDataClass() {
        multiFactory.prepend(String.class, String.class, singleFactory);

        List<ModelLoader<String, String>> modelLoaders = buildModelLoaders(String.class, String.class);
        assertThat(modelLoaders).containsExactly(modelLoader);
    }

    @Test
    public void testReplace_addsModelLoaderForModelClass() {
        multiFactory.replace(String.class, String.class, singleFactory);

        List<ModelLoader<String, ?>> modelLoaders = multiFactory.build(String.class);
        assertThat(modelLoaders).containsExactly(modelLoader);
    }

    @Test
    public void testReplace_addsModelLoaderForModelAndDataClasses() {
        multiFactory.replace(String.class, String.class, singleFactory);

        List<ModelLoader<String, String>> modelLoaders = buildModelLoaders(String.class, String.class);
        assertThat(modelLoaders).containsExactly(modelLoader);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReplace_returnsPreviouslyRegisteredFactories_withModelAndDataClasses() {
        ModelLoaderFactory<String, String> firstOtherFactory = mock(ModelLoaderFactory.class);
        ModelLoaderFactory<String, String> secondOtherFactory = mock(ModelLoaderFactory.class);
        multiFactory.append(String.class, String.class, firstOtherFactory);
        multiFactory.append(String.class, String.class, secondOtherFactory);

        List<ModelLoaderFactory<String, String>> removed =
                multiFactory.replace(String.class, String.class, singleFactory);
        assertThat(removed).containsExactly(firstOtherFactory, secondOtherFactory);
    }

    @Test
    public void testReplace_removesPreviouslyRegisteredFactories_withModelAndDataClasses() {
        appendFactoryFor(String.class, String.class);
        appendFactoryFor(String.class, String.class);

        multiFactory.replace(String.class, String.class, singleFactory);

        List<ModelLoader<String, ?>> modelLoaders = multiFactory.build(String.class);
        assertThat(modelLoaders).containsExactly(modelLoader);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemove_returnsPreviouslyRegisteredFactories_withModelAndDataClasses() {
        ModelLoaderFactory<String, String> other = mock(ModelLoaderFactory.class);
        multiFactory.append(String.class, String.class, other);
        multiFactory.append(String.class, String.class, singleFactory);

        List<ModelLoaderFactory<String, String>> removed = multiFactory.remove(String.class, String.class);
        assertThat(removed).containsExactly(singleFactory, other);
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
        multiFactory.append(String.class, String.class, singleFactory);

        List<ModelLoader<String, ?>> modelLoaders = multiFactory.build(String.class);
        assertThat(modelLoaders).containsExactly(otherLoader, modelLoader);
    }

    @Test
    public void testBuild_withModelClass_returnsMultipleModelLoaders_ofGivenModelClassWithDifferentDataClasses() {
        ModelLoader<String, Integer> otherLoader = appendFactoryFor(String.class, Integer.class);
        multiFactory.append(String.class, String.class, singleFactory);

        List<ModelLoader<String, ?>> modelLoaders = multiFactory.build(String.class);
        assertThat(modelLoaders).containsExactly(otherLoader, modelLoader);
    }

    @Test
    public void testBuild_withModelClass_excludesModelLoadersForOtherModelClasses() {
        multiFactory.append(String.class, String.class, singleFactory);
        List<ModelLoader<Integer, ?>> modelLoaders = multiFactory.build(Integer.class);
        assertThat(modelLoaders).doesNotContain(modelLoader);
    }

    @Test
    public void testBuild_withModelAndDataClasses_returnsMultipleModelLoaders_ofGivenModelAndDataClasses() {
        ModelLoader<String, String> otherLoader = appendFactoryFor(String.class, String.class);
        multiFactory.append(String.class, String.class, singleFactory);

        List<ModelLoader<String, String>> modelLoaders = buildModelLoaders(String.class, String.class);
        assertThat(modelLoaders).containsExactly(otherLoader, modelLoader);
    }

    @Test
    public void testBuild_withModelAndDataClasses_excludesModelLoadersForOtherDataClasses() {
        multiFactory.append(String.class, String.class, singleFactory);
        List<ModelLoader<String, Integer>> modelLoaders = buildModelLoaders(String.class, Integer.class);
        assertThat(modelLoaders).doesNotContain(modelLoader);
    }

    @Test
    public void testBuild_withModelAndDataClasses_excludesModelLoadersForOtherModelClasses() {
        multiFactory.append(String.class, String.class, singleFactory);
        List<ModelLoader<Integer, String>> modelLoaders = buildModelLoaders(Integer.class, String.class);
        assertThat(modelLoaders).doesNotContain(modelLoader);
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
        ModelLoader<String, Object> subclass = appendFactoryFor(String.class, Object.class);
        List<ModelLoader<Object, Object>> modelLoaders = buildModelLoaders(Object.class, Object.class);
        assertThat(modelLoaders).doesNotContain(subclass);
    }

    @Test
    public void testBuild_withModelAndDataClass_doesNotMatchSubclassesOfDataClass() {
        ModelLoader<Object, String> subclass = appendFactoryFor(Object.class, String.class);
        List<ModelLoader<Object, Object>> modelLoaders = buildModelLoaders(Object.class, Object.class);
        assertThat(modelLoaders).doesNotContain(subclass);
    }

    @Test
    public void testBuild_withModelAndDataClass_doesMatchSuperclassesOfModelClass() {
        ModelLoader<Object, Object> superclass = appendFactoryFor(Object.class, Object.class);
        List<ModelLoader<String, Object>> modelLoaders = buildModelLoaders(String.class, Object.class);
        assertThat(modelLoaders).contains(superclass);
    }

    @Test
    public void testBuild_withModelAndDataClass_matchesSuperclassesOfDataClass() {
        ModelLoader<Object, Object> superclass = appendFactoryFor(Object.class, Object.class);
        List<ModelLoader<Object, String>> modelLoaders = buildModelLoaders(Object.class, String.class);
        assertThat(modelLoaders).containsExactly(superclass);
    }

    @Test
    public void testBuild_withModelAndDataClass_matchesSuperclassOfModelAndDataClass() {
        ModelLoader<Object, Object> superclass = appendFactoryFor(Object.class, Object.class);
        List<ModelLoader<String, String>> modelLoaders = buildModelLoaders(String.class, String.class);
        assertThat(modelLoaders).contains(superclass);
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

    @SuppressWarnings("unchecked")
    private <X, Y> List<ModelLoader<X, Y>> buildModelLoaders(Class<X> modelClass, Class<Y> dataClass) {
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        multiFactory.build(modelClass, dataClass);
        verify(multiModelLoaderFactory).build(captor.capture());

        List<ModelLoader> captured = captor.getValue();
        List<ModelLoader<X, Y>> result = new ArrayList<>(captured.size());
        for (ModelLoader modelLoader : captured) {
            result.add(modelLoader);
        }
        return result;
    }

    private <X, Y> ModelLoader<X, Y> appendFactoryFor(Class<X> modelClass, Class<Y> dataClass) {
        return registerFactoryFor(modelClass, dataClass, true /*append*/);
    }

    private <X, Y> ModelLoader<X, Y> prependFactoryFor(Class<X> modelClass, Class<Y> dataClass) {
        return registerFactoryFor(modelClass, dataClass, false /*append*/);
    }

    @SuppressWarnings("unchecked")
    private <X, Y> ModelLoader<X, Y> registerFactoryFor(Class<X> modelClass, Class<Y> dataClass, boolean append) {
        ModelLoaderFactory<X, Y> factory = mock(ModelLoaderFactory.class);
        ModelLoader<X, Y> loader = mock(ModelLoader.class);
        when(factory.build(anyContext(), eq(multiFactory))).thenReturn(loader);
        if (append) {
            multiFactory.append(modelClass, dataClass, factory);
        } else {
            multiFactory.prepend(modelClass, dataClass, factory);
        }
        return loader;
    }

    private static Context anyContext() {
        return any(Context.class);
    }
}