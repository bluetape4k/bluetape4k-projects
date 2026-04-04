package io.bluetape4k.rule.engines.spel

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.ruleSetOf
import io.bluetape4k.rule.core.DefaultRuleEngine
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class SpelRuleTest {

    companion object: KLogging()

    @Test
    fun `SpelCondition 평가 (변수 참조)`() {
        val condition = SpelCondition("#amount > 1000")
        val facts = Facts.of("amount" to 1500)
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `SpelCondition 실패 시 false 반환`() {
        val condition = SpelCondition("#amount > 1000")
        val facts = Facts.of("amount" to 500)
        condition.evaluate(facts).shouldBeFalse()
    }

    @Test
    fun `SpelRule 조건과 액션 설정`() {
        val rule = SpelRule(name = "discount")
            .whenever("#amount > 1000")

        val facts = Facts.of("amount" to 1500)
        rule.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `SpelRule을 엔진에서 실행`() {
        val engine = DefaultRuleEngine()
        val rule = SpelRule(name = "test", priority = 1)
            .whenever("#value > 0")

        val facts = Facts.of("value" to 10)
        engine.fire(ruleSetOf(rule), facts)
    }

    @Test
    fun `SpelRule 팩토리 함수 사용`() {
        val condition = spelConditionOf("#x > 10")
        val facts = Facts.of("x" to 20)
        condition.evaluate(facts).shouldBeTrue()
    }
}
