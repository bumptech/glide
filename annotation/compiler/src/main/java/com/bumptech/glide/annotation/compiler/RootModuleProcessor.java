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
 * Generates the final {@link com.bumptech.glide.module.RootGlideModule} class after processing
 * all {@link GlideModule} annotated classes.
 */
final class RootModuleProcessor {
  private static final String GENERATED_ROOT_MODULE_PACKAGE_NAME = "com.bumptech.glide";
  private static final String COMPILER_PACKAGE_NAME =
      GlideAnnotationProcessor.class.getPackage().getName();

  private final ProcessingEnvironment processingEnv;
  private final ProcessorUtil processorUtil;
  private final List<TypeElement> rootGlideModules = new ArrayList<>();
  private final RequestOptionsGenerator requestOptionsGenerator;

  RootModuleProcessor(ProcessingEnvironment processingEnv, ProcessorUtil processorUtil) {
    this.processingEnv = processingEnv;
    this.processorUtil = processorUtil;

    requestOptionsGenerator = new RequestOptionsGenerator(processingEnv);
  }

  void processModules(Set<? extends TypeElement> set, RoundEnvironment env) {
     for (TypeElement element : processorUtil.getElementsFor(GlideModule.class, env)) {
       if (processorUtil.isRootGlideModule(element)) {
         rootGlideModules.add(element);
       }
     }

    processorUtil.debugLog("got root modules: " + rootGlideModules);

    if (rootGlideModules.size() > 1) {
      throw new IllegalStateException(
          "You cannot have more than one RootGlideModule, found: " + rootGlideModules);
    }
  }

  boolean maybeWriteRootModule() {
    // rootGlideModules is added to in order to catch errors where multiple RootGlideModules may be
    // present for a single application or library. Because we only add to rootGlideModules, we use
    // isGeneratedRootGlideModuleWritten to make sure the GeneratedRootGlideModule is written at
    // most once.
    if (rootGlideModules.isEmpty()) {
      return false;
    }
    processorUtil.debugLog("Processing root module: " + rootGlideModules.iterator().next());
    // If this package is null, it means there are no classes with this package name. One way this
    // could happen is if we process an annotation and reach this point without writing something
    // to the package. We do not error check here because that shouldn't happen with the
    // current implementation.
    PackageElement glideGenPackage =
        processingEnv.getElementUtils().getPackageElement(COMPILER_PACKAGE_NAME);
    FoundIndexedClassNames indexedClassNames = getIndexedClassNames(glideGenPackage);

    TypeSpec generatedRootGlideModule =
        RootModuleGenerator.generate(
            processingEnv, rootGlideModules.get(0).getQualifiedName().toString(),
            indexedClassNames.glideModules);
    writeRootModule(generatedRootGlideModule);

    processorUtil.infoLog("Wrote GeneratedRootGlideModule with: " + indexedClassNames.glideModules);

    if (!indexedClassNames.extensions.isEmpty()) {
      TypeSpec generatedRequestOptions =
          requestOptionsGenerator.generate(indexedClassNames.extensions);
      writeRequestOptions(generatedRequestOptions);
    }

    return true;
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

  private void writeRootModule(TypeSpec rootModule) {
    processorUtil.writeClass(GENERATED_ROOT_MODULE_PACKAGE_NAME, rootModule);
  }

  private void writeRequestOptions(TypeSpec requestOptions) {
    processorUtil.writeClass(RequestOptionsGenerator.GENERATED_REQUEST_OPTIONS_PACKAGE_NAME,
        requestOptions);
  }

  private static final class FoundIndexedClassNames {
    final Set<String> glideModules;
    final Set<String> extensions;

    private FoundIndexedClassNames(Set<String> glideModules, Set<String> extensions) {
      this.glideModules = glideModules;
      this.extensions = extensions;
    }
  }
}
