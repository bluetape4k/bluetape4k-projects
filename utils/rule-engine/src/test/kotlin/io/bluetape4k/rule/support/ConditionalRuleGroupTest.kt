package io.bluetape4k.rule.support

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.core.rule
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class ConditionalRuleGroupTest {

    companion object: KLogging()

    @Test
    fun `최상위 우선순위 Rule 성공 시 나머지 실행`() {
        val group = ConditionalRuleGroup(name = "conditional", priority = 1)

        val gateRule = rule {
            name = "gate"
            priority = 1
            condition { true }
            action { facts -> facts["gate"] = true }
        }
        val followRule = rule {
            name = "follow"
            priority = 2
            condition { true }
            action { facts -> facts["follow"] = true }
        }

        group.addRule(gateRule)
        group.addRule(followRule)

        val facts = Facts.empty()
        group.evaluate(facts).shouldBeTrue()
        group.execute(facts)
        facts.get<Boolean>("gate").shouldNotBeNull().shouldBeTrue()
        facts.get<Boolean>("follow").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `최상위 우선순위 Rule 실패 시 모든 Rule 미실행`() {
        val group = ConditionalRuleGroup(name = "conditional", priority = 1)

        val gateRule = rule {
            name = "gate"
            priority = 1
            condition { false }
            action { facts -> facts["gate"] = true }
        }
        val followRule = rule {
            name = "follow"
            priority = 2
            condition { true }
            action { facts -> facts["follow"] = true }
        }

        group.addRule(gateRule)
        group.addRule(followRule)

        val facts = Facts.empty()
        group.evaluate(facts).shouldBeFalse()
    }
}
