package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.dsl.Expressions
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test
import java.io.Serializable

class FactoryExpressionSupportTest {

    companion object: KLogging()

    @Test
    fun `List_wrap은 FactoryExpression을 반환한다`() {
        val str = Expressions.stringPath("name")
        val num = Expressions.numberPath(Long::class.javaObjectType, "id")

        val ctorExpr = constructorProjectionOf<DummyClass>(str, num)
        log.debug { "ctorExpr: $ctorExpr" }
        ctorExpr.toString() shouldBeEqualTo "new DummyClass(name, id)"

        // FactoryExpression이 포함된 리스트에서만 non-null 반환
        val wrapped = listOf(ctorExpr, str, num).wrap()
        log.debug { "wrapped: $wrapped" }
        wrapped.toString() shouldBeEqualTo "new Object[](name, id, name, id)"

    }

    @Test
    fun `FactoryExpression_wrap without conversions는 null-safe FactoryExpression을 반환한다`() {
        val str = Expressions.stringPath("name")
        val num = Expressions.numberPath(Long::class.javaObjectType, "id")

        val ctorExpr = constructorProjectionOf<DummyClass>(str, num)
        log.debug { "ctorExpr: $ctorExpr" }
        ctorExpr.toString() shouldBeEqualTo "new DummyClass(name, id)"

        val wrapped = ctorExpr.wrap()
        log.debug { "wrapped: $wrapped" }
        wrapped.toString() shouldBeEqualTo "new DummyClass(name, id)"
    }

    @Test
    fun `FactoryExpression_wrap with conversions는 변환된 FactoryExpression을 반환한다`() {
        val str = Expressions.stringPath("name")
        val num = Expressions.numberPath(Long::class.javaObjectType, "id")

        val ctorExpr = constructorProjectionOf<DummyClass>(str, num)
        log.debug { "ctorExpr: $ctorExpr" }
        ctorExpr.toString() shouldBeEqualTo "new DummyClass(name, id)"

        val wrapped = ctorExpr.wrap(listOf(str, num))
        log.debug { "wrapped: $wrapped" }
        wrapped.toString() shouldBeEqualTo "new DummyClass(name, id)"
    }

    data class DummyClass(
        val name: String? = null,
        val id: Long? = null
    ): Serializable
}
