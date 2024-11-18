package io.bluetape4k.jackson

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import java.io.StringWriter

class JsonGeneratorExtensionsTest {

    companion object: KLogging() {
        private val faker = Fakers.faker
    }

    val mapper = jacksonObjectMapper()


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
                gen.writeNumber("number", 42)
            }
            val json = writer.toString()
            log.debug { "json=$json" }
            json shouldBeEqualTo """{"number":42}"""
        }
    }

    data class Dummy(val name: String, val number: Int)

    private fun newDummy(): Dummy {
        return Dummy(faker.name().fullName(), faker.number().numberBetween(1, 100))
    }

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
            val dummies = mapper.readValue<List<Dummy>>(json)
            dummies shouldHaveSize 3
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
            val dummies = mapper.readValue<List<Dummy>>(json)
            dummies shouldHaveSize 3
        }
    }
}
