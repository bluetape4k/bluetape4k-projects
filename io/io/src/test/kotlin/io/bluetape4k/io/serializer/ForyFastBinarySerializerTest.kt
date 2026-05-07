package io.bluetape4k.io.serializer

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

/**
 * [ForyBinarySerializer.fast()] 사용 예제 및 검증 테스트
 *
 * `fast()`는 SCHEMA_CONSISTENT + refTracking=false로 최적화되어 기본 대비 약 +70% 처리량 향상.
 *
 * ## 특징
 * - Fory는 nullable 타입을 지원하므로 `String?` 등 nullable 필드도 직렬화 가능
 * - SCHEMA_CONSISTENT 모드: 기본 COMPATIBLE 포맷과 **호환되지 않음**
 * - 고정 스키마 DTO, 휘발성 캐시 환경(Redis·메시지큐)에 적합
 */
class ForyFastBinarySerializerTest {

    companion object: KLogging() {
        private val faker = Fakers.faker
        private const val REPEAT_SIZE = 5
    }

    private val serializer: BinarySerializer = ForyBinarySerializer.fast()

    /**
     * Fory fast()가 지원하는 nullable 필드 포함 DTO.
     * ForyBinarySerializer.fast()는 Kryo.fast()와 달리 nullable 타입도 처리 가능합니다.
     */
    data class CacheableDto(
        val id: Long,
        val name: String,
        val score: Double,
        val tag: String? = null,
    )

    @RepeatedTest(REPEAT_SIZE)
    fun `fast 직렬화기로 non-null DTO를 직렬화-역직렬화할 수 있다`() {
        val expected = CacheableDto(
            id = faker.random().nextLong(),
            name = faker.name().fullName(),
            score = faker.random().nextDouble(),
        )

        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<CacheableDto>(bytes)
        actual.shouldNotBeNull() shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `fast 직렬화기로 nullable 필드를 가진 DTO도 직렬화-역직렬화할 수 있다`() {
        val expected = CacheableDto(
            id = faker.random().nextLong(),
            name = faker.name().fullName(),
            score = faker.random().nextDouble(),
            tag = faker.lorem().word(),
        )

        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<CacheableDto>(bytes)
        actual.shouldNotBeNull() shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `fast 직렬화기로 컬렉션을 직렬화-역직렬화할 수 있다`() {
        val expected = (1..10).map {
            CacheableDto(
                id = it.toLong(),
                name = faker.name().fullName(),
                score = faker.random().nextDouble(),
                tag = if (it % 2 == 0) faker.lorem().word() else null,
            )
        }

        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<List<CacheableDto>>(bytes)
        actual.shouldNotBeNull() shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `fast 직렬화기로 기본 타입을 직렬화-역직렬화할 수 있다`() {
        val expected = faker.lorem().sentence()

        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<String>(bytes)
        actual.shouldNotBeNull() shouldBeEqualTo expected
    }

    @Test
    fun `fast 직렬화기끼리는 상호 호환된다`() {
        val serializer1 = ForyBinarySerializer.fast()
        val serializer2 = ForyBinarySerializer.fast()

        val expected = CacheableDto(id = 42L, name = "cache-item", score = 3.14)

        val bytes = serializer1.serialize(expected)
        val actual = serializer2.deserialize<CacheableDto>(bytes)

        actual.shouldNotBeNull() shouldBeEqualTo expected
    }
}
