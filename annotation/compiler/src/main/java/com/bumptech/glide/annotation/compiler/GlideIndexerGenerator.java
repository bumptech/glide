package com.bumptech.glide.annotation.compiler;

import static com.bumptech.glide.annotation.compiler.ModuleAnnotationProcessor.INDEXER_NAME_PREFIX;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * Generates an empty class with an annotation containing the class names of one or more
 * ChildGlideModules.
 *
 * <p>We use a separate class so that ChildGlideModules written in libraries can be bundled into an
 * AAR and later retrieved by the annotation processor when it processes the RootGlideModule in an
 * application.
 *
 * <p>The output of this file looks like this:
 * <pre>
 * <code>
 *  {@literal @com.bumptech.glide.annotation.compiler.ModuleIndex(}
 *      glideModules = "com.bumptech.glide.integration.okhttp3.OkHttpChildGlideModule"
 *  )
 *  public class GlideIndexer_com_bumptech_glide_integration_okhttp3_OkHttpChildGlideModule {
 *  }
 * </code>
 * </pre>
 */
final class GlideIndexerGenerator {

  private GlideIndexerGenerator() { }

  static TypeSpec generate(List<TypeElement> childModules) {
    AnnotationSpec.Builder annotationBuilder =
        AnnotationSpec.builder(ModuleIndex.class);
    for (TypeElement childModule : childModules) {
      annotationBuilder.addMember("glideModules", "$S", ClassName.get(childModule).toString());
    }

    String indexerName = INDEXER_NAME_PREFIX;
    for (TypeElement element : childModules) {
      indexerName += element.getQualifiedName().toString().replace(".", "_");
      indexerName += "_";
    }
    indexerName = indexerName.substring(0, indexerName.length() - 1);

    return TypeSpec.classBuilder(indexerName)
        .addAnnotation(annotationBuilder.build())
        .addModifiers(Modifier.PUBLIC)
        .build();
  }
}
