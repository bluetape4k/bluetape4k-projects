package io.bluetape4k.jackson3.text.yaml

import io.bluetape4k.jackson3.text.trimYamlDocMarker
import io.bluetape4k.jackson3.writeValue
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import tools.jackson.core.JsonGenerator
import tools.jackson.core.ObjectWriteContext
import java.io.StringWriter

class YamlGenerationExample: AbstractYamlExample() {

    companion object: KLogging()

    @Test
    fun `generate POJO`() {
        StringWriter().use { writer ->
            yamlFactory.createGenerator(ObjectWriteContext.empty(), writer).use { generator ->
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

    private fun JsonGenerator.writeBradDoc() {
        writeValue {
            writeStringProperty("name", "Brad")
            writeNumberProperty("age", 39)
        }
    }
}
