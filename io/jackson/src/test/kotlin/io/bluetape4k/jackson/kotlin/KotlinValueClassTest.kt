package io.bluetape4k.jackson.kotlin

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.Serializable

class KotlinValueClassTest {

    companion object: KLogging()

    private val mapper = Jackson.defaultJsonMapper

    data class SomeValue @JsonCreator constructor(
        val id: Identifier,
        val name: String,
    ): Serializable

    @JvmInline
    value class Identifier(val id: Long): Serializable

    @Test
    fun `json serialize value class`() {
        val someValue = SomeValue(Identifier(1L), "name")

        val json = mapper.writeValueAsString(someValue)
        log.debug { "json=$json" }

        val actual = mapper.readValue<SomeValue>(json)
        actual shouldBeEqualTo someValue
        log.debug { "actual=$actual" }
    }
}
