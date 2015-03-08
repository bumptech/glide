package com.bumptech.glide.load.model;

import com.google.common.testing.EqualsTester;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BasicHeadersTest {

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            new BasicHeaders.Builder().addHeader("key", "value").build(),
            new BasicHeaders.Builder().addHeader("key", "value").build()
        )
        .addEqualityGroup(
            new BasicHeaders.Builder().addHeader("otherKey", "value").build(),
            new BasicHeaders.Builder().addHeader("otherKey", "value").build()
        )
        .addEqualityGroup(
            new BasicHeaders.Builder().addHeader("key", "otherValue").build(),
            new BasicHeaders.Builder().addHeader("key", "otherValue").build()
        )
        .addEqualityGroup(
            new BasicHeaders.Builder()
                .addHeader("key", "value")
                .addHeader("otherKey", "otherValue")
                .build(),
            new BasicHeaders.Builder()
                .addHeader("key", "value")
                .addHeader("otherKey", "otherValue")
                .build(),
            new BasicHeaders.Builder()
                .addHeader("otherKey", "otherValue")
                .addHeader("key", "value")
                .build()
        )
        .addEqualityGroup(
            new BasicHeaders.Builder()
                .addHeader("key", "value")
                .addHeader("key", "otherValue")
                .build(),
            new BasicHeaders.Builder()
                .addHeader("key", "value")
                .addHeader("key", "otherValue")
                .build()
        ).testEquals();
  }

}