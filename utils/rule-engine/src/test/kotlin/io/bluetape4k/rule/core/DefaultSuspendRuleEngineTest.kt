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

    private inline fun buildSuspendRule(
        name: String,
        priority: Int = 0,
        crossinline cond: suspend (Facts) -> Boolean,
        crossinline act: suspend (Facts) -> Unit,
    ): DefaultSuspendRule = suspendRule {
        this.name = name; this.priority = priority; condition { cond(it) }; action { act(it) }
    }

    @Test
    fun `SuspendRule 기본 실행`() = runTest {
        val engine = DefaultSuspendRuleEngine()
        val rule = buildSuspendRule("asyncRule", cond = { true }, act = { delay(10.milliseconds); it["executed"] = true })

        val facts = Facts.empty()
        engine.fire(suspendRuleSetOf(rule), facts)
        facts.get<Boolean>("executed").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `SuspendRule 조건 불만족 시 미실행`() = runTest {
        val engine = DefaultSuspendRuleEngine()
        val rule = buildSuspendRule("asyncRule", cond = { false }, act = { it["executed"] = true })

        val facts = Facts.empty()
        engine.fire(suspendRuleSetOf(rule), facts)
        facts.containsKey("executed").shouldBeFalse()
    }

    @Test
    fun `SuspendRule skipOnFirstAppliedRule 동작`() = runTest {
        val config = RuleEngineConfig(skipOnFirstAppliedRule = true)
        val engine = DefaultSuspendRuleEngine(config)

        val rule1 = buildSuspendRule("rule1", 1, { true }, { it["rule1"] = true })
        val rule2 = buildSuspendRule("rule2", 2, { true }, { it["rule2"] = true })

        val facts = Facts.empty()
        engine.fire(suspendRuleSetOf(rule1, rule2), facts)
        facts.get<Boolean>("rule1").shouldNotBeNull().shouldBeTrue()
        facts.containsKey("rule2").shouldBeFalse()
    }

    @Test
    fun `SuspendRule check 메서드 동작`() = runTest {
        val engine = DefaultSuspendRuleEngine()
        val rule1 = buildSuspendRule("trueRule", cond = { true }, act = { })
        val rule2 = buildSuspendRule("falseRule", cond = { false }, act = { })

        val result = engine.check(suspendRuleSetOf(rule1, rule2), Facts.empty())
        result[rule1]!!.shouldBeTrue()
        result[rule2]!!.shouldBeFalse()
    }

    @Test
    fun `SuspendRule skipOnFirstFailedRule 동작`() = runTest {
        val config = RuleEngineConfig(skipOnFirstFailedRule = true)
        val engine = DefaultSuspendRuleEngine(config)

        val rule1 = buildSuspendRule("failRule", 1, { true }, { error("fail!") })
        val rule2 = buildSuspendRule("rule2", 2, { true }, { it["rule2"] = true })

        val facts = Facts.empty()
        engine.fire(suspendRuleSetOf(rule1, rule2), facts)
        facts.containsKey("rule2").shouldBeFalse()
    }

    @Test
    fun `SuspendRule 우선순위 순서대로 실행`() = runTest {
        val engine = DefaultSuspendRuleEngine()
        val executionOrder = mutableListOf<String>()

        val rule1 = buildSuspendRule("second", 2, { true }, { executionOrder.add("second") })
        val rule2 = buildSuspendRule("first", 1, { true }, { executionOrder.add("first") })

        engine.fire(suspendRuleSetOf(rule1, rule2), Facts.empty())
        executionOrder shouldBeEqualTo listOf("first", "second")
    }

    @Test
    fun `SuspendRule 실행 중 CancellationException 은 삼키지 않고 전파한다`() = runTest {
        val engine = DefaultSuspendRuleEngine(RuleEngineConfig(skipOnFirstFailedRule = true))
        val canceledRule = buildSuspendRule("canceledRule", 1, { true }, { throw CancellationException("cancel") })
        val nextRule = buildSuspendRule("nextRule", 2, { true }, { it["nextRule"] = true })

        assertFailsWith<CancellationException> {
            engine.fire(suspendRuleSetOf(canceledRule, nextRule), Facts.empty())
        }.message shouldBeEqualTo "cancel"
    }

    @Test
    fun `SuspendRule 평가 중 CancellationException 은 삼키지 않고 전파한다`() = runTest {
        val engine = DefaultSuspendRuleEngine(RuleEngineConfig(skipOnFirstFailedRule = true))
        val canceledRule = buildSuspendRule("canceledOnEvaluate", 1, { throw CancellationException("cancel-on-evaluate") }, { it["executed"] = true })
        val nextRule = buildSuspendRule("nextRule", 2, { true }, { it["nextRule"] = true })

        assertFailsWith<CancellationException> {
            engine.fire(suspendRuleSetOf(canceledRule, nextRule), Facts.empty())
        }.message shouldBeEqualTo "cancel-on-evaluate"
    }

    @Test
    fun `SuspendRule 평가 실패는 skipOnFirstFailedRule 에 따라 다음 Rule 을 중단한다`() = runTest {
        val engine = DefaultSuspendRuleEngine(RuleEngineConfig(skipOnFirstFailedRule = true))
        val failedOnEvaluateRule = buildSuspendRule("failedOnEvaluate", 1, { error("evaluate-fail") }, { it["executed"] = true })
        val nextRule = buildSuspendRule("nextRule", 2, { true }, { it["nextRule"] = true })

        val facts = Facts.empty()
        engine.fire(suspendRuleSetOf(failedOnEvaluateRule, nextRule), facts)
        facts.containsKey("executed").shouldBeFalse()
        facts.containsKey("nextRule").shouldBeFalse()
    }

    @Test
    fun `SuspendRule check 중 CancellationException 은 삼키지 않고 전파한다`() = runTest {
        val engine = DefaultSuspendRuleEngine()
        val canceledRule = buildSuspendRule("canceledOnCheck", cond = { throw CancellationException("cancel-on-check") }, act = { })

        assertFailsWith<CancellationException> {
            engine.check(suspendRuleSetOf(canceledRule), Facts.empty())
        }.message shouldBeEqualTo "cancel-on-check"
    }

    @Test
    fun `SuspendRule check 중 평가 실패는 false 로 기록한다`() = runTest {
        val engine = DefaultSuspendRuleEngine()
        val failedRule = buildSuspendRule("failedOnCheck", cond = { error("check-fail") }, act = { })
        val successRule = buildSuspendRule("successRule", 2, { true }, { })

        val result = engine.check(suspendRuleSetOf(failedRule, successRule), Facts.empty())
        result[failedRule] shouldBeEqualTo false
        result[successRule].shouldNotBeNull().shouldBeTrue()
    }
}
