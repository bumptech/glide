package com.bumptech.glide.annotation.compiler;

import com.bumptech.glide.annotation.GlideModule;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * Generates {@link ModuleIndex} annotated classes for all
 * {@link com.bumptech.glide.module.ChildGlideModule} implementations.
 */
final class ChildModuleProcessor {
  private static final String COMPILER_PACKAGE_NAME =
      GlideAnnotationProcessor.class.getPackage().getName();

  private ProcessorUtil processorUtil;

  ChildModuleProcessor(ProcessorUtil processorUtil) {
    this.processorUtil = processorUtil;
  }

  boolean processModules(Set<? extends TypeElement> set, RoundEnvironment env) {
     // Order matters here, if we find an Indexer below, we return before writing the root module.
    // If we fail to add to rootModules before then, we might accidentally skip a valid RootModule.
    List<TypeElement> childGlideModules = new ArrayList<>();
    for (TypeElement element : processorUtil.getElementsFor(GlideModule.class, env)) {
      // Root elements are added separately and must be checked separately because they're sub
      // classes of ChildGlideModules.
      if (processorUtil.isRootGlideModule(element)) {
        continue;
      } else if (!processorUtil.isChildGlideModule(element)) {
        throw new IllegalStateException("@GlideModule can only be applied to ChildGlideModule"
            + " and RootGlideModule implementations, not: " + element);
      }

      childGlideModules.add(element);
    }

    processorUtil.debugLog("got child modules: " + childGlideModules);
    if (childGlideModules.isEmpty()) {
      return false;
    }

    TypeSpec indexer = GlideIndexerGenerator.generate(childGlideModules);
    writeIndexer(indexer);
    processorUtil.debugLog("Wrote an Indexer this round, skipping the root module to ensure all "
        + "indexers are found");
     // If I write an Indexer in a round in the target package, then try to find all classes in
    // the target package, my newly written Indexer won't be found. Since we wrote a class with
    // an Annotation handled by this processor, we know we will be called again in the next round
    // and we can safely wait to write our RootModule until then.
    return true;
  }

  private void writeIndexer(TypeSpec indexer) {
    processorUtil.writeClass(COMPILER_PACKAGE_NAME, indexer);
  }

  Set<String> getSupportedAnnotationTypes() {
    return new HashSet<>(Arrays.asList(
        ModuleIndex.class.getName(),
        GlideModule.class.getName()
    ));
  }
}
