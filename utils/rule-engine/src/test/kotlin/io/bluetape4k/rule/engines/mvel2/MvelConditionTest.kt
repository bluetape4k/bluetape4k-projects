package io.bluetape4k.rule.engines.mvel2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class MvelConditionTest {

    companion object : KLogging()

    @Test
    fun `숫자 비교 MVEL 표현식 평가`() {
        val condition = MvelCondition("amount > 1000")
        val facts = Facts.of("amount" to 1500)
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `조건 불만족 시 false 반환`() {
        val condition = MvelCondition("amount > 1000")
        val facts = Facts.of("amount" to 500)
        condition.evaluate(facts).shouldBeFalse()
    }

    @Test
    fun `잘못된 MVEL 표현식은 false 반환`() {
        val condition = MvelCondition("nonExistentVar > 0")
        val facts = Facts.of("amount" to 100)
        condition.evaluate(facts).shouldBeFalse()
    }

    @Test
    fun `AND 논리 연산자 MVEL 표현식`() {
        val condition = MvelCondition("age >= 18 && role == 'admin'")
        val facts = Facts.of("age" to 25, "role" to "admin")
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `OR 논리 연산자 MVEL 표현식`() {
        val condition = MvelCondition("amount > 5000 || premium == true")
        val facts = Facts.of("amount" to 100, "premium" to true)
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `빈 표현식은 true 반환`() {
        val condition = MvelCondition("   ")
        condition.evaluate(Facts.empty()).shouldBeTrue()
    }

    @Test
    fun `MvelCondition equals and hashCode`() {
        val expr = "amount > 1000"
        val c1 = MvelCondition(expr)
        val c2 = MvelCondition(expr)
        (c1 == c2).shouldBeTrue()
        (c1.hashCode() == c2.hashCode()).shouldBeTrue()
    }

    @Test
    fun `MvelCondition toString 표현`() {
        val condition = MvelCondition("x > 0")
        condition.toString() shouldBeEqualTo "MvelCondition(expression='x > 0')"
    }

    @Test
    fun `mvelConditionOf 팩토리 함수`() {
        val condition = mvelConditionOf("x > 10")
        val facts = Facts.of("x" to 20)
        condition.evaluate(facts).shouldBeTrue()
    }
}
