package com.bumptech.glide.annotation.compiler;


import static com.bumptech.glide.annotation.compiler.test.Util.emptyAppModule;
import static com.bumptech.glide.annotation.compiler.test.Util.subpackage;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.Assert.fail;

import com.google.common.truth.Truth;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Checks assertions on {@link com.bumptech.glide.annotation.GlideExtension}s for methods annotated
 * with {@link com.bumptech.glide.annotation.GlideType}.
 */
// Ignore warnings since most methods use ExpectedException
@SuppressWarnings("ResultOfMethodCallIgnored")
@RunWith(JUnit4.class)
public class InvalidGlideTypeExtensionTest {
  @Rule public final ExpectedException expectedException = ExpectedException.none();

  @Test
  public void compilation_withAnnotatedNonStaticMethod_fails() {
    expectedException.expectMessage("@GlideType methods must be static");
    javac()
        .withProcessors(new GlideAnnotationProcessor())
        .compile(
            emptyAppModule(),
            JavaFileObjects.forSourceLines(
                "Extension",
                "package com.bumptech.glide.test;",
                "import com.bumptech.glide.annotation.GlideExtension;",
                "import com.bumptech.glide.annotation.GlideType;",
                "@GlideExtension",
                "public class Extension {",
                "  private Extension() {}",
                "  @GlideType(Number.class)",
                "  public void doSomething() {}",
                "}"));
  }

  @Test
  public void compilation_withAnnotatedStaticMethod_withoutRequestBuilderArg_fails() {
    expectedException
        .expectMessage(
            "@GlideType methods must take a RequestBuilder object as their first and only"
                + " parameter, but given multiple for:"
                + " com.bumptech.glide.test.Extension#doSomething()");
    javac()
        .withProcessors(new GlideAnnotationProcessor())
        .compile(
            emptyAppModule(),
            JavaFileObjects.forSourceLines(
                "Extension",
                "package com.bumptech.glide.test;",
                "import com.bumptech.glide.annotation.GlideExtension;",
                "import com.bumptech.glide.annotation.GlideType;",
                "@GlideExtension",
                "public class Extension {",
                "  private Extension() {}",
                "  @GlideType(Number.class)",
                "  public static void doSomething() {}",
                "}"));
  }

  @Test
  public void compilation_withAnnotatedStaticMethod_withRequestBuilderArg_succeeds() {
    Compilation compilation = javac()
        .withProcessors(new GlideAnnotationProcessor())
        .compile(
            emptyAppModule(),
            JavaFileObjects.forSourceLines(
                "Extension",
                "package com.bumptech.glide.test;",
                "import com.bumptech.glide.RequestBuilder;",
                "import com.bumptech.glide.annotation.GlideExtension;",
                "import com.bumptech.glide.annotation.GlideType;",
                "@GlideExtension",
                "public class Extension {",
                "  private Extension() {}",
                "  @GlideType(Number.class)",
                "  public static void type(RequestBuilder<Number> builder) {}",
                "}"));
    assertThat(compilation).succeededWithoutWarnings();
  }

  @Test
  public void compilation_withAnnotatedStaticMethod_withNonRequestBuilderArg_fails() {
    try {
      javac()
          .withProcessors(new GlideAnnotationProcessor())
          .compile(
              emptyAppModule(),
              JavaFileObjects.forSourceLines(
                  "WrongParameterTypeExtension",
                  "package com.bumptech.glide.test;",
                  "import com.bumptech.glide.RequestBuilder;",
                  "import com.bumptech.glide.annotation.GlideExtension;",
                  "import com.bumptech.glide.annotation.GlideType;",
                  "@GlideExtension",
                  "public class WrongParameterTypeExtension {",
                  "  private WrongParameterTypeExtension() {}",
                  "  @GlideType(Number.class)",
                  "  public static void type(Object arg) {}",
                  "}"));
    } catch (RuntimeException e) {
      String message = e.getCause().getMessage();
      Truth.assertThat(message).contains("RequestBuilder object as their first and only parameter");
      Truth.assertThat(message).contains("Object");
      Truth.assertThat(message).contains("WrongParameterTypeExtension");
    }
  }

