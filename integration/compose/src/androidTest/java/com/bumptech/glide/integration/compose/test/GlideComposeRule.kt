package com.bumptech.glide.integration.compose.test

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.bumptech.glide.load.engine.executor.GlideIdlingResourceInit
import com.bumptech.glide.testutil.TearDownGlide
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Merges [TearDownGlide], [ComposeContentTestRule] and [GlideIdlingResourceInit] into a single
 * helper rule that's common across (most of) Glide's compose integration tests.
 */
class GlideComposeRule(
  private val composeRule: ComposeContentTestRule = createComposeRule(),
) : TestRule, ComposeContentTestRule by composeRule {
  private val rules = RuleChain.outerRule(TearDownGlide()).around(composeRule)

  override fun apply(base: Statement?, description: Description?): Statement {
    return rules.apply(
      object : Statement() {
        override fun evaluate() {
          GlideIdlingResourceInit.initGlide(this@GlideComposeRule)
          base?.evaluate()
        }
      },
      description,
    )
  }
}
