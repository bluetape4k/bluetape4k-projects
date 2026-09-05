package io.bluetape4k.jackson

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.StreamReadConstraints
import com.fasterxml.jackson.core.exc.StreamConstraintsException
import com.fasterxml.jackson.databind.json.JsonMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Path
import javax.xml.datatype.Duration
import javax.xml.datatype.XMLGregorianCalendar

class DatabindInputConstraintsTest {

    @Test
    fun `XML datatype 숫자 문자열 길이 제한과 정상 변환을 검증한다`() {
        val constraints = StreamReadConstraints.builder().maxNumberLength(32).build()
        val mapper = JsonMapper.builder(JsonFactory.builder().streamReadConstraints(constraints).build()).build()

        assertFailsWith<StreamConstraintsException> {
            mapper.readValue("\"P${"9".repeat(64)}Y\"", Duration::class.java)
        }
        assertFailsWith<StreamConstraintsException> {
            mapper.readValue("\"00:00:00.${"9".repeat(64)}\"", XMLGregorianCalendar::class.java)
        }

        mapper.readValue("\"P1Y\"", Duration::class.java).years shouldBeEqualTo 1
        mapper.readValue("\"2026-01-01T00:00:00\"", XMLGregorianCalendar::class.java).year shouldBeEqualTo 2026
    }

    @ParameterizedTest
    @ValueSource(strings = ["https://example.invalid/file", "jar:file:/nonexistent.zip!/entry"])
    fun `Path 역직렬화는 허용되지 않은 scheme을 명시적으로 거부한다`(uri: String) {
        val error = assertFailsWith<InvalidFormatException> {
            Jackson.defaultJsonMapper.readValue("\"$uri\"", Path::class.java)
        }

        error.message.orEmpty() shouldContain "not allowed for Path deserialization"
    }

    @Test
    fun `Path 역직렬화는 로컬 경로와 file scheme을 허용한다`() {
        val mapper = Jackson.defaultJsonMapper

        mapper.readValue("\"relative/file.json\"", Path::class.java) shouldBeEqualTo Path.of("relative/file.json")
        mapper.readValue("\"file:/local/file.json\"", Path::class.java) shouldBeEqualTo Path.of("/local/file.json")
    }
}
