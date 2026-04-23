package io.bluetape4k.rule.api

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.ruleSetOf
import io.bluetape4k.rule.core.DefaultRule
import io.bluetape4k.rule.core.DefaultRuleEngine
import io.bluetape4k.rule.api.Action
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RuleEngineConfigTest {

    companion object : KLogging()

    @Test
    fun `기본 RuleEngineConfig 설정값 확인`() {
        val config = RuleEngineConfig.DEFAULT
        config.skipOnFirstAppliedRule.shouldBeFalse()
        config.skipOnFirstFailedRule.shouldBeFalse()
        config.skipOnFirstNonTriggeredRule.shouldBeFalse()
        config.priorityThreshold shouldBeEqualTo Int.MAX_VALUE
    }

    @Test
    fun `RuleEngineConfig 커스텀 설정`() {
        val config = RuleEngineConfig(
            skipOnFirstAppliedRule = true,
            skipOnFirstFailedRule = true,
            skipOnFirstNonTriggeredRule = true,
            priorityThreshold = 100
        )
        config.skipOnFirstAppliedRule.shouldBeTrue()
        config.skipOnFirstFailedRule.shouldBeTrue()
        config.skipOnFirstNonTriggeredRule.shouldBeTrue()
        config.priorityThreshold shouldBeEqualTo 100
    }

    @Test
    fun `priorityThreshold 음수 설정 시 예외 발생`() {
        assertThrows<IllegalArgumentException> {
            RuleEngineConfig(priorityThreshold = -1)
        }
    }

    @Test
    fun `RuleEngineConfig equals and hashCode`() {
        val c1 = RuleEngineConfig(skipOnFirstAppliedRule = true)
        val c2 = RuleEngineConfig(skipOnFirstAppliedRule = true)
        (c1 == c2).shouldBeTrue()
        (c1.hashCode() == c2.hashCode()).shouldBeTrue()
    }

    @Test
    fun `skipOnFirstAppliedRule 설정 시 두 번째 Rule 건너뜀`() {
        val config = RuleEngineConfig(skipOnFirstAppliedRule = true)
        val engine = DefaultRuleEngine(config)
        val executionCount = mutableListOf<String>()

        val rule1 = DefaultRule(
            name = "rule1",
            priority = 1,
            condition = Condition.TRUE,
            actions = listOf(Action { executionCount.add("rule1") })
        )
        val rule2 = DefaultRule(
            name = "rule2",
            priority = 2,
            condition = Condition.TRUE,
            actions = listOf(Action { executionCount.add("rule2") })
        )

        engine.fire(ruleSetOf(rule1, rule2), Facts.empty())
        // With skipOnFirstAppliedRule=true, only rule1 should execute
        executionCount.size shouldBeEqualTo 1
        executionCount[0] shouldBeEqualTo "rule1"
    }
}
