package com.bumptech.glide.load.model;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.testing.EqualsTester;

import com.bumptech.glide.load.model.LazyHeaders.Builder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class LazyHeadersTest {

    @Test
    public void testIncludesEagerHeaders() {
        Map<String, String> headers = new Builder()
            .addHeader("key", "value")
            .build()
            .getHeaders();
        assertThat(headers).containsEntry("key", "value");
        assertThat(headers).hasSize(1);
    }

    @Test
    public void testIncludesLazyHeaders() {
        LazyHeaderFactory factory = mock(LazyHeaderFactory.class);
        when(factory.buildHeader()).thenReturn("value");
        Map<String, String> headers = new Builder()
            .addHeader("key", factory)
            .build()
            .getHeaders();

        assertThat(headers).hasSize(1);
        assertThat(headers).containsEntry("key", "value");
    }

    @Test
    public void testMultipleEagerValuesAreSeparatedByCommas() {
        Map<String, String> headers = new Builder()
            .addHeader("key", "first")
            .addHeader("key", "second")
            .build()
            .getHeaders();

        assertThat(headers).hasSize(1);
        assertThat(headers).containsEntry("key", "first,second");
    }

    @Test
    public void testMultipleLazyValuesAreSeparatedByCommas() {
        LazyHeaderFactory first = mock(LazyHeaderFactory.class);
        when(first.buildHeader()).thenReturn("first");
        LazyHeaderFactory second = mock(LazyHeaderFactory.class);
        when(second.buildHeader()).thenReturn("second");

        Map<String, String> headers = new Builder()
            .addHeader("key", first)
            .addHeader("key", second)
            .build()
            .getHeaders();
        assertThat(headers).hasSize(1);
        assertThat(headers).containsEntry("key", "first,second");
    }

    @Test
    public void testMixedEagerAndLazyValuesAreIncluded() {
        LazyHeaderFactory factory = mock(LazyHeaderFactory.class);
        when(factory.buildHeader()).thenReturn("first");
        Map<String, String> headers = new Builder()
            .addHeader("key", factory)
            .addHeader("key", "second")
            .build()
            .getHeaders();

        assertThat(headers).hasSize(1);
        assertThat(headers).containsEntry("key", "first,second");

        headers = new Builder()
            .addHeader("key", "second")
            .addHeader("key", factory)
            .build()
            .getHeaders();

        assertThat(headers).hasSize(1);
        assertThat(headers).containsEntry("key", "second,first");
    }

    @Test
    public void testCanAddMultipleKeys() {
        LazyHeaderFactory factory = mock(LazyHeaderFactory.class);
        when(factory.buildHeader()).thenReturn("lazy");
        Map<String, String> headers = new Builder()
            .addHeader("first", factory)
            .addHeader("second", "eager")
            .build()
            .getHeaders();

        assertThat(headers).hasSize(2);
        assertThat(headers).containsEntry("first", "lazy");
        assertThat(headers).containsEntry("second", "eager");
    }

    @Test
    public void testUpdatingBuilderAfterBuildingDoesNotModifyOriginalHeaders() {
        Builder builder = new Builder();
        builder.addHeader("key", "firstValue");
        LazyHeaders first = builder.build();

        LazyHeaderFactory factory = mock(LazyHeaderFactory.class);
        when(factory.buildHeader()).thenReturn("otherValue");
        builder.addHeader("key", "secondValue");
        builder.addHeader("otherKey", factory);
        LazyHeaders second = builder.build();

        assertThat(first.getHeaders()).isNotEqualTo(second.getHeaders());

        assertThat(first.getHeaders()).hasSize(1);
        assertThat(first.getHeaders()).containsEntry("key", "firstValue");

        assertThat(second.getHeaders()).hasSize(2);
        assertThat(second.getHeaders()).containsEntry("key", "firstValue,secondValue");
        assertThat(second.getHeaders()).containsEntry("otherKey", "otherValue");
    }

    @Test
    public void testEquals() {
        LazyHeaderFactory firstLazyFactory = mock(LazyHeaderFactory.class);
        LazyHeaderFactory secondLazyFactory = mock(LazyHeaderFactory.class);
        new EqualsTester()
            .addEqualityGroup(
                new Builder().build(),
                new Builder().build()
            )
            .addEqualityGroup(
                new Builder().addHeader("key", "value").build(),
                new Builder().addHeader("key", "value").build()
            )
            .addEqualityGroup(
                new Builder().addHeader("key", "value").addHeader("key", "value").build()
            )
            .addEqualityGroup(
                new Builder().addHeader("key", firstLazyFactory).build(),
                new Builder().addHeader("key", firstLazyFactory).build()
            )
            .addEqualityGroup(
                new Builder()
                    .addHeader("key", firstLazyFactory)
                    .addHeader("key", firstLazyFactory)
                    .build()
            )
            .addEqualityGroup(
                new Builder()
                    .addHeader("firstKey", "value")
                    .addHeader("secondKey", firstLazyFactory)
                    .build(),
                new Builder()
                    .addHeader("secondKey", firstLazyFactory)
                    .addHeader("firstKey", "value")
                    .build()
            )
            .addEqualityGroup(
                new Builder().addHeader("key", "secondValue")
            )
            .addEqualityGroup(
                new Builder().addHeader("secondKey", "value")
            )
            .addEqualityGroup(
                new Builder().addHeader("key", secondLazyFactory)
            )
            .addEqualityGroup(
                new Builder().addHeader("secondKey", firstLazyFactory)
            )
            .addEqualityGroup(
                new Builder()
                    .addHeader("firstKey", "firstValue")
                    .addHeader("secondKey", "secondValue")
                    .build(),
                new Builder()
                    .addHeader("firstKey", "firstValue")
                    .addHeader("secondKey", "secondValue")
                    .build(),
                new Builder()
                    .addHeader("secondKey", "secondValue")
                    .addHeader("firstKey", "firstValue")
                    .build()
            )
            .addEqualityGroup(
                new Builder()
                    .addHeader("firstKey", firstLazyFactory)
                    .addHeader("secondKey", secondLazyFactory)
                    .build(),
                new Builder()
                    .addHeader("firstKey", firstLazyFactory)
                    .addHeader("secondKey", secondLazyFactory)
                    .build(),
                new Builder()
                    .addHeader("secondKey", secondLazyFactory)
                    .addHeader("firstKey", firstLazyFactory)
                    .build()
            )
            .testEquals();
    }
}