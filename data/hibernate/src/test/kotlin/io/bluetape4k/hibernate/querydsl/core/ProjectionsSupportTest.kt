package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.dsl.Expressions
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test

class ProjectionsSupportTest {

    companion object: KLogging()

    @Test
    fun `array and constructor projections are created`() {
        val str = Expressions.stringPath("str")
        val num = Expressions.numberPath(Long::class.javaObjectType, "id")

        val arrayProj = arrayProjectionOf(Array<String>::class.java, str, str)
        log.debug { "arrayProj: $arrayProj" }
        arrayProj.toString() shouldBeEqualTo "new String[](str, str)"

        val ctorProj = constructorProjectionOf<DummyDto>(str, num)
        log.debug { "ctorProj: $ctorProj" }
        ctorProj.toString() shouldBeEqualTo "new DummyDto(str, id)"
    }

    @Test
    fun `bean and field projections bind properties`() {
        val str = Expressions.stringPath("name")
        val num = Expressions.numberPath(Long::class.javaObjectType, "id")

        val bean = beanProjectionOf<DummyDto>(str, num)
        log.debug { "bean: $bean" }
        bean.toString() shouldBeEqualTo "new DummyDto(name, id)"

        val fields = fieldProjectionOf<DummyDto>(str, num)
        log.debug { "fields: $fields" }
        fields.toString() shouldBeEqualTo "new DummyDto(name, id)"
    }

    @Test
    fun `bean projection with bindings map`() {
        val str = Expressions.stringPath("name")
        val bindings = mapOf("name" to str)
        val bean = beanProjectionOf<DummyDto>(bindings)
        log.debug { "bean: $bean" }
        bean.toString() shouldBeEqualTo "new DummyDto(name)"
    }

    @Test
    fun `field projection with bindings map`() {
        val str = Expressions.stringPath("name")
        val bindings = mapOf("name" to str)
        val fields = fieldProjectionOf<DummyDto>(bindings)
        log.debug { "fields: $fields" }
        fields.toString() shouldBeEqualTo "new DummyDto(name)"
    }

    @Test
    fun `constructor projection with paramTypes and vararg`() {
        val str = Expressions.stringPath("name")
        val num = Expressions.numberPath(Long::class.javaObjectType, "id")
        val proj = constructorProjectionOf<DummyDto>(
            arrayOf(String::class, Long::class),
            str, num
        )
        log.debug { "proj: $proj" }
        proj.toString() shouldBeEqualTo "new DummyDto(name, id)"
    }

    @Test
    fun `constructor projection with paramTypes and list`() {
        val str = Expressions.stringPath("name")
        val num = Expressions.numberPath(Long::class.javaObjectType, "id")
        val proj = constructorProjectionOf<DummyDto>(
            arrayOf(String::class, Long::class),
            listOf(str, num)
        )
        log.debug { "proj: $proj" }
        proj.toString() shouldBeEqualTo "new DummyDto(name, id)"
    }

    @Test
    fun `list map tuple projections aggregate expressions`() {
        val str = Expressions.stringPath("name")
        val num = Expressions.numberPath(Int::class.javaObjectType, "age")

        projectionListOf(str, num).toString() shouldBeEqualTo "new List(name, age)"
        projectionListOf(listOf(str, num)).toString() shouldBeEqualTo "new List(name, age)"
        projectionMapOf(str, num).toString() shouldBeEqualTo "new Map(name, age)"
        projectionTupleOf(str, num).toString() shouldBeEqualTo "new Tuple(name, age)"
        projectionTupleOf(listOf(str, num)).toString() shouldBeEqualTo "new Tuple(name, age)"
    }

    @Test
    fun `Path bean and field projections`() {
        val str = Expressions.stringPath("name")
        val num = Expressions.numberPath(Long::class.javaObjectType, "id")
        val path = Expressions.path(DummyDto::class.java, "dto")

        path.beanProjectionOf(str, num).toString() shouldBeEqualTo "new DummyDto(name, id)"
        path.beanProjectionOf(mapOf("name" to str)).toString() shouldBeEqualTo "new DummyDto(name)"
        path.fieldProjectionOf(str, num).toString() shouldBeEqualTo "new DummyDto(name, id)"
        path.fieldProjectionOf(mapOf("name" to str)).toString() shouldBeEqualTo "new DummyDto(name)"
    }

    private data class DummyDto(val name: String?, val id: Long? = null)
}
