package com.bumptech.glide.load.engine;

import static com.google.common.truth.Truth.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GlideExceptionTest {

  @Test
  public void printStackTrace_withNullCause_printsNull() {
    GlideException exception = new GlideException("message", (Throwable) null);

    StringWriter output = new StringWriter();
    exception.printStackTrace(new PrintWriter(output));

    assertThat(output.toString()).contains("Cause (1 of 1): null");
  }
}
