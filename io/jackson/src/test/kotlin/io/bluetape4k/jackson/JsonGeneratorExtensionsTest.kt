package io.bluetape4k.jackson

import com.fasterxml.jackson.databind.json.JsonMapper
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.io.StringWriter

class JsonGeneratorExtensionsTest {

    companion object: KLogging() {
        private val faker = Fakers.faker
    }

    private val mapper: JsonMapper = Jackson.defaultJsonMapper

    @Test
    fun `generate string value`() {
        StringWriter().use { writer ->
            mapper.createGenerator(writer).use { gen ->
                gen.writeString("name", "hello")
            }
            val json = writer.toString()
            log.debug { "json=$json" }
            json shouldBeEqualTo """{"name":"hello"}"""
        }
    }

    @Test
    fun `generate number value`() {
        StringWriter().use { writer ->
            mapper.createGenerator(writer).use { gen ->
                gen.writeNumber("int", 42)
                gen.writeNumber("long", 42L)
                gen.writeNumber("double", 3.141926)
            }
            val json = writer.toString()
            log.debug { "json=$json" }
            json shouldBeEqualTo """{"int":42} {"long":42} {"double":3.141926}"""
        }
    }

    data class Dummy(val name: String, val number: Int): Serializable

    private fun newDummy(): Dummy = Dummy(
        faker.name().fullName(),
        faker.number().numberBetween(1, 100)
    )

    @Test
    fun `generate array value`() {
        StringWriter().use { writer ->
            mapper.createGenerator(writer).use { gen ->
                gen.writeArray {
                    repeat(3) {
                        writeObject(newDummy())
                    }
                }
            }
            val json = writer.toString()
            log.debug { "json=$json" }
            val dummies = mapper.readValueOrNull<List<Dummy>>(json)
            dummies.shouldNotBeNull() shouldHaveSize 3
        }
    }

    @Test
    fun `generate object list`() {
        StringWriter().use { writer ->
            mapper.createGenerator(writer).use { gen ->
                val objects = List(3) { newDummy() }
                gen.writeObjects(objects)
            }
            val json = writer.toString()
            log.debug { "json=$json" }
            val dummies = mapper.readValueOrNull<List<Dummy>>(json)
            dummies.shouldNotBeNull() shouldHaveSize 3
        }
    }
}
