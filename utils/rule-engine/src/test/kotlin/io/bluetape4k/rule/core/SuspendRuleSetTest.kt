package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.SuspendCondition
import io.bluetape4k.rule.api.SuspendRule
import io.bluetape4k.rule.api.SuspendRuleSet
import io.bluetape4k.rule.api.suspendRuleSetOf
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class SuspendRuleSetTest {

    companion object: KLogging()

    private fun makeRule(name: String, priority: Int = 0): DefaultSuspendRule =
        DefaultSuspendRule(name = name, priority = priority, condition = SuspendCondition.TRUE)

    @Test
    fun `빈 SuspendRuleSet 생성`() {
        val ruleSet = SuspendRuleSet()
        ruleSet.isEmpty().shouldBeTrue()
        ruleSet.size shouldBeEqualTo 0
    }

    @Test
    fun `suspendRuleSetOf로 생성`() {
        val rule1 = makeRule("a")
        val rule2 = makeRule("b")
        val ruleSet = suspendRuleSetOf(rule1, rule2)
        ruleSet.size shouldBeEqualTo 2
        ruleSet.isNotEmpty().shouldBeTrue()
    }

    @Test
    fun `suspendRuleSetOf Collection으로 생성`() {
        val rules = listOf(makeRule("a"), makeRule("b"))
        val ruleSet = suspendRuleSetOf(rules)
        ruleSet.size shouldBeEqualTo 2
    }

    @Test
    fun `register SuspendRule 추가`() {
        val ruleSet = SuspendRuleSet()
        val rule = makeRule("test")
        ruleSet.register(rule)
        ruleSet.size shouldBeEqualTo 1
    }

    @Test
    fun `unregister SuspendRule 제거`() {
        val rule1 = makeRule("a")
        val rule2 = makeRule("b")
        val ruleSet = suspendRuleSetOf(rule1, rule2)
        ruleSet.unregister(rule1)
        ruleSet.size shouldBeEqualTo 1
    }

    @Test
    fun `clear removes all rules`() {
        val ruleSet = suspendRuleSetOf(makeRule("a"), makeRule("b"))
        ruleSet.clear()
        ruleSet.isEmpty().shouldBeTrue()
    }

    @Test
    fun `rules are sorted by priority`() {
        val rule3 = makeRule("c", priority = 3)
        val rule1 = makeRule("a", priority = 1)
        val rule2 = makeRule("b", priority = 2)
        val ruleSet = suspendRuleSetOf(rule3, rule1, rule2)
        val sorted = ruleSet.toList()
        sorted[0].priority shouldBeEqualTo 1
        sorted[1].priority shouldBeEqualTo 2
        sorted[2].priority shouldBeEqualTo 3
    }

    @Test
    fun `toString contains rule info`() {
        val rule = makeRule("asyncRule")
        val ruleSet = suspendRuleSetOf(rule)
        val str = ruleSet.toString()
        str.contains("asyncRule").shouldBeTrue()
    }

    @Test
    fun `iterator works`() {
        val rules = listOf(makeRule("a", 1), makeRule("b", 2))
        val ruleSet = suspendRuleSetOf(rules)
        val collected = mutableListOf<SuspendRule>()
        for (rule in ruleSet) {
            collected.add(rule)
        }
        collected.size shouldBeEqualTo 2
    }
}
