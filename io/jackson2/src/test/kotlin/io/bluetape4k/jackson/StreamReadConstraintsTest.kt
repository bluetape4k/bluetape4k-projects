package io.bluetape4k.jackson

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.StreamReadConstraints
import com.fasterxml.jackson.core.exc.StreamConstraintsException
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.cbor.CBORFactory
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import com.fasterxml.jackson.dataformat.smile.SmileFactory
import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLAnchorReplayingFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.jackson.async.AsyncJsonParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.StringReader

class StreamReadConstraintsTest {

    @ParameterizedTest
    @ValueSource(ints = [1, 7, 1024])
    fun `async 문서 길이 초과는 루트 전달 전에 거부한다`(chunkSize: Int) {
        val bytes = """{"value":"${"a".repeat(64)}"}""".toByteArray()
        val constraints = StreamReadConstraints.builder().maxDocumentLength(bytes.size.toLong() - 1).build()
        var delivered = 0
        val parser = AsyncJsonParser(JsonFactory.builder().streamReadConstraints(constraints).build()) {
            delivered++
        }

        assertFailsWith<StreamConstraintsException> {
            bytes.asList().chunked(chunkSize).forEach { parser.consume(it.toByteArray()) }
        }

        delivered shouldBeEqualTo 0
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 7, 1024])
    fun `async 문서 길이 경계값은 정상 처리한다`(chunkSize: Int) {
        val bytes = """{"value":"한글"}""".toByteArray()
        val constraints = StreamReadConstraints.builder().maxDocumentLength(bytes.size.toLong()).build()
        var delivered = 0
        val parser = AsyncJsonParser(JsonFactory.builder().streamReadConstraints(constraints).build()) {
            it["value"].asText() shouldBeEqualTo "한글"
            delivered++
        }

        bytes.asList().chunked(chunkSize).forEach { parser.consume(it.toByteArray()) }
        parser.endOfInput()

        delivered shouldBeEqualTo 1
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 7, 1024])
    fun `async 필드 이름 길이 초과를 거부한다`(chunkSize: Int) {
        val bytes = """{"${"n".repeat(65)}":1}""".toByteArray()
        val constraints = StreamReadConstraints.builder().maxNameLength(64).build()
        var delivered = 0
        val parser = AsyncJsonParser(JsonFactory.builder().streamReadConstraints(constraints).build()) {
            delivered++
        }

        assertFailsWith<StreamConstraintsException> {
            bytes.asList().chunked(chunkSize).forEach { parser.consume(it.toByteArray()) }
        }

        delivered shouldBeEqualTo 0
    }

    @Test
    fun `Reader 필드 이름 제한은 전체 이름을 읽기 전에 거부한다`() {
        val input = """{"${"n".repeat(16_000)}":1}"""
        val reader = CountingReader(input)
        val constraints = StreamReadConstraints.builder().maxNameLength(64).build()
        val mapper = JsonMapper.builder(JsonFactory.builder().streamReadConstraints(constraints).build()).build()

        assertFailsWith<StreamConstraintsException> {
            mapper.readTree(reader)
        }

        reader.charactersRead shouldBeLessThan input.length
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 7, 1024])
    fun `async 필드 이름 경계값은 정상 처리한다`(chunkSize: Int) {
        val name = "n".repeat(64)
        val bytes = """{"$name":1}""".toByteArray()
        val constraints = StreamReadConstraints.builder().maxNameLength(64).build()
        var delivered = 0
        val parser = AsyncJsonParser(JsonFactory.builder().streamReadConstraints(constraints).build()) {
            it[name].asInt() shouldBeEqualTo 1
            delivered++
        }

        bytes.asList().chunked(chunkSize).forEach { parser.consume(it.toByteArray()) }
        parser.endOfInput()

        delivered shouldBeEqualTo 1
    }

    @Test
    fun `Reader 필드 이름 경계값은 정상 처리한다`() {
        val name = "n".repeat(64)
        val constraints = StreamReadConstraints.builder().maxNameLength(64).build()
        val mapper = JsonMapper.builder(JsonFactory.builder().streamReadConstraints(constraints).build()).build()

        mapper.readTree(StringReader("""{"$name":1}"""))[name].asInt() shouldBeEqualTo 1
    }

    @Test
    fun `CBOR 필드 이름 길이 제한과 경계값을 검증한다`() {
        val writer = CBORMapper()
        val constraints = StreamReadConstraints.builder().maxNameLength(64).build()
        val mapper = CBORMapper.builder(CBORFactory.builder().streamReadConstraints(constraints).build()).build()
        val name = "n".repeat(64)

        mapper.readTree(writer.writeValueAsBytes(mapOf(name to 1)))[name].asInt() shouldBeEqualTo 1
        assertFailsWith<StreamConstraintsException> {
            mapper.readTree(writer.writeValueAsBytes(mapOf(name + "n" to 1)))
        }
    }

    @Test
    fun `Smile 필드 이름 길이 제한과 경계값을 검증한다`() {
        val writer = SmileMapper()
        val constraints = StreamReadConstraints.builder().maxNameLength(64).build()
        val mapper = SmileMapper.builder(SmileFactory.builder().streamReadConstraints(constraints).build()).build()
        val name = "n".repeat(64)

        mapper.readTree(writer.writeValueAsBytes(mapOf(name to 1)))[name].asInt() shouldBeEqualTo 1
        assertFailsWith<StreamConstraintsException> {
            mapper.readTree(writer.writeValueAsBytes(mapOf(name + "n" to 1)))
        }
    }

    @Test
    fun `CBOR 과도한 선언 이름 길이는 이름 바이트를 읽기 전에 거부한다`() {
        val constraints = StreamReadConstraints.builder().maxNameLength(64).build()
        val mapper = CBORMapper.builder(CBORFactory.builder().streamReadConstraints(constraints).build()).build()
        // Map(1), text name(length=65536): 실제 이름 바이트가 없어도 EOF보다 길이 제한이 먼저 적용된다.
        val bytes = byteArrayOf(0xa1.toByte(), 0x7a, 0x00, 0x01, 0x00, 0x00)

        assertFailsWith<StreamConstraintsException> {
            mapper.readTree(bytes)
        }
    }

    @Test
    fun `YAML merge 중첩은 제한을 초과하면 거부한다`() {
        val constraints = StreamReadConstraints.builder().maxNestingDepth(8).build()
        val factory = YAMLFactory.builder().streamReadConstraints(constraints).build()
        val mapper = YAMLMapper(YAMLAnchorReplayingFactory(factory, null))
        val merge = "{<<: ".repeat(32) + "{value: 1}" + "}".repeat(32)

        assertFailsWith<StreamConstraintsException> {
            mapper.readTree("result: $merge")
        }
    }

    @Test
    fun `YAML 제한 이하 merge는 정상 처리한다`() {
        val constraints = StreamReadConstraints.builder().maxNestingDepth(8).build()
        val factory = YAMLFactory.builder().streamReadConstraints(constraints).build()
        val mapper = YAMLMapper(YAMLAnchorReplayingFactory(factory, null))

        mapper.readTree("result: {<<: {value: 1}}")["result"]["value"].asInt() shouldBeEqualTo 1
    }

    private class CountingReader(input: String): StringReader(input) {
        var charactersRead: Int = 0
            private set

        override fun read(buffer: CharArray, offset: Int, length: Int): Int =
            super.read(buffer, offset, length).also { count ->
                if (count > 0) charactersRead += count
            }
    }
}
