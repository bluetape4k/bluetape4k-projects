package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Action
import io.bluetape4k.rule.api.Condition
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.Rule
import io.bluetape4k.rule.api.RuleEngineConfig
import io.bluetape4k.rule.api.RuleEngineListener
import io.bluetape4k.rule.api.RuleListener
import io.bluetape4k.rule.api.ruleSetOf
import kotlinx.coroutines.CancellationException
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

class DefaultRuleEngineTest {

    companion object: KLogging()

    private fun createEngine(config: RuleEngineConfig = RuleEngineConfig.DEFAULT): DefaultRuleEngine {
        return DefaultRuleEngine(config)
    }

    private inline fun buildRule(
        name: String,
        priority: Int = 0,
        crossinline cond: (Facts) -> Boolean,
        crossinline act: (Facts) -> Unit,
    ): DefaultRule = rule {
        this.name = name; this.priority = priority; condition { cond(it) }; action { act(it) }
    }

    @Test
    fun `기본 설정으로 엔진 생성`() {
        val engine = createEngine()
        engine.config shouldBeEqualTo RuleEngineConfig.DEFAULT
    }

    @Test
    fun `단일 Rule 실행`() {
        val engine = createEngine()
        val rule = buildRule("test", cond = { true }, act = { it["executed"] = true })
        val facts = Facts.empty()
        engine.fire(ruleSetOf(rule), facts); facts.get<Boolean>("executed").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `조건 불만족 시 Rule 미실행`() {
        val engine = createEngine()
        val rule = buildRule("test", cond = { false }, act = { it["executed"] = true })
        val facts = Facts.empty()
        engine.fire(ruleSetOf(rule), facts); facts.containsKey("executed").shouldBeFalse()
    }

    @Test
    fun `skipOnFirstAppliedRule 옵션 동작`() {
        val config = RuleEngineConfig(skipOnFirstAppliedRule = true)
        val engine = createEngine(config)

        val rule1 = buildRule("rule1", 1, { true }, { it["rule1"] = true })
        val rule2 = buildRule("rule2", 2, { true }, { it["rule2"] = true })

        val facts = Facts.empty()
        engine.fire(ruleSetOf(rule1, rule2), facts)
        facts.get<Boolean>("rule1").shouldNotBeNull().shouldBeTrue()
        facts.containsKey("rule2").shouldBeFalse()
    }

    @Test
    fun `skipOnFirstFailedRule 옵션 동작`() {
        val config = RuleEngineConfig(skipOnFirstFailedRule = true)
        val engine = createEngine(config)

        val rule1 = buildRule("failRule", 1, { true }, { error("fail!") })
        val rule2 = buildRule("rule2", 2, { true }, { it["rule2"] = true })

        val facts = Facts.empty()
        engine.fire(ruleSetOf(rule1, rule2), facts); facts.containsKey("rule2").shouldBeFalse()
    }

    @Test
    fun `실행 중 CancellationException 은 삼키지 않고 전파한다`() {
        val engine = createEngine(RuleEngineConfig(skipOnFirstFailedRule = true))
        val canceledRule = buildRule("canceledRule", 1, { true }, { throw CancellationException("cancel") })
        val nextRule = buildRule("nextRule", 2, { true }, { it["nextRule"] = true })

        (assertFailsWith<CancellationException> {
            engine.fire(ruleSetOf(canceledRule, nextRule), Facts.empty())
        }).message shouldBeEqualTo "cancel"
    }

    @Test
    fun `평가 중 CancellationException 은 삼키지 않고 전파한다`() {
        val engine = createEngine(RuleEngineConfig(skipOnFirstFailedRule = true))
        val canceledRule = buildRule(
            "canceledOnEvaluate",
            1,
            { throw CancellationException("cancel-on-evaluate") },
            { it["executed"] = true }
        )
        val nextRule = buildRule("nextRule", 2, { true }, { it["nextRule"] = true })

        (assertFailsWith<CancellationException> {
            engine.fire(ruleSetOf(canceledRule, nextRule), Facts.empty())
        }).message shouldBeEqualTo "cancel-on-evaluate"
    }

    @Test
    fun `평가 실패는 skipOnFirstFailedRule 에 따라 다음 Rule 을 중단한다`() {
        val config = RuleEngineConfig(skipOnFirstFailedRule = true)
        val engine = createEngine(config)

        val failedOnEvaluateRule = buildRule("failedOnEvaluate", 1, { error("evaluate-fail") }, { it["executed"] = true })
        val nextRule = buildRule("nextRule", 2, { true }, { it["nextRule"] = true })

        val facts = Facts.empty()
        engine.fire(ruleSetOf(failedOnEvaluateRule, nextRule), facts)

        facts.containsKey("executed").shouldBeFalse()
        facts.containsKey("nextRule").shouldBeFalse()
    }

    @Test
    fun `평가 실패는 기본 설정에서 다음 Rule 실행을 막지 않는다`() {
        val engine = createEngine()
        val failedOnEvaluateRule = buildRule("failedOnEvaluate", 1, { error("evaluate-fail") }, { it["executed"] = true })
        val nextRule = buildRule("nextRule", 2, { true }, { it["nextRule"] = true })

        val facts = Facts.empty()
        engine.fire(ruleSetOf(failedOnEvaluateRule, nextRule), facts)

        facts.containsKey("executed").shouldBeFalse()
        facts.get<Boolean>("nextRule").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `평가 실패도 listener lifecycle 을 완료한다`() {
        val engine = createEngine(RuleEngineConfig(skipOnFirstFailedRule = true))
        var afterEvaluateResult: Boolean? = null
        var beforeExecuteCalled = false
        var afterRulesCalled = false
        engine.registerRuleListener(object : RuleListener {
            override fun afterEvaluate(rule: Rule, facts: Facts, evaluationResult: Boolean) {
                afterEvaluateResult = evaluationResult
            }

            override fun beforeExecute(rule: Rule, facts: Facts) {
                beforeExecuteCalled = true
            }
        })
        engine.registerRuleEngineListener(object : RuleEngineListener {
            override fun afterExecute(rules: Iterable<Rule>, facts: Facts) {
                afterRulesCalled = true
            }
        })
        val failedOnEvaluateRule = buildRule(
            "failedOnEvaluate",
            cond = { error("evaluate-fail") },
            act = { it["executed"] = true }
        )

        engine.fire(ruleSetOf(failedOnEvaluateRule), Facts.empty())

        afterEvaluateResult shouldBeEqualTo false
        beforeExecuteCalled.shouldBeFalse()
        afterRulesCalled.shouldBeTrue()
    }

    @Test
    fun `skipOnFirstNonTriggeredRule 옵션 동작`() {
        val config = RuleEngineConfig(skipOnFirstNonTriggeredRule = true)
        val engine = createEngine(config)

        val rule1 = buildRule("falseRule", 1, { false }, { it["rule1"] = true })
        val rule2 = buildRule("rule2", 2, { true }, { it["rule2"] = true })

        val facts = Facts.empty()
        engine.fire(ruleSetOf(rule1, rule2), facts)
        facts.containsKey("rule1").shouldBeFalse()
        facts.containsKey("rule2").shouldBeFalse()
    }

    @Test
    fun `priorityThreshold 초과 Rule 무시`() {
        val config = RuleEngineConfig(priorityThreshold = 5)
        val engine = createEngine(config)

        val rule1 = buildRule("low", 1, { true }, { it["low"] = true })
        val rule2 = buildRule("high", 10, { true }, { it["high"] = true })

        val facts = Facts.empty()
        engine.fire(ruleSetOf(rule1, rule2), facts)
        facts.get<Boolean>("low").shouldNotBeNull().shouldBeTrue()
        facts.containsKey("high").shouldBeFalse()
    }

    @Test
    fun `check 메서드로 Rule 평가`() {
        val engine = createEngine()
        val rule1 = buildRule("trueRule", cond = { true }, act = { })
        val rule2 = buildRule("falseRule", cond = { false }, act = { })

        val facts = Facts.empty()
        val result = engine.check(ruleSetOf(rule1, rule2), facts)
        result[rule1].shouldNotBeNull().shouldBeTrue()
        result[rule2] shouldBeEqualTo false
    }

    @Test
    fun `check 중 평가 실패는 false 로 기록한다`() {
        val engine = createEngine()
        val failedRule = buildRule("failedOnCheck", cond = { error("check-fail") }, act = { })
        val successRule = buildRule("successRule", 2, { true }, { })

        val result = engine.check(ruleSetOf(failedRule, successRule), Facts.empty())

        result[failedRule] shouldBeEqualTo false
        result[successRule].shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `check 중 CancellationException 은 삼키지 않고 전파한다`() {
        val engine = createEngine()
        val canceledRule = buildRule(
            "canceledOnCheck",
            cond = { throw CancellationException("cancel-on-check") },
            act = { }
        )

        (assertFailsWith<CancellationException> {
            engine.check(ruleSetOf(canceledRule), Facts.empty())
        }).message shouldBeEqualTo "cancel-on-check"
    }

    @Test
    fun `우선순위 순서대로 Rule 실행`() {
        val engine = createEngine()
        val executionOrder = mutableListOf<String>()

        val rule1 = buildRule("second", 2, { true }, { executionOrder.add("second") })
        val rule2 = buildRule("first", 1, { true }, { executionOrder.add("first") })

        engine.fire(ruleSetOf(rule1, rule2), Facts.empty()); executionOrder shouldBeEqualTo listOf("first", "second")
    }

    @Test
    fun `여러 Action 순차 실행`() {
        val engine = createEngine()
        val rule = DefaultRule(
            name = "multiAction",
            condition = Condition.TRUE,
            actions = listOf(
                Action { it["step1"] = true },
                Action { it["step2"] = true }
            )
        )

        val facts = Facts.empty()
        engine.fire(ruleSetOf(rule), facts)
        facts.get<Boolean>("step1").shouldNotBeNull().shouldBeTrue()
        facts.get<Boolean>("step2").shouldNotBeNull().shouldBeTrue()
    }
}
