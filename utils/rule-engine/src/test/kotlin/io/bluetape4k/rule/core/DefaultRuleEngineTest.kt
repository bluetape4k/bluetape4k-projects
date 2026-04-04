package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Action
import io.bluetape4k.rule.api.Condition
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.RuleEngineConfig
import io.bluetape4k.rule.api.ruleSetOf
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.junit.jupiter.api.Test

class DefaultRuleEngineTest {

    companion object: KLogging()

    private fun createEngine(config: RuleEngineConfig = RuleEngineConfig.DEFAULT): DefaultRuleEngine {
        return DefaultRuleEngine(config)
    }

    @Test
    fun `기본 설정으로 엔진 생성`() {
        val engine = createEngine()
        engine.config shouldBeEqualTo RuleEngineConfig.DEFAULT
    }

    @Test
    fun `단일 Rule 실행`() {
        val engine = createEngine()
        val rule = rule {
            name = "test"
            condition { true }
            action { facts -> facts["executed"] = true }
        }
        val facts = Facts.empty()
        engine.fire(ruleSetOf(rule), facts)
        facts.get<Boolean>("executed") shouldBeEqualTo true
    }

    @Test
    fun `조건 불만족 시 Rule 미실행`() {
        val engine = createEngine()
        val rule = rule {
            name = "test"
            condition { false }
            action { facts -> facts["executed"] = true }
        }
        val facts = Facts.empty()
        engine.fire(ruleSetOf(rule), facts)
        facts.containsKey("executed").shouldBeFalse()
    }

    @Test
    fun `skipOnFirstAppliedRule 옵션 동작`() {
        val config = RuleEngineConfig(skipOnFirstAppliedRule = true)
        val engine = createEngine(config)

        val rule1 = rule {
            name = "rule1"
            priority = 1
            condition { true }
            action { facts -> facts["rule1"] = true }
        }
        val rule2 = rule {
            name = "rule2"
            priority = 2
            condition { true }
            action { facts -> facts["rule2"] = true }
        }

        val facts = Facts.empty()
        engine.fire(ruleSetOf(rule1, rule2), facts)
        facts.get<Boolean>("rule1") shouldBeEqualTo true
        facts.containsKey("rule2").shouldBeFalse()
    }

    @Test
    fun `skipOnFirstFailedRule 옵션 동작`() {
        val config = RuleEngineConfig(skipOnFirstFailedRule = true)
        val engine = createEngine(config)

        val rule1 = rule {
            name = "failRule"
            priority = 1
            condition { true }
            action { error("fail!") }
        }
        val rule2 = rule {
            name = "rule2"
            priority = 2
            condition { true }
            action { facts -> facts["rule2"] = true }
        }

        val facts = Facts.empty()
        engine.fire(ruleSetOf(rule1, rule2), facts)
        facts.containsKey("rule2").shouldBeFalse()
    }

    @Test
    fun `skipOnFirstNonTriggeredRule 옵션 동작`() {
        val config = RuleEngineConfig(skipOnFirstNonTriggeredRule = true)
        val engine = createEngine(config)

        val rule1 = rule {
            name = "falseRule"
            priority = 1
            condition { false }
            action { facts -> facts["rule1"] = true }
        }
        val rule2 = rule {
            name = "rule2"
            priority = 2
            condition { true }
            action { facts -> facts["rule2"] = true }
        }

        val facts = Facts.empty()
        engine.fire(ruleSetOf(rule1, rule2), facts)
        facts.containsKey("rule1").shouldBeFalse()
        facts.containsKey("rule2").shouldBeFalse()
    }

    @Test
    fun `priorityThreshold 초과 Rule 무시`() {
        val config = RuleEngineConfig(priorityThreshold = 5)
        val engine = createEngine(config)

        val rule1 = rule {
            name = "low"
            priority = 1
            condition { true }
            action { facts -> facts["low"] = true }
        }
        val rule2 = rule {
            name = "high"
            priority = 10
            condition { true }
            action { facts -> facts["high"] = true }
        }

        val facts = Facts.empty()
        engine.fire(ruleSetOf(rule1, rule2), facts)
        facts.get<Boolean>("low") shouldBeEqualTo true
        facts.containsKey("high").shouldBeFalse()
    }

    @Test
    fun `check 메서드로 Rule 평가`() {
        val engine = createEngine()
        val rule1 = rule {
            name = "trueRule"
            condition { true }
            action { }
        }
        val rule2 = rule {
            name = "falseRule"
            condition { false }
            action { }
        }

        val facts = Facts.empty()
        val result = engine.check(ruleSetOf(rule1, rule2), facts)
        result[rule1] shouldBeEqualTo true
        result[rule2] shouldBeEqualTo false
    }

    @Test
    fun `우선순위 순서대로 Rule 실행`() {
        val engine = createEngine()
        val executionOrder = mutableListOf<String>()

        val rule1 = rule {
            name = "second"
            priority = 2
            condition { true }
            action { executionOrder.add("second") }
        }
        val rule2 = rule {
            name = "first"
            priority = 1
            condition { true }
            action { executionOrder.add("first") }
        }

        engine.fire(ruleSetOf(rule1, rule2), Facts.empty())
        executionOrder shouldBeEqualTo listOf("first", "second")
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
        facts.get<Boolean>("step1") shouldBeEqualTo true
        facts.get<Boolean>("step2") shouldBeEqualTo true
    }
}
