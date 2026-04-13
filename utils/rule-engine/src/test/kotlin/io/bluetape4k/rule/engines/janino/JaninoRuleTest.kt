package io.bluetape4k.rule.engines.janino

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.RuleDefinition
import io.bluetape4k.rule.api.ruleSetOf
import io.bluetape4k.rule.core.DefaultRuleEngine
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class JaninoRuleTest {

    companion object: KLogging()

    @Test
    fun `JaninoCondition 평가 - 조건 만족`() {
        val condition = JaninoCondition(
            "((Integer)facts.get(\"amount\")).intValue() > 1000"
        )
        val facts = Facts.of("amount" to 1500)
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `JaninoCondition 평가 - 조건 불만족 시 false 반환`() {
        val condition = JaninoCondition(
            "((Integer)facts.get(\"amount\")).intValue() > 1000"
        )
        val facts = Facts.of("amount" to 500)
        condition.evaluate(facts).shouldBeFalse()
    }

    @Test
    fun `JaninoCondition 잘못된 표현식은 false 반환`() {
        val condition = JaninoCondition("invalid_expression_xyz")
        val facts = Facts.of("amount" to 1500)
        condition.evaluate(facts).shouldBeFalse()
    }

    @Test
    fun `JaninoAction 실행 - facts에 값 추가`() {
        val action = JaninoAction("facts.put(\"discount\", Boolean.TRUE);")
        val facts = Facts.of("amount" to 1500)

        action.execute(facts)

        facts.get<Boolean>("discount").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `JaninoRule 조건과 액션 실행`() {
        val rule = JaninoRule(name = "discount")
            .whenever("((Integer)facts.get(\"amount\")).intValue() > 1000")
            .then("facts.put(\"discount\", Boolean.TRUE);")

        val facts = Facts.of("amount" to 1500)
        rule.evaluate(facts).shouldBeTrue()
        rule.execute(facts)

        facts.get<Boolean>("discount").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `JaninoRule을 엔진에서 실행`() {
        val engine = DefaultRuleEngine()
        val rule = JaninoRule(name = "discount", priority = 1)
            .whenever("((Integer)facts.get(\"amount\")).intValue() > 1000")
            .then("facts.put(\"discount\", Boolean.TRUE);")

        val facts = Facts.of("amount" to 2000)
        engine.fire(ruleSetOf(rule), facts)

        facts.get<Boolean>("discount").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `Janino 팩토리 함수 사용`() {
        val condition = janinoConditionOf(
            "((Integer)facts.get(\"x\")).intValue() > 10"
        )
        val facts = Facts.of("x" to 20)
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `JaninoRule - 복합 조건식`() {
        val condition = JaninoCondition(
            "((Integer)facts.get(\"age\")).intValue() >= 18 && ((String)facts.get(\"role\")).equals(\"admin\")"
        )
        val facts = Facts.of("age" to 25, "role" to "admin")
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `RuleDefinition으로부터 JaninoRule 빌드`() {
        val definition = RuleDefinition(
            name = "discountRule",
            condition = "((Integer)facts.get(\"amount\")).intValue() > 1000",
            actions = listOf("facts.put(\"discount\", Boolean.TRUE);")
        )
        val rule = definition.toJaninoRule()
        val facts = Facts.of("amount" to 2000)

        rule.evaluate(facts).shouldBeTrue()
        rule.execute(facts)
        facts.get<Boolean>("discount").shouldNotBeNull().shouldBeTrue()
    }
}
