package io.bluetape4k.rule.engines.mvel2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.RuleDefinition
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class MvelSupportTest {

    companion object : KLogging()

    @Test
    fun `mvelConditionOf 팩토리 함수로 MvelCondition 생성`() {
        val condition = mvelConditionOf("amount > 1000")
        val facts = Facts.of("amount" to 1500)
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `mvelActionOf 팩토리 함수로 MvelAction 생성`() {
        // MvelAction passes an immutable copy of facts to MVEL; mutations don't propagate back
        val action = mvelActionOf("discount = true")
        val facts = Facts.of("amount" to 1500)
        action.execute(facts) // should not throw
    }

    @Test
    fun `RuleDefinition toMvelRule로 MvelRule 빌드`() {
        val definition = RuleDefinition(
            name = "discountRule",
            description = "할인 규칙",
            priority = 1,
            condition = "amount > 1000",
            actions = listOf("discount = true")
        )
        val rule = definition.toMvelRule()
        rule.name shouldBeEqualTo "discountRule"

        val facts = Facts.of("amount" to 2000)
        rule.evaluate(facts).shouldBeTrue()
        rule.execute(facts) // should not throw
    }

    @Test
    fun `toMvelRule 여러 액션 포함`() {
        val definition = RuleDefinition(
            name = "multiAction",
            condition = "value > 0",
            actions = listOf("a = true", "b = value * 2")
        )
        val rule = definition.toMvelRule()
        val facts = Facts.of("value" to 5)
        rule.evaluate(facts).shouldBeTrue()
        rule.execute(facts) // should not throw
    }
}
