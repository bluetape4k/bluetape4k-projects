package io.bluetape4k.rule.engines.janino

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class JaninoConditionTest {

    companion object : KLogging()

    private fun intCast(varName: String) = "((Integer)facts.get(\"$varName\")).intValue()"

    @Test
    fun `숫자 비교 Janino 표현식 평가`() {
        val condition = JaninoCondition("${intCast("amount")} > 1000")
        val facts = Facts.of("amount" to 1500)
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `조건 불만족 시 false 반환`() {
        val condition = JaninoCondition("${intCast("amount")} > 1000")
        val facts = Facts.of("amount" to 500)
        condition.evaluate(facts).shouldBeFalse()
    }

    @Test
    fun `잘못된 Janino 표현식은 false 반환`() {
        val condition = JaninoCondition("invalid_expression_xyz")
        val facts = Facts.of("amount" to 1500)
        condition.evaluate(facts).shouldBeFalse()
    }

    @Test
    fun `AND 논리 연산자 Janino 표현식`() {
        val condition = JaninoCondition(
            "${intCast("age")} >= 18 && ((String)facts.get(\"role\")).equals(\"admin\")"
        )
        val facts = Facts.of("age" to 25, "role" to "admin")
        condition.evaluate(facts).shouldBeTrue()
    }

    @Test
    fun `boolean 결과 직접 반환`() {
        val condition = JaninoCondition("Boolean.TRUE")
        condition.evaluate(Facts.empty()).shouldBeTrue()
    }

    @Test
    fun `JaninoCondition equals and hashCode`() {
        val expr = "${intCast("x")} > 10"
        val c1 = JaninoCondition(expr)
        val c2 = JaninoCondition(expr)
        (c1 == c2).shouldBeTrue()
        (c1.hashCode() == c2.hashCode()).shouldBeTrue()
    }

    @Test
    fun `JaninoCondition toString 표현`() {
        val expr = "Boolean.TRUE"
        val condition = JaninoCondition(expr)
        condition.toString() shouldBeEqualTo "JaninoCondition(expression='$expr')"
    }

    @Test
    fun `janinoConditionOf 팩토리 함수`() {
        val condition = janinoConditionOf("${intCast("x")} > 10")
        val facts = Facts.of("x" to 20)
        condition.evaluate(facts).shouldBeTrue()
    }
}
