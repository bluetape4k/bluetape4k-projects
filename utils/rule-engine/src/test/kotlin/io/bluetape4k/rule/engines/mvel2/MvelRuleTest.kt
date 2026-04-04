package io.bluetape4k.rule.engines.mvel2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.ruleSetOf
import io.bluetape4k.rule.core.DefaultRuleEngine
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class MvelRuleTest {

    companion object: KLogging()

    @Test
    fun `MvelCondition 평가`() {
        val condition = MvelCondition("amount > 1000")
        val facts = Facts.of("amount" to 1500)
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `MvelCondition 실패 시 false 반환`() {
        val condition = MvelCondition("amount > 1000")
        val facts = Facts.of("amount" to 500)
        condition.evaluate(facts).shouldBeFalse()
    }

    @Test
    fun `MvelRule 조건과 액션 실행`() {
        val rule = MvelRule(name = "discount")
            .whenever("amount > 1000")
            .then("discount = true")

        val facts = Facts.of("amount" to 1500)
        rule.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `MvelRule을 엔진에서 실행`() {
        val engine = DefaultRuleEngine()
        val rule = MvelRule(name = "discount", priority = 1)
            .whenever("amount > 1000")
            .then("discount = true")

        val facts = Facts.of("amount" to 2000)
        engine.fire(ruleSetOf(rule), facts)
    }

    @Test
    fun `MvelRule 팩토리 함수 사용`() {
        val condition = mvelConditionOf("x > 10")
        val action = mvelActionOf("result = x * 2")

        val facts = Facts.of("x" to 20)
        condition.evaluate(facts).shouldBeTrue()
    }
}
