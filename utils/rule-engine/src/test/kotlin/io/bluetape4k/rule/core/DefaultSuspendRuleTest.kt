package io.bluetape4k.rule.core

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.SuspendAction
import io.bluetape4k.rule.api.SuspendCondition
import io.bluetape4k.rule.api.suspendRuleSetOf
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

class DefaultSuspendRuleTest {

    companion object: KLogging()

    @Test
    fun `기본 생성 시 이름과 우선순위 기본값 확인`() {
        val rule = DefaultSuspendRule()
        rule.name shouldBeEqualTo io.bluetape4k.rule.DEFAULT_RULE_NAME
        rule.description shouldBeEqualTo io.bluetape4k.rule.DEFAULT_RULE_DESCRIPTION
        rule.priority shouldBeEqualTo io.bluetape4k.rule.DEFAULT_RULE_PRIORITY
    }

    @Test
    fun `evaluate FALSE condition`() = runSuspendIO {
        val rule = DefaultSuspendRule(condition = SuspendCondition.FALSE)
        val facts = Facts.empty()
        rule.evaluate(facts).shouldBeFalse()
    }

    @Test
    fun `evaluate TRUE condition`() = runSuspendIO {
        val rule = DefaultSuspendRule(condition = SuspendCondition.TRUE)
        val facts = Facts.empty()
        rule.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `execute runs all actions in order`() = runSuspendIO {
        val order = mutableListOf<Int>()
        val rule = DefaultSuspendRule(
            name = "ordered",
            condition = SuspendCondition.TRUE,
            actions = listOf(
                SuspendAction { order.add(1) },
                SuspendAction { order.add(2) },
                SuspendAction { order.add(3) }
            )
        )
        rule.execute(Facts.empty())
        order shouldBeEqualTo listOf(1, 2, 3)
    }

    @Test
    fun `execute writes to facts`() = runSuspendIO {
        val rule = DefaultSuspendRule(
            name = "writeRule",
            condition = SuspendCondition.TRUE,
            actions = listOf(SuspendAction { facts -> facts["result"] = "done" })
        )
        val facts = Facts.empty()
        rule.execute(facts)
        facts.get<String>("result") shouldBeEqualTo "done"
    }

    @Test
    fun `equals based on name`() {
        val rule1 = DefaultSuspendRule(name = "sameName")
        val rule2 = DefaultSuspendRule(name = "sameName")
        val rule3 = DefaultSuspendRule(name = "different")
        (rule1 == rule2).shouldBeTrue()
        (rule1 == rule3).shouldBeFalse()
    }

    @Test
    fun `hashCode based on name`() {
        val rule1 = DefaultSuspendRule(name = "sameName")
        val rule2 = DefaultSuspendRule(name = "sameName")
        (rule1.hashCode() == rule2.hashCode()).shouldBeTrue()
    }

    @Test
    fun `toString contains name and priority`() {
        val rule = DefaultSuspendRule(name = "myRule", priority = 5)
        val str = rule.toString()
        str.contains("myRule").shouldBeTrue()
        str.contains("5").shouldBeTrue()
    }

    @Test
    fun `suspendRule DSL 빌더로 생성`() = runSuspendIO {
        val rule = suspendRule {
            name = "dslRule"
            description = "DSL 생성 규칙"
            priority = 1
            condition { facts -> facts.get<Int>("score")!! >= 60 }
            action { facts -> facts["passed"] = true }
        }
        rule.name shouldBeEqualTo "dslRule"
        rule.priority shouldBeEqualTo 1

        val facts = Facts.of("score" to 80)
        rule.evaluate(facts).shouldBeTrue()
        rule.execute(facts)
        facts.get<Boolean>("passed").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `suspendRule DSL false condition`() = runSuspendIO {
        val rule = suspendRule {
            name = "failRule"
            condition { facts -> facts.get<Int>("score")!! >= 60 }
            action { facts -> facts["passed"] = true }
        }

        val facts = Facts.of("score" to 30)
        rule.evaluate(facts).shouldBeFalse()
    }

    @Test
    fun `suspendRuleSetOf and iteration`() = runSuspendIO {
        val rule1 = suspendRule { name = "r1"; priority = 1; condition { true }; action {} }
        val rule2 = suspendRule { name = "r2"; priority = 2; condition { true }; action {} }
        val ruleSet = suspendRuleSetOf(rule1, rule2)

        val names = ruleSet.map { it.name }
        names shouldBeEqualTo listOf("r1", "r2")
    }
}
