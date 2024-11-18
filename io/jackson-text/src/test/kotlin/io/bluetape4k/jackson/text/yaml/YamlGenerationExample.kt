package io.bluetape4k.jackson.text.yaml

import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import io.bluetape4k.jackson.writeValue
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.StringWriter

class YamlGenerationExample: AbstractYamlExample() {

    companion object: KLogging()

    @Test
    fun `generate POJO`() {
        StringWriter().use { writer ->
            yamlFactory.createGenerator(writer).use { generator ->
                generator.writeBradDoc()
            }

            val yaml = writer.toString().trimYamlDocMarker()
            log.debug { "generated yaml=\n$yaml" }

            val expected =
                """
                |name: "Brad"
                |age: 39
                """.trimMargin()

            yaml shouldBeEqualTo expected
        }
    }

    private fun YAMLGenerator.writeBradDoc() {
        writeValue {
            writeStringField("name", "Brad")
            writeNumberField("age", 39)
        }
    }
}
