package io.bluetape4k.spring.core

import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.spring.AbstractSpringTest
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.RepeatedTest
import java.io.Serializable
import java.time.LocalDate

@RandomizedTest
class ToStringCreatorExtensionsTest: AbstractSpringTest() {

    companion object: KLogging()

    class SampleClass(
        val name: String,
        val age: Int,
        val birth: LocalDate = LocalDate.of(1968, 10, 14),
    ): Serializable {
        override fun toString(): String =
            toStringCreatorOf(this) {
                append("name", name)
                append("age", age)
                append("birth", birth)

            }.toString()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `ToStringCreator를 이용하여 객체를 문자열로 표현하기`(@RandomValue instance: SampleClass) {
        val toString = instance.toString()

        log.trace { "toString=$toString" }
        toString shouldContain instance.javaClass.simpleName
        toString shouldContain "name = '${instance.name}'"
        toString shouldContain "age = ${instance.age}"
        toString shouldContain "birth = ${instance.birth}"
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `use ToStringCreatorToken`() {
        val instance = SampleValueObject().apply {
            name = faker.name().fullName()
            age = faker.random().nextInt(19, 80)
        }

        val toString = instance.toString()
        log.trace { "toString=$toString" }
        toString shouldContain instance.javaClass.simpleName
        toString shouldContain "name = '${instance.name}'"
        toString shouldContain "age = ${instance.age}"
    }

    internal class SampleValueObject: Serializable {
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
