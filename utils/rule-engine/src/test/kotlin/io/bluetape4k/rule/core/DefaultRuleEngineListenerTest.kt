package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Condition
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.RuleEngineConfig
import io.bluetape4k.rule.api.ruleSetOf
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class DefaultRuleEngineListenerTest {

    companion object: KLogging()

    @Test
    fun `beforeEvaluate with empty rules does not throw`() {
        val listener = DefaultRuleEngineListener(RuleEngineConfig.DEFAULT)
        listener.beforeEvaluate(emptyList(), Facts.empty()) // should not throw
    }

    @Test
    fun `beforeEvaluate with non-empty rules does not throw`() {
        val listener = DefaultRuleEngineListener(RuleEngineConfig.DEFAULT)
        val rule = DefaultRule(name = "test", condition = Condition.TRUE)
        listener.beforeEvaluate(listOf(rule), Facts.empty()) // should not throw
    }

    @Test
    fun `afterExecute does not throw`() {
        val listener = DefaultRuleEngineListener(RuleEngineConfig.DEFAULT)
        val rule = DefaultRule(name = "test", condition = Condition.TRUE)
        listener.afterExecute(listOf(rule), Facts.empty()) // should not throw
    }

    @Test
    fun `listener config property`() {
        val config = RuleEngineConfig(skipOnFirstAppliedRule = true)
        val listener = DefaultRuleEngineListener(config)
        listener.config shouldBeEqualTo config
    }

    @Test
    fun `RuleEngineListener registered in engine is called`() {
        var afterExecuteCalled = false
        val customListener = object : io.bluetape4k.rule.api.RuleEngineListener {
            override fun afterExecute(rules: Iterable<io.bluetape4k.rule.api.Rule>, facts: Facts) {
                afterExecuteCalled = true
            }
        }
        val engine = DefaultRuleEngine()
        engine.registerRuleEngineListener(customListener)

        val rule = DefaultRule(name = "test", condition = Condition.TRUE)
        engine.fire(ruleSetOf(rule), Facts.empty())
        afterExecuteCalled.shouldBeTrue()
    }

    @Test
    fun `clearEngineListeners removes all listeners`() {
        val engine = DefaultRuleEngine()
        engine.clearEngineListeners()
        engine.ruleEngineListeners.isEmpty().shouldBeTrue()
    }

    @Test
    fun `registerRuleEngineListeners adds multiple listeners`() {
        val engine = DefaultRuleEngine()
        engine.clearEngineListeners()
        engine.registerRuleEngineListeners(
            listOf(
                DefaultRuleEngineListener(),
                DefaultRuleEngineListener()
            )
        )
        engine.ruleEngineListeners.size shouldBeEqualTo 2
    }
}
