package io.bluetape4k.exposed.core.jackson3

import io.bluetape4k.support.toUtf8Bytes
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [JacksonColumnType] 및 [JacksonBColumnType]의 직렬화/역직렬화 단위 테스트입니다.
 */
class Jackson3ColumnTypeUnitTest {
    private data class SamplePayload(
        val name: String,
        val count: Int,
    )

    private val serializer = DefaultJacksonSerializer
    private val columnType =
        JacksonColumnType<SamplePayload>(
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

    @Test
    fun `notNullValueToDB 는 객체를 JSON 문자열로 직렬화한다`() {
        val source = SamplePayload("gamma", 30)
        val result = columnType.notNullValueToDB(source)

        result shouldBeInstanceOf String::class
        val json = result as String
        json shouldContain "\"name\":\"gamma\""
        json shouldContain "\"count\":30"
    }

    @Test
    fun `valueFromDB 후 notNullValueToDB 왕복 변환이 일관된다`() {
        val source = SamplePayload("roundtrip", 99)
        val json = columnType.notNullValueToDB(source) as String
        val restored = columnType.valueFromDB(json)

        restored shouldBeEqualTo source
    }

    @Test
    fun `JacksonColumnType 은 usesBinaryFormat 이 false 이다`() {
        columnType.usesBinaryFormat.shouldBeFalse()
    }

    @Test
    fun `JacksonBColumnType 은 usesBinaryFormat 이 true 이다`() {
        val bColumnType =
            JacksonBColumnType<SamplePayload>(
                serialize = { serializer.serializeAsString(it) },
                deserialize = { serializer.deserializeFromString<SamplePayload>(it)!! }
            )
        bColumnType.usesBinaryFormat.shouldBeTrue()
    }

    @Test
    fun `JacksonBColumnType 은 valueFromDB 에서 문자열을 역직렬화한다`() {
        val bColumnType =
            JacksonBColumnType<SamplePayload>(
                serialize = { serializer.serializeAsString(it) },
                deserialize = { serializer.deserializeFromString<SamplePayload>(it)!! }
            )
        val source = SamplePayload("jsonb", 42)
        val json = serializer.serializeAsString(source)

        bColumnType.valueFromDB(json) shouldBeEqualTo source
    }

    @Test
    fun `valueFromDB 에 잘못된 JSON 문자열이 들어오면 예외가 발생한다`() {
        assertThrows<Exception> {
            columnType.valueFromDB("not-valid-json")
        }
    }
}
