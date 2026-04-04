package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.RuleEngineConfig
import io.bluetape4k.rule.api.suspendRuleSetOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class DefaultSuspendRuleEngineTest {

    companion object: KLogging()

    @Test
    fun `SuspendRule 기본 실행`() = runTest {
        val engine = DefaultSuspendRuleEngine()
        val rule = suspendRule {
            name = "asyncRule"
            condition { true }
            action { facts ->
                delay(10)
                facts["executed"] = true
            }
        }

        val facts = Facts.empty()
        engine.fire(suspendRuleSetOf(rule), facts)
        facts.get<Boolean>("executed") shouldBeEqualTo true
    }

    @Test
    fun `SuspendRule 조건 불만족 시 미실행`() = runTest {
        val engine = DefaultSuspendRuleEngine()
        val rule = suspendRule {
            name = "asyncRule"
            condition { false }
            action { facts -> facts["executed"] = true }
        }

        val facts = Facts.empty()
        engine.fire(suspendRuleSetOf(rule), facts)
        facts.containsKey("executed").shouldBeFalse()
    }

    @Test
    fun `SuspendRule skipOnFirstAppliedRule 동작`() = runTest {
        val config = RuleEngineConfig(skipOnFirstAppliedRule = true)
        val engine = DefaultSuspendRuleEngine(config)

        val rule1 = suspendRule {
            name = "rule1"
            priority = 1
            condition { true }
            action { facts -> facts["rule1"] = true }
        }
        val rule2 = suspendRule {
            name = "rule2"
            priority = 2
            condition { true }
            action { facts -> facts["rule2"] = true }
        }

        val facts = Facts.empty()
        engine.fire(suspendRuleSetOf(rule1, rule2), facts)
        facts.get<Boolean>("rule1") shouldBeEqualTo true
        facts.containsKey("rule2").shouldBeFalse()
    }

    @Test
    fun `SuspendRule check 메서드 동작`() = runTest {
        val engine = DefaultSuspendRuleEngine()
        val rule1 = suspendRule {
            name = "trueRule"
            condition { true }
            action { }
        }
        val rule2 = suspendRule {
            name = "falseRule"
            condition { false }
            action { }
        }

        val result = engine.check(suspendRuleSetOf(rule1, rule2), Facts.empty())
        result[rule1]!!.shouldBeTrue()
        result[rule2]!!.shouldBeFalse()
    }

    @Test
    fun `SuspendRule skipOnFirstFailedRule 동작`() = runTest {
        val config = RuleEngineConfig(skipOnFirstFailedRule = true)
        val engine = DefaultSuspendRuleEngine(config)

        val rule1 = suspendRule {
            name = "failRule"
            priority = 1
            condition { true }
            action { error("fail!") }
        }
        val rule2 = suspendRule {
            name = "rule2"
            priority = 2
            condition { true }
            action { facts -> facts["rule2"] = true }
        }

        val facts = Facts.empty()
        engine.fire(suspendRuleSetOf(rule1, rule2), facts)
        facts.containsKey("rule2").shouldBeFalse()
    }

    @Test
    fun `SuspendRule 우선순위 순서대로 실행`() = runTest {
        val engine = DefaultSuspendRuleEngine()
        val executionOrder = mutableListOf<String>()

        val rule1 = suspendRule {
            name = "second"
            priority = 2
            condition { true }
            action { executionOrder.add("second") }
        }
        val rule2 = suspendRule {
            name = "first"
            priority = 1
            condition { true }
            action { executionOrder.add("first") }
        }

        engine.fire(suspendRuleSetOf(rule1, rule2), Facts.empty())
        executionOrder shouldBeEqualTo listOf("first", "second")
    }
}
