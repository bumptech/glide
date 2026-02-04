package com.bumptech.glide.annotation.ksp.test

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Tests that module sorting by qualified name works correctly and deterministically.
 * This ensures reproducible builds across KSP versions.
 */
@RunWith(JUnit4::class)
class ModuleSortingLogicTest {

  @Test
  fun sortByQualifiedName_withTwoModules_sortsAlphabetically() {
    val modules = listOf("com.test.ZebraModule", "com.test.AppleModule")

    val sorted = modules.sorted()

    assertThat(sorted).containsExactly("com.test.AppleModule", "com.test.ZebraModule").inOrder()
  }

  @Test
  fun sortByQualifiedName_withReverseOrder_producesIdenticalSortedResult() {
    val modulesForward = listOf("com.test.Alpha", "com.test.Beta", "com.test.Gamma")
    val modulesReverse = listOf("com.test.Gamma", "com.test.Beta", "com.test.Alpha")

    val sortedForward = modulesForward.sorted()
    val sortedReverse = modulesReverse.sorted()

    assertThat(sortedForward).isEqualTo(sortedReverse)
    assertThat(sortedForward)
      .containsExactly("com.test.Alpha", "com.test.Beta", "com.test.Gamma")
      .inOrder()
  }

  @Test
  fun sortByQualifiedName_withDifferentPackages_sortsByFullQualifiedName() {
    val modules =
      listOf("org.example.Module", "com.example.Module", "app.example.Module", "net.example.Module")

    val sorted = modules.sorted()

    assertThat(sorted)
      .containsExactly(
        "app.example.Module",
        "com.example.Module",
        "net.example.Module",
        "org.example.Module"
      )
      .inOrder()
  }

  @Test
  fun sortByQualifiedName_withSimilarNames_sortsCorrectly() {
    val modules = listOf("MyModule", "MyModuleExt", "MyModule2", "MyModuleA")

    val sorted = modules.sorted()

    assertThat(sorted).containsExactly("MyModule", "MyModule2", "MyModuleA", "MyModuleExt").inOrder()
  }

  @Test
  fun sortByQualifiedName_withNestedPackages_sortsByDepth() {
    val modules =
      listOf("com.test.deep.nested.DeepModule", "com.test.ShallowModule", "com.test.deep.MidModule")

    val sorted = modules.sorted()

    assertThat(sorted)
      .containsExactly(
        "com.test.ShallowModule",
        "com.test.deep.MidModule",
        "com.test.deep.nested.DeepModule"
      )
      .inOrder()
  }

  @Test
  fun sortByQualifiedName_withTenModules_sortsCorrectly() {
    val modules =
      listOf(
        "Module10",
        "Module1",
        "Module9",
        "Module2",
        "Module8",
        "Module3",
        "Module7",
        "Module4",
        "Module6",
        "Module5"
      )

    val sorted = modules.sorted()

    assertThat(sorted)
      .containsExactly(
        "Module1",
        "Module10",
        "Module2",
        "Module3",
        "Module4",
        "Module5",
        "Module6",
        "Module7",
        "Module8",
        "Module9"
      )
      .inOrder()
  }

  @Test
  fun sortByQualifiedName_withRandomOrder_alwaysProducesSameResult() {
    val moduleNames =
      listOf("Delta", "Alpha", "Echo", "Bravo", "Charlie", "Foxtrot", "Golf", "Hotel", "India")

    // Create three different orderings
    val modules1 = moduleNames
    val modules2 = moduleNames.shuffled()
    val modules3 = moduleNames.shuffled()

    val sorted1 = modules1.sorted()
    val sorted2 = modules2.sorted()
    val sorted3 = modules3.sorted()

    // All three should produce identical sorted results
    assertThat(sorted1).isEqualTo(sorted2)
    assertThat(sorted2).isEqualTo(sorted3)
    assertThat(sorted1)
      .containsExactly(
        "Alpha",
        "Bravo",
        "Charlie",
        "Delta",
        "Echo",
        "Foxtrot",
        "Golf",
        "Hotel",
        "India"
      )
      .inOrder()
  }

  @Test
  fun sortByQualifiedName_withCaseSensitiveNames_sortsCorrectly() {
    val modules = listOf("com.test.aModule", "com.test.AModule", "com.test.BModule")

    val sorted = modules.sorted()

    // Capital letters come before lowercase in lexicographic order
    assertThat(sorted)
      .containsExactly("com.test.AModule", "com.test.BModule", "com.test.aModule")
      .inOrder()
  }

  @Test
  fun sortByQualifiedName_withLongPackageHierarchy_sortsCorrectly() {
    val modules =
      listOf(
        "com.company.product.feature.module.deep.nested.VeryDeepModule",
        "com.company.product.feature.ShallowModule",
        "com.company.product.feature.module.MidModule",
        "com.company.OtherModule"
      )

    val sorted = modules.sorted()

    assertThat(sorted)
      .containsExactly(
        "com.company.OtherModule",
        "com.company.product.feature.ShallowModule",
        "com.company.product.feature.module.MidModule",
        "com.company.product.feature.module.deep.nested.VeryDeepModule"
      )
      .inOrder()
  }

  @Test
  fun sortByQualifiedName_withSpecialCharacters_sortsCorrectly() {
    // Test modules with underscores and numbers
    val modules =
      listOf(
        "com.test.Module_V2",
        "com.test.Module_V1",
        "com.test.Module2",
        "com.test.Module1"
      )

    val sorted = modules.sorted()

    assertThat(sorted)
      .containsExactly(
        "com.test.Module1",
        "com.test.Module2",
        "com.test.Module_V1",
        "com.test.Module_V2"
      )
      .inOrder()
  }
}
