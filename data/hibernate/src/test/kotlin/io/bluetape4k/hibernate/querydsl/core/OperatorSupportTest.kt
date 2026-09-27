package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.dsl.Expressions
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test

class OperatorSupportTest {

    companion object: KLogging()

    @Test
    fun `SimpleExpression inValues 는 빈 인자도 처리한다`() {
        val name = Expressions.stringPath("name")

        val expr = name.inValues()
        log.debug { "expr: $expr" }
        expr.toString() shouldBeEqualTo "name in []"
    }

    @Test
    fun `SimpleExpression inValues 는 가변 인자를 묶어준다`() {
        val name = Expressions.stringPath("name")

        val expr = name.inValues("a", "b", "c")
        log.debug { "expr: $expr" }
        expr.toString() shouldBeEqualTo "name in [a, b, c]"
    }

    @Test
    fun `StringExpression plus 는 blank 문자열도 concat 한다`() {
        val name = Expressions.stringPath("name")

        val expr = name + "   "
        log.debug { "expr: $expr" }
        expr.toString() shouldBeEqualTo "name +    "
    }

    @Test
    fun `StringExpression plus 는 expression concat 을 지원한다`() {
        val left = Expressions.stringPath("left")
        val right = Expressions.stringPath("right")

        val expr = left + right
        log.debug { "expr: $expr" }
        expr.toString() shouldBeEqualTo "left + right"
    }

    @Test
    fun `StringExpression plus 는 문자열 누적을 지원한다`() {
        val left = Expressions.stringPath("left")

        val expr = left + "foo" + "bar"
        log.debug { "expr: $expr" }
        expr.toString() shouldBeEqualTo "left + foo + bar"
    }
}
