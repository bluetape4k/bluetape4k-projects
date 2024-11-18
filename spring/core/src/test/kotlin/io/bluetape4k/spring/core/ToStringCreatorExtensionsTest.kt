package io.bluetape4k.spring.core

import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import java.time.LocalDate

@RandomizedTest
class ToStringCreatorExtensionsTest {

    companion object: KLogging()

    class SampleClass(
        val name: String,
        val age: Int,
        val birth: LocalDate = LocalDate.of(1968, 10, 14),
    ) {
        override fun toString(): String {
            return toStringCreatorOf(this) {
                append("name", name)
                append("age", age)
                append("birth", birth)

            }.toString()
        }
    }

    @Test
    fun `ToStringCreator를 이용하여 객체를 문자열로 표현하기`(@RandomValue instance: SampleClass) {
        val toString = instance.toString()

        log.trace { "toString=$toString" }
        toString shouldContain instance.javaClass.simpleName
        toString shouldContain "name = '${instance.name}'"
        toString shouldContain "age = ${instance.age}"
        toString shouldContain "birth = ${instance.birth}"
    }

    @Test
    fun `use ToStringCreatorToken`() {
        val instance = ValueObject().apply {
            name = "debop"
            age = 51
        }

        val toString = instance.toString()
        log.trace { "toString=$toString" }
        toString shouldContain instance.javaClass.simpleName
        toString shouldContain "name = '${instance.name}'"
        toString shouldContain "age = ${instance.age}"
    }

    internal class ValueObject {
        var name: String? = null
        var age: Int? = null

        override fun toString(): String =
            toStringCreatorOf(this) {
                val tokens = ToStringCreatorAppendTokens(this)
                tokens["name"] = name
                tokens["age"] = age
            }.toString()
    }
}
