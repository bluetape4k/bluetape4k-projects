package io.bluetape4k.rule.support

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.core.rule
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class UnitRuleGroupTest {

    companion object: KLogging()

    @Test
    fun `모든 Rule이 성공해야 모든 action 실행`() {
        val group = UnitRuleGroup(name = "unit", priority = 1)

        val rule1 = rule {
            name = "rule1"
            condition { true }
            action { facts -> facts["rule1"] = true }
        }
        val rule2 = rule {
            name = "rule2"
            condition { true }
            action { facts -> facts["rule2"] = true }
        }

        group.addRule(rule1)
        group.addRule(rule2)

        val facts = Facts.empty()
        group.evaluate(facts).shouldBeTrue()
        group.execute(facts)
        facts.get<Boolean>("rule1").shouldNotBeNull().shouldBeTrue()
        facts.get<Boolean>("rule2").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `하나라도 실패하면 미실행`() {
        val group = UnitRuleGroup(name = "unit", priority = 1)

        val rule1 = rule {
            name = "rule1"
            condition { true }
            action { facts -> facts["rule1"] = true }
        }
        val rule2 = rule {
            name = "rule2"
            condition { false }
            action { facts -> facts["rule2"] = true }
        }

        group.addRule(rule1)
        group.addRule(rule2)

        val facts = Facts.empty()
        group.evaluate(facts).shouldBeFalse()
    }
}
