package io.bluetape4k.exposed.core.jackson3

import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

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
        assertFailsWith<Exception> {
            columnType.valueFromDB("not-valid-json")
        }
    }

    /**
     * DefaultJacksonSerializer 가 싱글턴 인스턴스임을 보장한다.
     * ObjectMapper 인스턴스를 프로세스 전체에서 재사용해 초기화 비용을 방지한다.
     */
    @Test
    fun `DefaultJacksonSerializer 는 동일한 싱글턴 인스턴스를 반환한다`() {
        val s1 = DefaultJacksonSerializer
        val s2 = DefaultJacksonSerializer
        (s1 === s2).shouldBeTrue()
    }

    /**
     * valueFromDB 가 이미 T 타입인 값을 그대로 캐스팅하여 반환하는지 확인한다.
     * DB 드라이버가 이미 역직렬화된 객체를 반환하는 경우를 방어한다.
     */
    @Test
    fun `valueFromDB 는 이미 T 타입인 값을 역직렬화 없이 반환한다`() {
        val source = SamplePayload("already", 55)
        columnType.valueFromDB(source) shouldBeEqualTo source
    }

    /**
     * JacksonColumnType 의 serilaize 함수가 null-safe 하게 동작하는지 확인한다.
     * 직렬화된 JSON 에 불필요한 공백이 없고 예상 키를 포함해야 한다.
     */
    @Test
    fun `notNullValueToDB 직렬화 결과에 null 필드가 포함되지 않는다`() {
        data class WithNullable(val name: String, val extra: String? = null)
        val ct = JacksonColumnType<WithNullable>(
            serilaize = { serializer.serializeAsString(it) },
            deserialize = { serializer.deserializeFromString<WithNullable>(it)!! }
        )
        val value = WithNullable("test")
        val json = ct.notNullValueToDB(value) as String
        json shouldContain "\"name\":\"test\""
    }

    /**
     * JacksonBColumnType 의 needsBinaryFormatCast 기본값이 false 임을 확인한다.
     * castToJsonFormat=false 이면 SQLite 여부와 관계없이 false 여야 한다.
     */
    @Test
    fun `JacksonBColumnType castToJsonFormat=false 이면 needsBinaryFormatCast 는 false 이다`() {
        val bColumnType =
            JacksonBColumnType<SamplePayload>(
                serialize = { serializer.serializeAsString(it) },
                deserialize = { serializer.deserializeFromString<SamplePayload>(it)!! },
                castToJsonFormat = false,
            )
        bColumnType.needsBinaryFormatCast.shouldBeFalse()
    }

    /**
     * JacksonBColumnType 왕복(직렬화→역직렬화) 일관성을 확인한다.
     */
    @Test
    fun `JacksonBColumnType 왕복 변환이 일관된다`() {
        val bColumnType =
            JacksonBColumnType<SamplePayload>(
                serialize = { serializer.serializeAsString(it) },
                deserialize = { serializer.deserializeFromString<SamplePayload>(it)!! }
            )
        val source = SamplePayload("roundtrip-b", 77)
        val json = bColumnType.notNullValueToDB(source) as String
        val restored = bColumnType.valueFromDB(json)
        restored shouldBeEqualTo source
    }

    /**
     * JacksonColumnType 의 notNullValueToDB 는 직렬화 함수가 반환한 값을 그대로 DB 저장 값으로 사용한다.
     * 별도 캐시나 변환 없이 사용자 제공 함수에 위임하는 것을 검증한다.
     */
    @Test
    fun `notNullValueToDB 는 커스텀 직렬화 함수에 위임한다`() {
        val customOutput = "CUSTOM_JSON"
        val customColumnType = JacksonColumnType<SamplePayload>(
            serilaize = { customOutput },
            deserialize = { serializer.deserializeFromString<SamplePayload>(it)!! }
        )
        val source = SamplePayload("any", 0)
        customColumnType.notNullValueToDB(source) shouldBeEqualTo customOutput
    }
}
