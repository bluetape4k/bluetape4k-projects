package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.dsl.Expressions
import io.bluetape4k.assertions.invoking
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

class OperatorSupportTest {

    @Test
    fun `SimpleExpression inValues 는 빈 인자도 처리한다`() {
        val name = Expressions.stringPath("name")

        val expr = invoking {
            name.inValues()
        }.shouldNotThrow()
        expr.shouldNotBeNull()

        expr.toString().isNotBlank().shouldBeTrue()
    }

    @Test
    fun `SimpleExpression inValues 는 가변 인자를 묶어준다`() {
        val name = Expressions.stringPath("name")

        val expr = invoking {
            name.inValues("a", "b", "c")
        }.shouldNotThrow()
        expr.shouldNotBeNull()

        expr.toString().contains("in").shouldBeTrue()
    }

    @Test
    fun `StringExpression plus 는 blank 문자열도 concat 한다`() {
        val name = Expressions.stringPath("name")

        val expr = invoking {
            name + "   "
        }.shouldNotThrow()
        expr.shouldNotBeNull()

        expr.toString().isNotBlank().shouldBeTrue()
    }

    @Test
    fun `StringExpression plus 는 expression concat 을 지원한다`() {
        val left = Expressions.stringPath("left")
        val right = Expressions.stringPath("right")

        val expr = invoking {
            left + right
        }.shouldNotThrow()
        expr.shouldNotBeNull()

        expr.toString().isNotBlank().shouldBeTrue()
    }

    @Test
    fun `StringExpression plus 는 문자열 누적을 지원한다`() {
        val left = Expressions.stringPath("left")

        val expr = invoking {
            left + "foo" + "bar"
        }.shouldNotThrow()
        expr.shouldNotBeNull()

        expr.toString().isNotBlank().shouldBeTrue()
    }
}
