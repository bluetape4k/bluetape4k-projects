package io.bluetape4k.rule.engines.janino

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.RuleDefinition
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class JaninoSupportTest {

    companion object : KLogging()

    private val amountGt1000 = "((Integer)facts.get(\"amount\")).intValue() > 1000"
    private val putDiscountTrue = "facts.put(\"discount\", Boolean.TRUE);"

    @Test
    fun `janinoConditionOf 팩토리 함수로 JaninoCondition 생성`() {
        val condition = janinoConditionOf(amountGt1000)
        val facts = Facts.of("amount" to 1500)
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `janinoActionOf 팩토리 함수로 JaninoAction 생성`() {
        val action = janinoActionOf(putDiscountTrue)
        val facts = Facts.of("amount" to 1500)
        action.execute(facts)
        facts.get<Boolean>("discount").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `RuleDefinition toJaninoRule로 JaninoRule 빌드`() {
        val definition = RuleDefinition(
            name = "discountRule",
            description = "할인 규칙",
            priority = 1,
            condition = amountGt1000,
            actions = listOf(putDiscountTrue)
        )
        val rule = definition.toJaninoRule()
        rule.name shouldBeEqualTo "discountRule"

        val facts = Facts.of("amount" to 2000)
        rule.evaluate(facts).shouldBeTrue()
        rule.execute(facts)
        facts.get<Boolean>("discount").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `toJaninoRule 여러 액션 포함`() {
        val definition = RuleDefinition(
            name = "multiAction",
            condition = amountGt1000,
            actions = listOf(
                putDiscountTrue,
                "facts.put(\"processed\", Boolean.TRUE);"
            )
        )
        val rule = definition.toJaninoRule()
        val facts = Facts.of("amount" to 2000)
        rule.evaluate(facts).shouldBeTrue()
        rule.execute(facts)
        facts.get<Boolean>("discount").shouldNotBeNull().shouldBeTrue()
        facts.get<Boolean>("processed").shouldNotBeNull().shouldBeTrue()
    }
}