  @Test
  public void compilation_withAnnotatedStaticMethod_withRequestBuilderArgAndOtherArg_fails() {
    expectedException
        .expectMessage(
            "@GlideType methods must take a RequestBuilder object as their first and only"
                + " parameter, but given multiple for:"
                + " com.bumptech.glide.test.Extension#type("
                + "com.bumptech.glide.RequestBuilder<java.lang.Number>,"
                + "java.lang.Object)");
    javac()
        .withProcessors(new GlideAnnotationProcessor())
        .compile(
            emptyAppModule(),
            JavaFileObjects.forSourceLines(
                "Extension",
                "package com.bumptech.glide.test;",
                "import com.bumptech.glide.RequestBuilder;",
                "import com.bumptech.glide.annotation.GlideExtension;",
                "import com.bumptech.glide.annotation.GlideType;",
                "@GlideExtension",
                "public class Extension {",
                "  private Extension() {}",
                "  @GlideType(Number.class)",
                "  public static void type(RequestBuilder<Number> builder, Object arg2) {}",
                "}"));
  }

  @Test
  public void compilation_withAnnotatedStaticMethod_overridingExistingType_fails()
      throws IOException {
    Compilation compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                emptyAppModule(),
                JavaFileObjects.forSourceLines(
                    "Extension",
                    "package com.bumptech.glide.test;",
                    "import android.graphics.drawable.Drawable;",
                    "import com.bumptech.glide.RequestBuilder;",
                    "import com.bumptech.glide.annotation.GlideExtension;",
                    "import com.bumptech.glide.annotation.GlideType;",
                    "@GlideExtension",
                    "public class Extension {",
                    "  private Extension() {}",
                    "  @GlideType(Drawable.class)",
                    "  public static void asDrawable(RequestBuilder<Drawable> builder) {}",
                    "}"));
    expectedException
        .expectMessage(
            "error: method asDrawable() is already defined in class"
                + " com.bumptech.glide.test.GlideRequests");
    compilation.generatedSourceFile(subpackage("GlideRequests"));
  }

  @Test
  public void compilation_withAnnotatedStaticMethod_returningRequestBuilder_succeeds() {
    Compilation compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                emptyAppModule(),
                JavaFileObjects.forSourceLines(
                    "Extension",
                    "package com.bumptech.glide.test;",
                    "import android.support.annotation.NonNull;",
                    "import com.bumptech.glide.RequestBuilder;",
                    "import com.bumptech.glide.annotation.GlideExtension;",
                    "import com.bumptech.glide.annotation.GlideType;",
                    "@GlideExtension",
                    "public class Extension {",
                    "  private Extension() {}",
                    "  @NonNull",
                    "  @GlideType(Number.class)",
                    "  public static RequestBuilder<Number> asNumber(",
                    "      RequestBuilder<Number> builder) {",
                    "    return builder;",
                    "  }",
                    "}"));
    assertThat(compilation).succeededWithoutWarnings();
  }

  @Test
  public void compilation_withAnnotatedStaticMethod_returningNonRequestBuilder_fails() {
    try {
      javac()
          .withProcessors(new GlideAnnotationProcessor())
          .compile(
              emptyAppModule(),
              JavaFileObjects.forSourceLines(
                  "WrongReturnTypeExtension",
                  "package com.bumptech.glide.test;",
                  "import com.bumptech.glide.RequestBuilder;",
                  "import com.bumptech.glide.annotation.GlideExtension;",
                  "import com.bumptech.glide.annotation.GlideType;",
                  "@GlideExtension",
                  "public class WrongReturnTypeExtension {",
                  "  private WrongReturnTypeExtension() {}",
                  "  @GlideType(Number.class)",
                  "  public static Object asNumber(",
                  "      RequestBuilder<Number> builder) {",
                  "    return new Object();",
                  "  }",
                  "}"));
      fail();
    } catch (RuntimeException e) {
      String message = e.getCause().getMessage();
      Truth.assertThat(message).contains("@GlideType methods should return a RequestBuilder");
      Truth.assertThat(message).contains("Number");
      Truth.assertThat(message).contains("WrongReturnTypeExtension");
    }
  }

  @Test
  public void compilation_withAnnotatedStaticMethod_returningBuilderWithIncorrectType_fails() {
    try {
      javac()
          .withProcessors(new GlideAnnotationProcessor())
          .compile(
              emptyAppModule(),
              JavaFileObjects.forSourceLines(
                  "WrongBuilderTypeExtension",
                  "package com.bumptech.glide.test;",
                  "import com.bumptech.glide.RequestBuilder;",
                  "import com.bumptech.glide.annotation.GlideExtension;",
                  "import com.bumptech.glide.annotation.GlideType;",
                  "@GlideExtension",
                  "public class WrongBuilderTypeExtension {",
                  "  private WrongBuilderTypeExtension() {}",
                  "  @GlideType(Number.class)",
                  "  public static RequestBuilder<Object> asNumber(",
                  "      RequestBuilder<Object> builder) {",
                  "    return builder;",
                  "  }",
                  "}"));
      fail();
    } catch (RuntimeException e) {
      String message = e.getCause().getMessage();
      Truth.assertThat(message)
          .contains("@GlideType methods should return a RequestBuilder<java.lang.Number>");
      Truth.assertThat(message)
          .contains("WrongBuilderTypeExtension");
    }
  }

  @Test
  public void compilation_withAnnotatedStaticMethod_returningBuilder_andMultipleParams_fails() {
    expectedException.expectMessage(
        "@GlideType methods must take a RequestBuilder object as their first and only parameter,"
            + " but given multiple for:"
            + " com.bumptech.glide.test.Extension#asNumber("
            + "com.bumptech.glide.RequestBuilder<java.lang.Number>,java.lang.Object)");
    javac()
        .withProcessors(new GlideAnnotationProcessor())
        .compile(
            emptyAppModule(),
            JavaFileObjects.forSourceLines(
                "Extension",
                "package com.bumptech.glide.test;",
                "import com.bumptech.glide.RequestBuilder;",
                "import com.bumptech.glide.annotation.GlideExtension;",
                "import com.bumptech.glide.annotation.GlideType;",
                "@GlideExtension",
                "public class Extension {",
                "  private Extension() {}",
                "  @GlideType(Number.class)",
                "  public static RequestBuilder<Number> asNumber(",
                "      RequestBuilder<Number> builder, Object arg1) {",
                "    return builder;",
                "  }",
                "}"));
  }

  @Test
  public void compilation_withAnnotatedStaticMethod_returningBuilder_nonBuilderParam_fails() {
    try {
      javac()
          .withProcessors(new GlideAnnotationProcessor())
          .compile(
              emptyAppModule(),
              JavaFileObjects.forSourceLines(
                  "IncorrectParameterExtension",
                  "package com.bumptech.glide.test;",
                  "import com.bumptech.glide.RequestBuilder;",
                  "import com.bumptech.glide.annotation.GlideExtension;",
                  "import com.bumptech.glide.annotation.GlideType;",
                  "@GlideExtension",
                  "public class IncorrectParameterExtension {",
                  "  private IncorrectParameterExtension() {}",
                  "  @GlideType(Number.class)",
                  "  public static RequestBuilder<Number> asNumber(",
                  "      Object arg) {",
                  "    return null;",
                  "  }",
                  "}"));
      fail();
    } catch (RuntimeException e) {
      String message = e.getCause().getMessage();
      Truth.assertThat(message).contains("@GlideType methods must take a RequestBuilder object"
          + " as their first and only parameter");
      Truth.assertThat(message).contains("Object");
      Truth.assertThat(message).contains("IncorrectParameterExtension");
    }
  }

  @Test
  public void compilation_withAnnotatedStaticMethod_returningRequestBuilder_missingNonNull_warns() {
    Compilation compilation =
        javac()
            .withProcessors(new GlideAnnotationProcessor())
            .compile(
                emptyAppModule(),
                JavaFileObjects.forSourceLines(
                    "Extension",
                    "package com.bumptech.glide.test;",
                    "import com.bumptech.glide.RequestBuilder;",
                    "import com.bumptech.glide.annotation.GlideExtension;",
                    "import com.bumptech.glide.annotation.GlideType;",
                    "@GlideExtension",
                    "public class Extension {",
                    "  private Extension() {}",
                    "  @GlideType(Number.class)",
                    "  public static RequestBuilder<Number> asNumber(",
                    "      RequestBuilder<Number> builder) {",
                    "    return builder;",
                    "  }",
                    "}"));
    assertThat(compilation).succeeded();
    assertThat(compilation).hadWarningCount(1);
    assertThat(compilation).hadWarningContaining("android.support.annotation.NonNull");
    assertThat(compilation).hadWarningContaining("com.bumptech.glide.test.Extension#asNumber");
  }
}
