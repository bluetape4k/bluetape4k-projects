package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.dsl.Expressions
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ExpressionsSupportTest {

    @Test
    fun `all은 빈 컬렉션에서 true를 반환한다`() {
        val result = emptyList<com.querydsl.core.types.dsl.BooleanExpression>().all()
        result shouldBeEqualTo Expressions.TRUE
    }

    @Test
    fun `any는 빈 컬렉션에서 false를 반환한다`() {
        val result = emptyList<com.querydsl.core.types.dsl.BooleanExpression>().any()
        result shouldBeEqualTo Expressions.FALSE
    }

    @Test
    fun `all과 any는 비어있지 않은 컬렉션에서 논리식을 조합한다`() {
        val flag = Expressions.booleanPath("flag")
        val p1 = flag.isTrue
        val p2 = flag.isFalse

        val allExpr = listOf(p1, p2).all()
        val anyExpr = listOf(p1, p2).any()

        allExpr.toString() shouldBeEqualTo "flag = true && flag = false"
        anyExpr.toString() shouldBeEqualTo "flag = true || flag = false"
    }
}
