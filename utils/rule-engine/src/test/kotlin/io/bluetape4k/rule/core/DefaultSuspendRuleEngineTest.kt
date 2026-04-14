package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.RuleEngineConfig
import io.bluetape4k.rule.api.suspendRuleSetOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class DefaultSuspendRuleEngineTest {

    companion object: KLogging()

    @Test
    fun `SuspendRule 기본 실행`() = runTest {
        val engine = DefaultSuspendRuleEngine()
        val rule = suspendRule {
            name = "asyncRule"
            condition { true }
            action { facts ->
                delay(10.milliseconds)
                facts["executed"] = true
            }
        }

        val facts = Facts.empty()
        engine.fire(suspendRuleSetOf(rule), facts)
        facts.get<Boolean>("executed").shouldNotBeNull().shouldBeTrue()
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
        facts.get<Boolean>("rule1").shouldNotBeNull().shouldBeTrue()
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

    @Test
    fun `SuspendRule 실행 중 CancellationException 은 삼키지 않고 전파한다`() = runTest {
        val engine = DefaultSuspendRuleEngine(RuleEngineConfig(skipOnFirstFailedRule = true))
        val canceledRule = suspendRule {
            name = "canceledRule"
            priority = 1
            condition { true }
            action { throw CancellationException("cancel") }
        }
        val nextRule = suspendRule {
            name = "nextRule"
            priority = 2
            condition { true }
            action { facts -> facts["nextRule"] = true }
        }

        assertFailsWith<CancellationException> {
            engine.fire(suspendRuleSetOf(canceledRule, nextRule), Facts.empty())
        }.message shouldBeEqualTo "cancel"
    }

    @Test
    fun `SuspendRule 평가 중 CancellationException 은 삼키지 않고 전파한다`() = runTest {
        val engine = DefaultSuspendRuleEngine(RuleEngineConfig(skipOnFirstFailedRule = true))
        val canceledRule = suspendRule {
            name = "canceledOnEvaluate"
            priority = 1
            condition { throw CancellationException("cancel-on-evaluate") }
            action { facts -> facts["executed"] = true }
        }
        val nextRule = suspendRule {
            name = "nextRule"
            priority = 2
            condition { true }
            action { facts -> facts["nextRule"] = true }
        }

        assertFailsWith<CancellationException> {
            engine.fire(suspendRuleSetOf(canceledRule, nextRule), Facts.empty())
        }.message shouldBeEqualTo "cancel-on-evaluate"
    }

    @Test
    fun `SuspendRule 평가 실패는 skipOnFirstFailedRule 에 따라 다음 Rule 을 중단한다`() = runTest {
        val engine = DefaultSuspendRuleEngine(RuleEngineConfig(skipOnFirstFailedRule = true))
        val failedOnEvaluateRule = suspendRule {
            name = "failedOnEvaluate"
            priority = 1
            condition { error("evaluate-fail") }
            action { facts -> facts["executed"] = true }
        }
        val nextRule = suspendRule {
            name = "nextRule"
            priority = 2
            condition { true }
            action { facts -> facts["nextRule"] = true }
        }

        val facts = Facts.empty()
        engine.fire(suspendRuleSetOf(failedOnEvaluateRule, nextRule), facts)
        facts.containsKey("executed").shouldBeFalse()
        facts.containsKey("nextRule").shouldBeFalse()
    }

    @Test
    fun `SuspendRule check 중 CancellationException 은 삼키지 않고 전파한다`() = runTest {
        val engine = DefaultSuspendRuleEngine()
        val canceledRule = suspendRule {
            name = "canceledOnCheck"
            condition { throw CancellationException("cancel-on-check") }
            action { }
        }

        assertFailsWith<CancellationException> {
            engine.check(suspendRuleSetOf(canceledRule), Facts.empty())
        }.message shouldBeEqualTo "cancel-on-check"
    }

    @Test
    fun `SuspendRule check 중 평가 실패는 false 로 기록한다`() = runTest {
        val engine = DefaultSuspendRuleEngine()
        val failedRule = suspendRule {
            name = "failedOnCheck"
            condition { error("check-fail") }
            action { }
        }
        val successRule = suspendRule {
            name = "successRule"
            priority = 2
            condition { true }
            action { }
        }

        val result = engine.check(suspendRuleSetOf(failedRule, successRule), Facts.empty())
        result[failedRule] shouldBeEqualTo false
        result[successRule].shouldNotBeNull().shouldBeTrue()
    }
}
