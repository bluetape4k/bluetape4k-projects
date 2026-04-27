package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.dsl.Expressions
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class FactoryExpressionSupportTest {

    @Test
    fun `List_wrap은 FactoryExpression을 반환한다`() {
        val str = Expressions.stringPath("name")
        val num = Expressions.numberPath(Long::class.javaObjectType, "id")
        val ctorExpr = constructorProjectionOf<DummyClass>(str, num)
        // FactoryExpression이 포함된 리스트에서만 non-null 반환
        val wrapped = listOf(ctorExpr, str, num).wrap()
        wrapped.shouldNotBeNull()
    }

    @Test
    fun `FactoryExpression_wrap without conversions는 null-safe FactoryExpression을 반환한다`() {
        val str = Expressions.stringPath("name")
        val num = Expressions.numberPath(Long::class.javaObjectType, "id")
        val ctorExpr = constructorProjectionOf<DummyClass>(str, num)
        val wrapped = ctorExpr.wrap()
        wrapped.shouldNotBeNull()
    }

    @Test
    fun `FactoryExpression_wrap with conversions는 변환된 FactoryExpression을 반환한다`() {
        val str = Expressions.stringPath("name")
        val num = Expressions.numberPath(Long::class.javaObjectType, "id")
        val ctorExpr = constructorProjectionOf<DummyClass>(str, num)
        val wrapped = ctorExpr.wrap(listOf(str, num))
        wrapped.shouldNotBeNull()
    }

    data class DummyClass(val name: String? = null, val id: Long? = null)
}
