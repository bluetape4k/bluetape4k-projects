package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.dsl.Expressions
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class OperatorSupportTest {

    @Test
    fun `SimpleExpression inValues 는 빈 인자도 처리한다`() {
        val name = Expressions.stringPath("name")

        val expr = assertDoesNotThrow {
            name.inValues()
        }

        expr.toString().isNotBlank().shouldBeTrue()
    }

    @Test
    fun `StringExpression plus 는 blank 문자열도 concat 한다`() {
        val name = Expressions.stringPath("name")

        val expr = assertDoesNotThrow {
            name + "   "
        }

        expr.toString().isNotBlank().shouldBeTrue()
    }

    @Test
    fun `StringExpression plus 는 expression concat 을 지원한다`() {
        val left = Expressions.stringPath("left")
        val right = Expressions.stringPath("right")

        val expr = assertDoesNotThrow {
            left + right
        }

        expr.toString().isNotBlank().shouldBeTrue()
    }
}
