package io.bluetape4k.exposed.core.jackson

import io.bluetape4k.support.toUtf8Bytes
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class JacksonColumnTypeUnitTest {

    private data class SamplePayload(
        val name: String,
        val count: Int,
    )

    private val serializer = DefaultJacksonSerializer
    private val columnType = JacksonColumnType<SamplePayload>(
        serilaize = { serializer.serializeAsString(it) },
        deserialize = { serializer.deserializeFromString<SamplePayload>(it)!! }
    )

    @Test
    fun `valueFromDB 는 문자열 JSON 을 객체로 역직렬화한다`() {
        val source = SamplePayload("alpha", 10)
        val json = serializer.serializeAsString(source)

        columnType.valueFromDB(json) shouldBeEqualTo source
    }

    @Test
    fun `valueFromDB 는 UTF-8 바이트 JSON 을 객체로 역직렬화한다`() {
        val source = SamplePayload("beta", 20)
        val jsonBytes = serializer.serializeAsString(source).toUtf8Bytes()

        columnType.valueFromDB(jsonBytes) shouldBeEqualTo source
    }

    @Test
    fun `valueFromDB 는 미지원 타입 입력 시 원본 값을 그대로 반환한다`() {
        columnType.valueFromDB(1234) shouldBeEqualTo 1234
    }
}
