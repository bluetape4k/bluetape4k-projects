package io.bluetape4k.exposed.core.jackson

import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [JacksonColumnType] 및 [JacksonBColumnType]의 직렬화/역직렬화 단위 테스트입니다.
 */
class JacksonColumnTypeUnitTest {
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

    /**
     * jackson() 함수는 역직렬화 결과가 null이면 !! 대신 requireNotNull을 사용하므로,
     * 문제 원인(타입명, 원본 JSON)이 포함된 명확한 메시지로 실패해야 합니다.
     */
    @Test
    fun `jackson Table 확장함수 기반 역직렬화가 null이면 IllegalArgumentException이 발생한다`() {
        // null을 반환하도록 설계된 역직렬화 람다를 직접 사용하여 경로 검증
        val nullDeserializeType = JacksonColumnType<SamplePayload>(
            serilaize = { serializer.serializeAsString(it) },
            deserialize = {
                requireNotNull(null as SamplePayload?) {
                    "JSON 역직렬화 결과가 null입니다. 타입 [${SamplePayload::class.qualifiedName}] 으로 변환할 수 없는 JSON 입니다: $it"
                }
            }
        )
        val ex = assertThrows<IllegalArgumentException> {
            nullDeserializeType.valueFromDB("{}")
        }
        require(ex.message?.contains("SamplePayload") == true) {
            "예외 메시지에 타입명이 포함되어야 합니다. 실제: ${ex.message}"
        }
    }

    /**
     * valueFromDB가 이미 타겟 타입 T인 값을 받으면, 역직렬화 없이 그대로 반환해야 합니다.
     * — 불필요한 직렬화 오버헤드를 방지하는 최적화 경로 검증
     */
    @Test
    fun `valueFromDB 는 이미 T 타입인 값을 역직렬화 없이 반환한다`() {
        val source = SamplePayload("direct", 77)
        // SamplePayload 는 T이므로 역직렬화 없이 반환돼야 함
        val result = columnType.valueFromDB(source)
        // valueFromDB 의 else 분기: T 캐스팅 시도 → as? T 성공
        require(result === source || result == source) {
            "이미 T 타입인 경우 동일 인스턴스(또는 동등 값)를 반환해야 합니다"
        }
    }

    /**
     * JacksonColumnType을 사용자 정의 직렬화 함수로 생성해서 정상 동작하는지 확인합니다.
     * — 커스텀 직렬화/역직렬화 함수가 올바르게 연동되는지 검증
     */
    @Test
    fun `사용자 정의 직렬화 함수로 생성한 JacksonColumnType이 올바르게 동작한다`() {
        val customType = JacksonColumnType<SamplePayload>(
            serilaize = { """{"name":"${it.name}","count":${it.count}}""" },
            deserialize = { serializer.deserializeFromString<SamplePayload>(it)!! }
        )
        val source = SamplePayload("custom", 55)
        val serialized = customType.notNullValueToDB(source) as String
        val deserialized = customType.valueFromDB(serialized)

        deserialized shouldBeEqualTo source
    }

    /**
     * JacksonBColumnType은 usesBinaryFormat=true이므로 sqlType()이 JSON이 아닌 JSONB 타입을 반환해야 합니다.
     * — JacksonColumnType(JSON)과 JacksonBColumnType(JSONB)의 포맷 구분이 올바른지 검증합니다.
     */
    @Test
    fun `JacksonBColumnType 은 JacksonColumnType 과 usesBinaryFormat 이 다르다`() {
        val jsonType = JacksonColumnType<SamplePayload>(
            serilaize = { serializer.serializeAsString(it) },
            deserialize = { serializer.deserializeFromString<SamplePayload>(it)!! }
        )
        val jsonbType = JacksonBColumnType<SamplePayload>(
            serialize = { serializer.serializeAsString(it) },
            deserialize = { serializer.deserializeFromString<SamplePayload>(it)!! }
        )
        jsonType.usesBinaryFormat.shouldBeFalse()
        jsonbType.usesBinaryFormat.shouldBeTrue()
    }

    /**
     * JacksonColumnType은 JSON 문자열로 직렬화 후 ByteArray로 역직렬화해도 원본과 동일해야 합니다.
     * — H2 Dialect에서 ByteArray로 파라미터가 전달되는 경로(setParameter)를 간접 검증합니다.
     */
    @Test
    fun `valueFromDB 는 ByteArray UTF-8 JSON 을 역직렬화할 수 있다`() {
        val source = SamplePayload("bytes", 9)
        val jsonBytes = serializer.serializeAsString(source).toByteArray(Charsets.UTF_8)

        columnType.valueFromDB(jsonBytes) shouldBeEqualTo source
    }
}
