package io.bluetape4k.io.serializer

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

/**
 * [KryoBinarySerializer.fast()] 사용 예제 및 검증 테스트
 *
 * `fast()`는 [com.esotericsoftware.kryo.serializers.FieldSerializer] 기반으로
 * [com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer]의 필드별 청크 헤더를 제거해
 * 처리량이 향상됩니다.
 *
 * ## 주의사항
 * - **Kotlin nullable 타입(`String?`, `Long?` 등) 포함 클래스는 사용 불가**: FieldSerializer가
 *   nullable 타입을 올바르게 처리하지 못해 역직렬화 오류가 발생합니다.
 * - **고정 스키마 전용**: 필드 추가·제거·순서 변경이 없는 DTO에만 사용합니다.
 * - 기본 [KryoBinarySerializer]로 직렬화한 데이터와 **포맷이 달라 호환되지 않습니다**.
 */
class KryoFastBinarySerializerTest {

    companion object: KLogging() {
        private val faker = Fakers.faker
        private const val REPEAT_SIZE = 5
    }

    private val serializer: BinarySerializer = KryoBinarySerializer.fast()

    /**
     * Kryo fast()와 함께 사용할 non-null 필드만 가진 고정 스키마 DTO.
     * nullable 필드(`?`)가 없어야 안전하게 직렬화됩니다.
     */
    data class NonNullableProduct(
        val id: Long,
        val name: String,
        val price: Double,
        val quantity: Int,
        val sku: String,
    )

    @RepeatedTest(REPEAT_SIZE)
    fun `fast 직렬화기로 non-null 필드 DTO를 직렬화-역직렬화할 수 있다`() {
        val expected = NonNullableProduct(
            id = faker.random().nextLong(),
            name = faker.commerce().productName(),
            price = faker.commerce().price().toDouble(),
            quantity = faker.random().nextInt(1, 1000),
            sku = faker.code().isbn10(),
        )

        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<NonNullableProduct>(bytes)
        actual.shouldNotBeNull() shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `fast 직렬화기로 String을 직렬화-역직렬화할 수 있다`() {
        val expected = faker.lorem().sentence()

        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<String>(bytes)
        actual.shouldNotBeNull() shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `fast 직렬화기로 Long을 직렬화-역직렬화할 수 있다`() {
        val expected = faker.random().nextLong()

        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<Long>(bytes)
        actual.shouldNotBeNull() shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `fast 직렬화기로 컬렉션을 직렬화-역직렬화할 수 있다`() {
        val expected = (1..20).map {
            NonNullableProduct(
                id = it.toLong(),
                name = faker.commerce().productName(),
                price = faker.commerce().price().toDouble(),
                quantity = faker.random().nextInt(1, 500),
                sku = faker.code().isbn10(),
            )
        }

        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<List<NonNullableProduct>>(bytes)
        actual.shouldNotBeNull() shouldBeEqualTo expected
    }

    @Test
    fun `fast 직렬화기끼리는 상호 호환된다`() {
        val serializer1 = KryoBinarySerializer.fast()
        val serializer2 = KryoBinarySerializer.fast()

        val expected = NonNullableProduct(
            id = 1L,
            name = "Test Product",
            price = 9.99,
            quantity = 10,
            sku = "TEST-001",
        )

        val bytes = serializer1.serialize(expected)
        val actual = serializer2.deserialize<NonNullableProduct>(bytes)

        actual.shouldNotBeNull() shouldBeEqualTo expected
    }
}
