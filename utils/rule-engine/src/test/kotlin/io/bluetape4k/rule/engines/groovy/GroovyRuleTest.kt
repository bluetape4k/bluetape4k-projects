package io.bluetape4k.rule.engines.groovy

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.RuleDefinition
import io.bluetape4k.rule.api.ruleSetOf
import io.bluetape4k.rule.core.DefaultRuleEngine
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class GroovyRuleTest {

    companion object: KLogging()

    @Test
    fun `GroovyCondition 평가 - 조건 만족`() {
        val condition = GroovyCondition("amount > 1000")
        val facts = Facts.of("amount" to 1500)
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `GroovyCondition 평가 - 조건 불만족 시 false 반환`() {
        val condition = GroovyCondition("amount > 1000")
        val facts = Facts.of("amount" to 500)
        condition.evaluate(facts).shouldBeFalse()
    }

    @Test
    fun `GroovyCondition 잘못된 표현식은 false 반환`() {
        val condition = GroovyCondition("nonExistentVar > 0")
        val facts = Facts.of("amount" to 1500)
        condition.evaluate(facts).shouldBeFalse()
    }

    @Test
    fun `GroovyAction 실행 - facts에 값 추가`() {
        val action = GroovyAction("discount = true")
        val facts = Facts.of("amount" to 1500)

        action.execute(facts)

        facts.get<Boolean>("discount").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `GroovyAction 실행 - 계산 결과 저장`() {
        val action = GroovyAction("result = amount * 0.1")
        val facts = Facts.of("amount" to 2000)

        action.execute(facts)

        val result = facts.get<Number>("result")
        result.shouldNotBeNull()
        result.toDouble() shouldBeEqualTo 200.0
    }

    @Test
    fun `GroovyRule 조건과 액션 실행`() {
        val rule = GroovyRule(name = "discount")
            .whenever("amount > 1000")
            .then("discount = true")

        val facts = Facts.of("amount" to 1500)
        rule.evaluate(facts).shouldBeTrue()
        rule.execute(facts)

        facts.get<Boolean>("discount").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `GroovyRule을 엔진에서 실행`() {
        val engine = DefaultRuleEngine()
        val rule = GroovyRule(name = "discount", priority = 1)
            .whenever("amount > 1000")
            .then("discount = true")

        val facts = Facts.of("amount" to 2000)
        engine.fire(ruleSetOf(rule), facts)

        facts.get<Boolean>("discount").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `Groovy 팩토리 함수 사용`() {
        val condition = groovyConditionOf("x > 10")
        val facts = Facts.of("x" to 20)
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `GroovyRule - 복합 조건식`() {
        val condition = GroovyCondition("age >= 18 && role == 'admin'")
        val facts = Facts.of("age" to 25, "role" to "admin")
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `GroovyRule - 클로저 활용 액션`() {
        val action = GroovyAction(
            """
            def items = amount > 2000 ? ['gold', 'silver'] : ['bronze']
            tier = items[0]
            """.trimIndent()
        )
        val facts = Facts.of("amount" to 3000)
        action.execute(facts)

        facts.get<String>("tier") shouldBe "gold"
    }

    @Test
    fun `GroovyRule - 문자열 처리`() {
        val condition = GroovyCondition("name?.toUpperCase() == 'ALICE'")
        val facts = Facts.of("name" to "alice")
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `RuleDefinition으로부터 GroovyRule 빌드`() {
        val definition = RuleDefinition(
            name = "discountRule",
            condition = "amount > 1000",
            actions = listOf("discount = true")
        )
        val rule = definition.toGroovyRule()
        val facts = Facts.of("amount" to 2000)

        rule.evaluate(facts).shouldBeTrue()
        rule.execute(facts)
        facts.get<Boolean>("discount").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `GroovyRule - 여러 액션 순차 실행`() {
        val rule = GroovyRule(name = "multi-action")
            .whenever("amount > 1000")
            .then("discount = true")
            .then("discountRate = amount > 5000 ? 20 : 10")

        val facts = Facts.of("amount" to 3000)
        rule.evaluate(facts).shouldBeTrue()
        rule.execute(facts)

        facts.get<Boolean>("discount").shouldNotBeNull().shouldBeTrue()
        facts.get<Number>("discountRate")?.toInt() shouldBeEqualTo 10
    }
}
