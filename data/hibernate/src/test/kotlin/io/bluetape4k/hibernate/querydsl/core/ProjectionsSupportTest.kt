package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.dsl.Expressions
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class ProjectionsSupportTest {

    @Test
    fun `array and constructor projections are created`() {
        val str = Expressions.stringPath("str")
        val num = Expressions.numberPath(Long::class.javaObjectType, "id")

        val arrayProj = arrayProjectionOf(Array<String>::class.java, str, str)
        arrayProj.shouldNotBeNull()

        val ctorProj = constructorProjectionOf<DummyDto>(str, num)
        ctorProj.shouldNotBeNull()
    }

    @Test
    fun `bean and field projections bind properties`() {
        val str = Expressions.stringPath("name")
        val num = Expressions.numberPath(Long::class.javaObjectType, "id")

        val bean = beanProjectionOf<DummyDto>(str, num)
        bean.shouldNotBeNull()

        val fields = fieldProjectionOf<DummyDto>(str, num)
        fields.shouldNotBeNull()
    }

    @Test
    fun `list map tuple projections aggregate expressions`() {
        val str = Expressions.stringPath("name")
        val num = Expressions.numberPath(Int::class.javaObjectType, "age")

        projectionListOf(str, num).args.shouldNotBeEmpty()
        projectionMapOf(str, num).args.shouldNotBeEmpty()
        projectionTupleOf(str, num).args.shouldNotBeEmpty()
    }

    private data class DummyDto(val name: String?, val id: Long? = null)
}
