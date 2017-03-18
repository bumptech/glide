package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.compiler.GlideAnnotationProcessor.DEBUG;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

/**
 * Utilities for writing classes and logging.
 */
final class ProcessorUtil {
  private static final String GLIDE_MODULE_PACKAGE_NAME = "com.bumptech.glide.module";
  private static final String ROOT_GLIDE_MODULE_SIMPLE_NAME = "RootGlideModule";
  private static final String CHILD_GLIDE_MODULE_SIMPLE_NAME = "ChildGlideModule";
  private static final String ROOT_GLIDE_MODULE_QUALIFIED_NAME =
      GLIDE_MODULE_PACKAGE_NAME + "." + ROOT_GLIDE_MODULE_SIMPLE_NAME;
  private static final String CHILD_GLIDE_MODULE_QUALIFIED_NAME =
      GLIDE_MODULE_PACKAGE_NAME + "." + CHILD_GLIDE_MODULE_SIMPLE_NAME;

  private final ProcessingEnvironment processingEnv;
  private final TypeElement rootGlideModuleType;
  private final TypeElement childGlideModuleType;
  private int round;

  ProcessorUtil(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;

    rootGlideModuleType =
        processingEnv.getElementUtils().getTypeElement(ROOT_GLIDE_MODULE_QUALIFIED_NAME);
    childGlideModuleType =
        processingEnv.getElementUtils().getTypeElement(CHILD_GLIDE_MODULE_QUALIFIED_NAME);
  }

  void process() {
    round++;
  }

  boolean isRootGlideModule(TypeElement element) {
    return processingEnv.getTypeUtils().isAssignable(element.asType(),
        rootGlideModuleType.asType());
  }

  boolean isChildGlideModule(TypeElement element) {
    return processingEnv.getTypeUtils().isAssignable(element.asType(),
        childGlideModuleType.asType());
  }

  void writeClass(String packageName, TypeSpec clazz) {
    try {
      debugLog("Writing class:\n" + clazz);
      JavaFile.builder(packageName, clazz).build().writeTo(processingEnv.getFiler());
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  List<TypeElement> getElementsFor(
      Class<? extends Annotation> clazz, RoundEnvironment env) {
    Collection<? extends Element> annotatedElements = env.getElementsAnnotatedWith(clazz);
    return ElementFilter.typesIn(annotatedElements);
  }

  void debugLog(String toLog) {
    if (DEBUG) {
      infoLog(toLog);
    }
  }

  void infoLog(String toLog) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "[" + round + "] " + toLog);
  }

}
