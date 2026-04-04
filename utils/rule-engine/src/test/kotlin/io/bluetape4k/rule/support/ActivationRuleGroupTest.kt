package io.bluetape4k.rule.support

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.core.rule
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class ActivationRuleGroupTest {

    companion object: KLogging()

    @Test
    fun `첫 번째 성공 Rule만 실행`() {
        val group = ActivationRuleGroup(name = "activation", priority = 1)

        val rule1 = rule {
            name = "rule1"
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

        group.addRule(rule1)
        group.addRule(rule2)

        val facts = Facts.empty()
        group.evaluate(facts).shouldBeTrue()
        group.execute(facts)
        facts.containsKey("rule1").shouldBeFalse()
        facts.get<Boolean>("rule2") shouldBeEqualTo true
    }

    @Test
    fun `모든 Rule이 실패하면 미실행`() {
        val group = ActivationRuleGroup(name = "activation", priority = 1)

        val rule = rule {
            name = "failRule"
            condition { false }
            action { facts -> facts["executed"] = true }
        }
        group.addRule(rule)

        val facts = Facts.empty()
        group.evaluate(facts).shouldBeFalse()
    }
}
