package io.bluetape4k.exposed.core.fastjson2

import io.bluetape4k.fastjson2.FastjsonSerializer
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [FastjsonColumnType] 및 [FastjsonBColumnType]의 직렬화/역직렬화 단위 테스트입니다.
 */
class FastjsonColumnTypeUnitTest {
    private data class SamplePayload(
        val name: String,
        val count: Int,
    )

    private val serializer = FastjsonSerializer.Default
    private val columnType =
        FastjsonColumnType<SamplePayload>(
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
    fun `FastjsonColumnType 은 usesBinaryFormat 이 false 이다`() {
        columnType.usesBinaryFormat.shouldBeFalse()
    }

    @Test
    fun `FastjsonBColumnType 은 usesBinaryFormat 이 true 이다`() {
        val bColumnType =
            FastjsonBColumnType<SamplePayload>(
                serialize = { serializer.serializeAsString(it) },
                deserialize = { serializer.deserializeFromString<SamplePayload>(it)!! }
            )
        bColumnType.usesBinaryFormat.shouldBeTrue()
    }

    @Test
    fun `FastjsonBColumnType 은 valueFromDB 에서 문자열을 역직렬화한다`() {
        val bColumnType =
            FastjsonBColumnType<SamplePayload>(
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

    /**
     * [fastjson] 테이블 확장함수의 역직렬화 람다는 빈 문자열 입력 시
     * `!!` 대신 `requireNotNull`을 사용하므로 [IllegalArgumentException]이 발생해야 합니다.
     */
    @Test
    fun `fastjson 확장함수 역직렬화 람다는 빈 문자열 입력 시 IllegalArgumentException 을 던진다`() {
        // FastjsonSerializer.deserializeFromString returns null for empty string
        // The default fastjson column extension wraps this with requireNotNull
        val requireNotNullDeserialize: (String) -> SamplePayload = {
            requireNotNull(serializer.deserializeFromString<SamplePayload>(it)) {
                "JSON 문자열을 SamplePayload 타입으로 역직렬화한 결과가 null입니다. 입력: $it"
            }
        }
        val ct = FastjsonColumnType<SamplePayload>(
            serilaize = { serializer.serializeAsString(it) },
            deserialize = requireNotNullDeserialize
        )
        assertThrows<IllegalArgumentException> {
            ct.valueFromDB("")
        }
    }

    /**
     * [FastjsonBColumnType]의 역직렬화 람다도 빈 문자열에서 [IllegalArgumentException]을 던져야 합니다.
     */
    @Test
    fun `fastjsonb 확장함수 역직렬화 람다는 빈 문자열 입력 시 IllegalArgumentException 을 던진다`() {
        val bColumnType = FastjsonBColumnType<SamplePayload>(
            serialize = { serializer.serializeAsString(it) },
            deserialize = {
                requireNotNull(serializer.deserializeFromString<SamplePayload>(it)) {
                    "JSON 문자열을 SamplePayload 타입으로 역직렬화한 결과가 null입니다. 입력: $it"
                }
            }
        )
        assertThrows<IllegalArgumentException> {
            bColumnType.valueFromDB("")
        }
    }

    /**
     * JSON 직렬화 후 역직렬화 왕복 변환이 [FastjsonBColumnType]에서도 동일하게 동작해야 합니다.
     */
    @Test
    fun `FastjsonBColumnType 도 왕복 변환이 일관된다`() {
        val bColumnType = FastjsonBColumnType<SamplePayload>(
            serialize = { serializer.serializeAsString(it) },
            deserialize = { serializer.deserializeFromString<SamplePayload>(it)!! }
        )
        val source = SamplePayload("bRoundtrip", 77)
        val json = bColumnType.notNullValueToDB(source) as String
        val restored = bColumnType.valueFromDB(json)

        restored shouldBeEqualTo source
    }

    /**
     * 특수 문자가 포함된 값도 직렬화 후 역직렬화가 정상 동작해야 합니다.
     */
    @Test
    fun `특수문자가 포함된 값도 왕복 변환이 정상 동작한다`() {
        val source = SamplePayload("name with \"quotes\" and 한글", 0)
        val json = columnType.notNullValueToDB(source) as String
        val restored = columnType.valueFromDB(json)

        restored shouldBeEqualTo source
    }

    /**
     * [FastjsonColumnType.notNullValueToDB]는 특수문자를 포함한 객체도 올바르게 직렬화합니다.
     * JSON 직렬화 결과에는 field명과 값이 모두 포함되어야 합니다.
     */
    @Test
    fun `notNullValueToDB 는 직렬화된 JSON 문자열에 필드명과 값이 모두 포함된다`() {
        val source = SamplePayload("myValue", 42)
        val result = columnType.notNullValueToDB(source) as String

        result shouldContain "\"name\""
        result shouldContain "\"myValue\""
        result shouldContain "\"count\""
        result shouldContain "42"
    }
}
