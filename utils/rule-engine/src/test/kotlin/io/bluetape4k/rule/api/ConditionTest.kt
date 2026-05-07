package io.bluetape4k.rule.api

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class ConditionTest {

    companion object : KLogging()

    @Test
    fun `Condition TRUE 항상 true 반환`() {
        Condition.TRUE.evaluate(Facts.empty()).shouldBeTrue()
        Condition.TRUE.evaluate(Facts.of("x" to 1)).shouldBeTrue()
    }

    @Test
    fun `Condition FALSE 항상 false 반환`() {
        Condition.FALSE.evaluate(Facts.empty()).shouldBeFalse()
        Condition.FALSE.evaluate(Facts.of("x" to 1)).shouldBeFalse()
    }

    @Test
    fun `fun interface Condition 람다로 생성`() {
        val condition = Condition { facts -> facts.get<Int>("age")!! >= 18 }
        condition.evaluate(Facts.of("age" to 20)).shouldBeTrue()
        condition.evaluate(Facts.of("age" to 15)).shouldBeFalse()
    }

    @Test
    fun `Condition facts 값 기반 평가`() {
        val condition = Condition { facts ->
            val amount = facts.get<Int>("amount") ?: 0
            val premium = facts.get<Boolean>("premium") ?: false
            amount > 1000 || premium
        }
        condition.evaluate(Facts.of("amount" to 500, "premium" to true)).shouldBeTrue()
        condition.evaluate(Facts.of("amount" to 2000, "premium" to false)).shouldBeTrue()
        condition.evaluate(Facts.of("amount" to 500, "premium" to false)).shouldBeFalse()
    }
}
