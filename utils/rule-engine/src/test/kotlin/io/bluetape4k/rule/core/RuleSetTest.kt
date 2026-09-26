package io.bluetape4k.rule.core

import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Condition
import io.bluetape4k.rule.api.Rule
import io.bluetape4k.rule.api.RuleSet
import io.bluetape4k.rule.api.ruleSetOf
import org.junit.jupiter.api.Test

class RuleSetTest {

    companion object: KLogging()

    private fun makeRule(name: String, priority: Int = 0): DefaultRule =
        DefaultRule(name = name, priority = priority, condition = Condition.TRUE)

    @Test
    fun `빈 RuleSet 생성`() {
        val ruleSet = RuleSet()
        ruleSet.shouldBeEmpty()
    }

    @Test
    fun `ruleSetOf로 RuleSet 생성`() {
        val rule1 = makeRule("a")
        val rule2 = makeRule("b")
        val ruleSet = ruleSetOf(rule1, rule2)
        ruleSet shouldHaveSize 2
    }

    @Test
    fun `ruleSetOf Collection으로 생성`() {
        val rules = listOf(makeRule("a"), makeRule("b"), makeRule("c"))
        val ruleSet = ruleSetOf(rules)
        ruleSet shouldHaveSize 3
    }

    @Test
    fun `register Rule 추가`() {
        val ruleSet = RuleSet()
        val rule = makeRule("test")
        ruleSet.register(rule)
        ruleSet shouldHaveSize 1
    }

    @Test
    fun `unregister Rule 제거`() {
        val rule1 = makeRule("a")
        val rule2 = makeRule("b")
        val ruleSet = ruleSetOf(rule1, rule2)
        ruleSet.unregister(rule1)
        ruleSet shouldHaveSize 1
    }

    @Test
    fun `unregister by name`() {
        val rule1 = makeRule("alpha")
        val rule2 = makeRule("beta")
        val ruleSet = ruleSetOf(rule1, rule2)
        ruleSet.unregister("alpha")
        ruleSet shouldHaveSize 1
    }

    @Test
    fun `unregister by name case insensitive`() {
        val rule = makeRule("MyRule")
        val ruleSet = ruleSetOf(rule)
        ruleSet.unregister("myrule")
        ruleSet.isEmpty().shouldBeTrue()
    }

    @Test
    fun `clear removes all rules`() {
        val ruleSet = ruleSetOf(makeRule("a"), makeRule("b"), makeRule("c"))
        ruleSet.clear()
        ruleSet.shouldBeEmpty()
    }

    @Test
    fun `rules are sorted by priority`() {
        val rule3 = makeRule("c", priority = 3)
        val rule1 = makeRule("a", priority = 1)
        val rule2 = makeRule("b", priority = 2)
        val ruleSet = ruleSetOf(rule3, rule1, rule2)
        val sorted = ruleSet.toList()
        sorted[0].priority shouldBeEqualTo 1
        sorted[1].priority shouldBeEqualTo 2
        sorted[2].priority shouldBeEqualTo 3
    }

    @Test
    fun `registerProxy adds annotation-based rule`() {
        @io.bluetape4k.rule.annotation.Rule(name = "proxyRule")
        class AnnotatedRule {
            @io.bluetape4k.rule.annotation.Condition
            fun check(): Boolean = true

            @io.bluetape4k.rule.annotation.Action
            fun doSomething() {
            }
        }

        val ruleSet = RuleSet()
        ruleSet.registerProxy(AnnotatedRule())
        ruleSet shouldHaveSize 1
    }

    @Test
    fun `toString includes rule info`() {
        val rule = makeRule("myRule")
        val ruleSet = ruleSetOf(rule)
        val str = ruleSet.toString()
        str shouldContain "myRule"
    }

    @Test
    fun `iterator works correctly`() {
        val rule1 = makeRule("a", 1)
        val rule2 = makeRule("b", 2)
        val ruleSet = ruleSetOf(rule1, rule2)
        val collected = mutableListOf<Rule>()
        for (rule in ruleSet) {
            collected.add(rule)
        }
        collected shouldHaveSize 2
    }
}
