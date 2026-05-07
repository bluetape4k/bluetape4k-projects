package io.bluetape4k.rule.engines.spel

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class SpelConditionTest {

    companion object : KLogging()

    @Test
    fun `숫자 비교 SpEL 표현식 평가`() {
        val condition = SpelCondition("#amount > 1000")
        val facts = Facts.of("amount" to 1500)
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `조건 불만족 시 false 반환`() {
        val condition = SpelCondition("#amount > 1000")
        val facts = Facts.of("amount" to 500)
        condition.evaluate(facts).shouldBeFalse()
    }

    @Test
    fun `잘못된 SpEL 표현식은 false 반환`() {
        val condition = SpelCondition("#nonExistentVar > 0")
        val facts = Facts.of("amount" to 100)
        condition.evaluate(facts).shouldBeFalse()
    }

    @Test
    fun `문자열 비교 SpEL 표현식 평가`() {
        val condition = SpelCondition("#role == 'admin'")
        val facts = Facts.of("role" to "admin")
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `AND 논리 연산자 SpEL 표현식`() {
        val condition = SpelCondition("#age >= 18 && #role == 'user'")
        val facts = Facts.of("age" to 25, "role" to "user")
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `OR 논리 연산자 SpEL 표현식`() {
        val condition = SpelCondition("#amount > 5000 || #premium == true")
        val facts = Facts.of("amount" to 100, "premium" to true)
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `SpelCondition equals and hashCode`() {
        val expr = "#amount > 1000"
        val c1 = SpelCondition(expr)
        val c2 = SpelCondition(expr)
        (c1 == c2).shouldBeTrue()
        (c1.hashCode() == c2.hashCode()).shouldBeTrue()
    }

    @Test
    fun `SpelCondition toString 표현`() {
        val condition = SpelCondition("#x > 0")
        condition.toString() shouldBeEqualTo "SpelCondition(expression='#x > 0')"
    }

    @Test
    fun `spelConditionOf 팩토리 함수`() {
        val condition = spelConditionOf("#x > 10")
        val facts = Facts.of("x" to 20)
        condition.evaluate(facts).shouldBeTrue()
    }
}
