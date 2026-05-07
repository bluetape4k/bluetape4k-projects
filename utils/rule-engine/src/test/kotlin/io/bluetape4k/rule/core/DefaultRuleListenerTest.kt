package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Condition
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.ruleSetOf
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class DefaultRuleListenerTest {

    companion object: KLogging()

    @Test
    fun `beforeEvaluate always returns true`() {
        val listener = DefaultRuleListener()
        val rule = DefaultRule(name = "test", condition = Condition.TRUE)
        val facts = Facts.empty()
        listener.beforeEvaluate(rule, facts).shouldBeTrue()
    }

    @Test
    fun `afterEvaluate true does not throw`() {
        val listener = DefaultRuleListener()
        val rule = DefaultRule(name = "test", condition = Condition.TRUE)
        val facts = Facts.empty()
        listener.afterEvaluate(rule, facts, true) // should not throw
    }

    @Test
    fun `afterEvaluate false does not throw`() {
        val listener = DefaultRuleListener()
        val rule = DefaultRule(name = "test", condition = Condition.TRUE)
        val facts = Facts.empty()
        listener.afterEvaluate(rule, facts, false) // should not throw
    }

    @Test
    fun `beforeExecute does not throw`() {
        val listener = DefaultRuleListener()
        val rule = DefaultRule(name = "test", condition = Condition.TRUE)
        val facts = Facts.empty()
        listener.beforeExecute(rule, facts) // should not throw
    }

    @Test
    fun `afterExecute without exception does not throw`() {
        val listener = DefaultRuleListener()
        val rule = DefaultRule(name = "test", condition = Condition.TRUE)
        val facts = Facts.empty()
        listener.afterExecute(rule, facts, null) // should not throw
    }

    @Test
    fun `afterExecute with exception does not throw`() {
        val listener = DefaultRuleListener()
        val rule = DefaultRule(name = "test", condition = Condition.TRUE)
        val facts = Facts.empty()
        listener.afterExecute(rule, facts, RuntimeException("test error")) // should not throw
    }

    @Test
    fun `RuleListener registered in engine is called`() {
        var beforeEvaluateCalled = false
        val customListener = object : io.bluetape4k.rule.api.RuleListener {
            override fun beforeEvaluate(rule: io.bluetape4k.rule.api.Rule, facts: Facts): Boolean {
                beforeEvaluateCalled = true
                return true
            }
        }
        val engine = DefaultRuleEngine()
        engine.registerRuleListener(customListener)

        val rule = DefaultRule(name = "test", condition = Condition.TRUE)
        engine.fire(ruleSetOf(rule), Facts.empty())
        beforeEvaluateCalled.shouldBeTrue()
    }

    @Test
    fun `clearRuleListeners removes all listeners`() {
        val engine = DefaultRuleEngine()
        engine.clearRuleListeners()
        engine.ruleListeners.isEmpty().shouldBeTrue()
    }

    @Test
    fun `registerRuleListeners adds multiple listeners`() {
        val engine = DefaultRuleEngine()
        engine.clearRuleListeners()
        engine.registerRuleListeners(listOf(DefaultRuleListener(), DefaultRuleListener()))
        engine.ruleListeners.size shouldBeEqualTo 2
    }
}

