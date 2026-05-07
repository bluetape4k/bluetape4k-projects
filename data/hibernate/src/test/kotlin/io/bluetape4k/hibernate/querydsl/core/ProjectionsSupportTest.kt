package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.dsl.Expressions
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
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
    fun `bean projection with bindings map`() {
        val str = Expressions.stringPath("name")
        val bindings = mapOf("name" to str)
        val bean = beanProjectionOf<DummyDto>(bindings)
        bean.shouldNotBeNull()
    }

    @Test
    fun `field projection with bindings map`() {
        val str = Expressions.stringPath("name")
        val bindings = mapOf("name" to str)
        val fields = fieldProjectionOf<DummyDto>(bindings)
        fields.shouldNotBeNull()
    }

    @Test
    fun `constructor projection with paramTypes and vararg`() {
        val str = Expressions.stringPath("name")
        val num = Expressions.numberPath(Long::class.javaObjectType, "id")
        val proj = constructorProjectionOf<DummyDto>(
            arrayOf(String::class, Long::class),
            str, num
        )
        proj.shouldNotBeNull()
    }

    @Test
    fun `constructor projection with paramTypes and list`() {
        val str = Expressions.stringPath("name")
        val num = Expressions.numberPath(Long::class.javaObjectType, "id")
        val proj = constructorProjectionOf<DummyDto>(
            arrayOf(String::class, Long::class),
            listOf(str, num)
        )
        proj.shouldNotBeNull()
    }

    @Test
    fun `list map tuple projections aggregate expressions`() {
        val str = Expressions.stringPath("name")
        val num = Expressions.numberPath(Int::class.javaObjectType, "age")

        projectionListOf(str, num).args.shouldNotBeEmpty()
        projectionListOf(listOf(str, num)).args.shouldNotBeEmpty()
        projectionMapOf(str, num).args.shouldNotBeEmpty()
        projectionTupleOf(str, num).args.shouldNotBeEmpty()
        projectionTupleOf(listOf(str, num)).args.shouldNotBeEmpty()
    }

    @Test
    fun `Path bean and field projections`() {
        val str = Expressions.stringPath("name")
        val num = Expressions.numberPath(Long::class.javaObjectType, "id")
        val path = Expressions.path(DummyDto::class.java, "dto")

        path.beanProjectionOf(str, num).shouldNotBeNull()
        path.beanProjectionOf(mapOf("name" to str)).shouldNotBeNull()
        path.fieldProjectionOf(str, num).shouldNotBeNull()
        path.fieldProjectionOf(mapOf("name" to str)).shouldNotBeNull()
    }

    private data class DummyDto(val name: String?, val id: Long? = null)
}
