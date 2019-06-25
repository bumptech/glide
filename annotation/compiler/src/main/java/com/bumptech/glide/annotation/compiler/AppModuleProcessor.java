package com.bumptech.glide.annotation.compiler;

import com.bumptech.glide.annotation.GlideModule;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

/**
 * Runs the final steps of Glide's annotation process and generates the combined {@code
 * AppGlideModule}, {@code com.bumptech.glide.Glide}, {@code com.bumptech.glide.RequestManager}, and
 * {@code com.bumptech.glide.request.RequestOptions} classes.
 */
final class AppModuleProcessor {
  private static final String COMPILER_PACKAGE_NAME =
      GlideAnnotationProcessor.class.getPackage().getName();

  private final ProcessingEnvironment processingEnv;
  private final ProcessorUtil processorUtil;
  private final List<TypeElement> appGlideModules = new ArrayList<>();
  private final RequestOptionsGenerator requestOptionsGenerator;
  private final RequestManagerGenerator requestManagerGenerator;
  private final AppModuleGenerator appModuleGenerator;
  private final RequestBuilderGenerator requestBuilderGenerator;
  private final RequestManagerFactoryGenerator requestManagerFactoryGenerator;
  private final GlideGenerator glideGenerator;

  AppModuleProcessor(ProcessingEnvironment processingEnv, ProcessorUtil processorUtil) {
    this.processingEnv = processingEnv;
    this.processorUtil = processorUtil;

    appModuleGenerator = new AppModuleGenerator(processingEnv, processorUtil);
    requestOptionsGenerator = new RequestOptionsGenerator(processingEnv, processorUtil);
    requestManagerGenerator = new RequestManagerGenerator(processingEnv, processorUtil);
    requestManagerFactoryGenerator =
        new RequestManagerFactoryGenerator(processingEnv, processorUtil);
    glideGenerator = new GlideGenerator(processingEnv, processorUtil);
    requestBuilderGenerator = new RequestBuilderGenerator(processingEnv, processorUtil);
  }

  void processModules(Set<? extends TypeElement> set, RoundEnvironment env) {
    for (TypeElement element : processorUtil.getElementsFor(GlideModule.class, env)) {
      if (processorUtil.isAppGlideModule(element)) {
        appGlideModules.add(element);
      }
    }

    processorUtil.debugLog("got app modules: " + appGlideModules);

    if (appGlideModules.size() > 1) {
      throw new IllegalStateException(
          "You cannot have more than one AppGlideModule, found: " + appGlideModules);
    }
  }

  boolean maybeWriteAppModule() {
    // appGlideModules is added to in order to catch errors where multiple AppGlideModules may be
    // present for a single application or library. Because we only add to appGlideModules, we use
    // isGeneratedAppGlideModuleWritten to make sure the GeneratedAppGlideModule is written at
    // most once.
    if (appGlideModules.isEmpty()) {
      return false;
    }
    TypeElement appModule = appGlideModules.get(0);
    processorUtil.debugLog("Processing app module: " + appModule);
    // If this package is null, it means there are no classes with this package name. One way this
    // could happen is if we process an annotation and reach this point without writing something
    // to the package. We do not error check here because that shouldn't happen with the
    // current implementation.
    PackageElement glideGenPackage =
        processingEnv.getElementUtils().getPackageElement(COMPILER_PACKAGE_NAME);
    FoundIndexedClassNames indexedClassNames = getIndexedClassNames(glideGenPackage);

    // Write all generated code to the package containing the AppGlideModule. Doing so fixes
    // classpath collisions if more than one Application containing a AppGlideModule is included
    // in a project.
    String generatedCodePackageName = appModule.getEnclosingElement().toString();

    TypeSpec generatedRequestOptions =
        requestOptionsGenerator.generate(generatedCodePackageName, indexedClassNames.extensions);
    writeRequestOptions(generatedCodePackageName, generatedRequestOptions);

    TypeSpec generatedRequestBuilder =
        requestBuilderGenerator.generate(
            generatedCodePackageName, indexedClassNames.extensions, generatedRequestOptions);
    writeRequestBuilder(generatedCodePackageName, generatedRequestBuilder);

    TypeSpec requestManager =
        requestManagerGenerator.generate(
            generatedCodePackageName,
            generatedRequestOptions,
            generatedRequestBuilder,
            indexedClassNames.extensions);
    writeRequestManager(generatedCodePackageName, requestManager);

    TypeSpec requestManagerFactory =
        requestManagerFactoryGenerator.generate(generatedCodePackageName, requestManager);
    writeRequestManagerFactory(requestManagerFactory);

    TypeSpec glide =
        glideGenerator.generate(generatedCodePackageName, getGlideName(appModule), requestManager);
    writeGlide(generatedCodePackageName, glide);

    TypeSpec generatedAppGlideModule =
        appModuleGenerator.generate(appModule, indexedClassNames.glideModules);
    writeAppModule(generatedAppGlideModule);

    processorUtil.infoLog("Wrote GeneratedAppGlideModule with: " + indexedClassNames.glideModules);

    return true;
  }

  private String getGlideName(TypeElement appModule) {
    return appModule.getAnnotation(GlideModule.class).glideName();
  }

  @SuppressWarnings("unchecked")
  private FoundIndexedClassNames getIndexedClassNames(PackageElement glideGenPackage) {
    Set<String> glideModules = new HashSet<>();
    Set<String> extensions = new HashSet<>();
    List<? extends Element> glideGeneratedElements = glideGenPackage.getEnclosedElements();
    for (Element indexer : glideGeneratedElements) {
      Index annotation = indexer.getAnnotation(Index.class);
      // If the annotation is null, it means we've come across another class in the same package
      // that we can safely ignore.
      if (annotation != null) {
        Collections.addAll(glideModules, annotation.modules());
        Collections.addAll(extensions, annotation.extensions());
      }
    }

    processorUtil.debugLog("Found GlideModules: " + glideModules);
    return new FoundIndexedClassNames(glideModules, extensions);
  }

  private void writeGlide(String packageName, TypeSpec glide) {
    processorUtil.writeClass(packageName, glide);
  }

  private void writeRequestManager(String packageName, TypeSpec requestManager) {
    processorUtil.writeClass(packageName, requestManager);
  }

  // We dont' care about collisions in IDEs since this class isn't an API class.
  private void writeRequestManagerFactory(TypeSpec requestManagerFactory) {
    processorUtil.writeClass(
        AppModuleGenerator.GENERATED_ROOT_MODULE_PACKAGE_NAME, requestManagerFactory);
  }

  // The app module we generate subclasses a package private class. We don't care about classpath
  // collisions in IDEs since this class isn't an API class.
  private void writeAppModule(TypeSpec appModule) {
    processorUtil.writeClass(AppModuleGenerator.GENERATED_ROOT_MODULE_PACKAGE_NAME, appModule);
  }

  private void writeRequestOptions(String packageName, TypeSpec requestOptions) {
    processorUtil.writeClass(packageName, requestOptions);
  }

  private void writeRequestBuilder(String packageName, TypeSpec requestBuilder) {
    processorUtil.writeClass(packageName, requestBuilder);
  }

  private static final class FoundIndexedClassNames {
    private final Set<String> glideModules;
    private final Set<String> extensions;

    private FoundIndexedClassNames(Set<String> glideModules, Set<String> extensions) {
      this.glideModules = glideModules;
      this.extensions = extensions;
    }
  }
}
