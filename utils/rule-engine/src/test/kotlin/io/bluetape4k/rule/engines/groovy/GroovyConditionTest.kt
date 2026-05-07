package io.bluetape4k.rule.engines.groovy

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class GroovyConditionTest {

    companion object : KLogging()

    @Test
    fun `숫자 비교 Groovy 표현식 평가`() {
        val condition = GroovyCondition("amount > 1000")
        val facts = Facts.of("amount" to 1500)
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `조건 불만족 시 false 반환`() {
        val condition = GroovyCondition("amount > 1000")
        val facts = Facts.of("amount" to 500)
        condition.evaluate(facts).shouldBeFalse()
    }

    @Test
    fun `잘못된 Groovy 표현식은 false 반환`() {
        val condition = GroovyCondition("nonExistentVar > 0")
        val facts = Facts.of("amount" to 100)
        condition.evaluate(facts).shouldBeFalse()
    }

    @Test
    fun `문자열 비교 Groovy 표현식`() {
        val condition = GroovyCondition("role == 'admin'")
        val facts = Facts.of("role" to "admin")
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `AND 논리 연산자 Groovy 표현식`() {
        val condition = GroovyCondition("age >= 18 && role == 'admin'")
        val facts = Facts.of("age" to 25, "role" to "admin")
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `OR 논리 연산자 Groovy 표현식`() {
        val condition = GroovyCondition("amount > 5000 || premium == true")
        val facts = Facts.of("amount" to 100, "premium" to true)
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `null-safe 연산자 Groovy 표현식`() {
        val condition = GroovyCondition("name?.toUpperCase() == 'ALICE'")
        val facts = Facts.of("name" to "alice")
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `GroovyCondition equals and hashCode`() {
        val expr = "amount > 1000"
        val c1 = GroovyCondition(expr)
        val c2 = GroovyCondition(expr)
        (c1 == c2).shouldBeTrue()
        (c1.hashCode() == c2.hashCode()).shouldBeTrue()
    }

    @Test
    fun `GroovyCondition toString 표현`() {
        val condition = GroovyCondition("x > 0")
        condition.toString() shouldBeEqualTo "GroovyCondition(expression='x > 0')"
    }
}
