package io.bluetape4k.rule.engines.groovy

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.RuleDefinition
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

class GroovySupportTest {

    companion object : KLogging()

    @Test
    fun `groovyConditionOf 팩토리 함수로 GroovyCondition 생성`() {
        val condition = groovyConditionOf("amount > 1000")
        val facts = Facts.of("amount" to 1500)
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `groovyActionOf 팩토리 함수로 GroovyAction 생성`() {
        val action = groovyActionOf("discount = true")
        val facts = Facts.of("amount" to 1500)
        action.execute(facts)
        facts.get<Boolean>("discount").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `RuleDefinition toGroovyRule로 GroovyRule 빌드`() {
        val definition = RuleDefinition(
            name = "discountRule",
            description = "할인 규칙",
            priority = 1,
            condition = "amount > 1000",
            actions = listOf("discount = true")
        )
        val rule = definition.toGroovyRule()
        rule.name shouldBeEqualTo "discountRule"

        val facts = Facts.of("amount" to 2000)
        rule.evaluate(facts).shouldBeTrue()
        rule.execute(facts)
        facts.get<Boolean>("discount").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `toGroovyRule 여러 Action 포함`() {
        val definition = RuleDefinition(
            name = "multiAction",
            condition = "value > 0",
            actions = listOf("a = true", "b = 42")
        )
        val rule = definition.toGroovyRule()

        val facts = Facts.of("value" to 10)
        rule.evaluate(facts).shouldBeTrue()
        rule.execute(facts)
        facts.get<Boolean>("a").shouldNotBeNull().shouldBeTrue()
        facts.get<Number>("b")?.toInt() shouldBeEqualTo 42
    }
}
