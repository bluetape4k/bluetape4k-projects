package io.bluetape4k.rule.engines.spel

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.RuleDefinition
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class SpelSupportTest {

    companion object : KLogging()

    @Test
    fun `spelConditionOf 팩토리 함수로 SpelCondition 생성`() {
        val condition = spelConditionOf("#amount > 1000")
        val facts = Facts.of("amount" to 1500)
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `spelActionOf 팩토리 함수로 SpelAction 생성`() {
        // SpelAction evaluates expression; just verify it doesn't throw
        val action = spelActionOf("#amount")
        val facts = Facts.of("amount" to 100)
        action.execute(facts)
    }

    @Test
    fun `RuleDefinition toSpelRule로 SpelRule 빌드`() {
        val definition = RuleDefinition(
            name = "amountCheck",
            description = "금액 확인",
            priority = 1,
            condition = "#amount > 1000",
            actions = listOf("#amount")
        )
        val rule = definition.toSpelRule()
        rule.name shouldBeEqualTo "amountCheck"

        val facts = Facts.of("amount" to 2000)
        rule.evaluate(facts).shouldBeTrue()
        // execute does not throw
        rule.execute(facts)
    }

    @Test
    fun `toSpelRule 조건 불만족 시 false 반환`() {
        val definition = RuleDefinition(
            name = "check",
            condition = "#value > 100",
            actions = listOf("#value")
        )
        val rule = definition.toSpelRule()
        val facts = Facts.of("value" to 50)
        rule.evaluate(facts) shouldBeEqualTo false
    }
}
