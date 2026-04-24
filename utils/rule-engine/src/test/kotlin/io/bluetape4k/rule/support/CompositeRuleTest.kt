package io.bluetape4k.rule.support

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.core.rule
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class CompositeRuleTest {

    companion object : KLogging()

    @Test
    fun `UnitRuleGroup에 Rule 추가 후 실행`() {
        val composite = UnitRuleGroup(name = "composite", priority = 1)

        val rule1 = rule {
            name = "rule1"
            condition { true }
            action { facts -> facts["r1"] = true }
        }
        val rule2 = rule {
            name = "rule2"
            condition { true }
            action { facts -> facts["r2"] = true }
        }

        composite.addRule(rule1)
        composite.addRule(rule2)

        val facts = Facts.empty()
        composite.evaluate(facts).shouldBeTrue()
        composite.execute(facts)

        facts.get<Boolean>("r1").shouldNotBeNull().shouldBeTrue()
        facts.get<Boolean>("r2").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `CompositeRule에서 Rule 제거 후 실행`() {
        val composite = UnitRuleGroup(name = "composite", priority = 1)

        val rule1 = rule {
            name = "rule1"
            condition { true }
            action { facts -> facts["r1"] = true }
        }
        val rule2 = rule {
            name = "rule2"
            condition { true }
            action { facts -> facts["r2"] = true }
        }

        composite.addRule(rule1)
        composite.addRule(rule2)
        composite.removeRule(rule2)

        val facts = Facts.empty()
        composite.evaluate(facts).shouldBeTrue()
        composite.execute(facts)

        facts.get<Boolean>("r1").shouldNotBeNull().shouldBeTrue()
        facts.containsKey("r2").shouldBeFalse()
    }

    @Test
    fun `ActivationRuleGroup는 첫 번째 성공 Rule만 실행하고 중단`() {
        val composite = ActivationRuleGroup(name = "activation", priority = 1)
        val executionOrder = mutableListOf<String>()

        val rule1 = rule {
            name = "rule1"
            priority = 1
            condition { true }
            action { executionOrder.add("rule1") }
        }
        val rule2 = rule {
            name = "rule2"
            priority = 2
            condition { true }
            action { executionOrder.add("rule2") }
        }

        composite.addRule(rule1)
        composite.addRule(rule2)

        val facts = Facts.empty()
        composite.evaluate(facts).shouldBeTrue()
        composite.execute(facts)

        // Only the first matching rule (by priority) should run
        (executionOrder.size == 1).shouldBeTrue()
    }

    @Test
    fun `ConditionalRuleGroup에 어노테이션 기반 Rule 추가`() {
        val composite = ConditionalRuleGroup(name = "conditional", priority = 1)

        val gateRule = rule {
            name = "gate"
            priority = 1
            condition { true }
            action { facts -> facts["gate"] = true }
        }

        composite.addRule(gateRule)

        val facts = Facts.empty()
        composite.evaluate(facts).shouldBeTrue()
        composite.execute(facts)

        facts.get<Boolean>("gate").shouldNotBeNull().shouldBeTrue()
    }

}
