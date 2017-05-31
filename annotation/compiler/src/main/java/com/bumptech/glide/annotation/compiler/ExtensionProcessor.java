package com.bumptech.glide.annotation.compiler;

import com.bumptech.glide.annotation.GlideExtension;
import com.squareup.javapoet.TypeSpec;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * Writes Indexer classes annotated with {@link Index} for all
 * classes found annotated with {@link GlideExtension}.
 */
final class ExtensionProcessor {
  private final ProcessorUtil processorUtil;
  private final IndexerGenerator indexerGenerator;

  ExtensionProcessor(ProcessorUtil processorUtil, IndexerGenerator indexerGenerator) {
    this.processorUtil = processorUtil;
    this.indexerGenerator = indexerGenerator;
  }

  boolean processExtensions(Set<? extends TypeElement> set, RoundEnvironment env) {
    List<TypeElement> elements = processorUtil.getElementsFor(GlideExtension.class, env);
    processorUtil.debugLog("Processing types : " + elements);
    for (TypeElement typeElement : elements) {
      GlideExtensionValidator.validateExtension(typeElement);
      processorUtil.debugLog("Processing elements: " + typeElement.getEnclosedElements());
    }

    if (elements.isEmpty()) {
      return false;
    }
    TypeSpec spec = indexerGenerator.generate(elements);
    processorUtil.writeIndexer(spec);
    return true;
  }

  Set<String> getSupportedAnnotationTypes() {
    return Collections.singleton(GlideExtension.class.getName());
  }
}
