package io.bluetape4k.io.serializer

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * 보안 설정의 [KryoBinarySerializer] 사용 예제 테스트
 *
 * ## 언제 사용하나?
 * 신뢰할 수 없는 데이터 소스(외부 API, 사용자 입력 등)에서 역직렬화가 필요할 때는
 * [KryoBinarySerializer.secure]로 허용할 클래스를 명시적으로 등록하여 사용합니다.
 *
 * ## 기본 설정 vs 보안 설정
 * | 설정 | 클래스 등록 | 미등록 클래스 처리 | 적합한 환경 |
 * |------|------------|------------------|------------|
 * | 기본 (`KryoBinarySerializer()`) | 불필요 | 허용 | 내부 캐시, 신뢰된 마이크로서비스 |
 * | 보안 (`KryoBinarySerializer.secure(...)`) | 필수 | 예외 발생 | 외부 API, 신뢰 불가 데이터 수신 |
 */
class SecureKryoBinarySerializerTest {

    companion object: KLogging() {
        private val faker = Fakers.faker
    }

    // 직렬화를 허용할 클래스 (등록 대상)
    data class AllowedProduct(val id: Long, val name: String, val price: Double)

    // 등록하지 않은 클래스 (직렬화 불가)
    data class UnregisteredCart(val id: Long, val item: String)

    /**
     * 보안 Kryo: 명시적으로 등록한 클래스만 직렬화/역직렬화를 허용합니다.
     *
     * - [KryoBinarySerializer.secure]에 허용할 클래스를 전달합니다.
     * - 내부적으로 `isRegistrationRequired = true`인 Kryo 풀을 생성합니다.
     */
    private val serializer = KryoBinarySerializer.secure(
        AllowedProduct::class.java,
    )

    @Test
    fun `등록된 클래스는 직렬화-역직렬화가 가능하다`() {
        val expected = AllowedProduct(
            id = faker.random().nextLong(),
            name = faker.commerce().productName(),
            price = faker.random().nextDouble() * 100,
        )

        val bytes = serializer.serialize(expected)
        val actual = serializer.deserialize<AllowedProduct>(bytes)

        actual.shouldNotBeNull() shouldBeEqualTo expected
    }

    @Test
    fun `등록되지 않은 클래스는 직렬화 시 BinarySerializationException이 발생한다`() {
        val unregistered = UnregisteredCart(id = 1L, item = "apple")

        assertFailsWith<BinarySerializationException> {
            serializer.serialize(unregistered)
        }
    }

    @Test
    fun `기본 KryoBinarySerializer는 등록 없이 모든 클래스를 직렬화할 수 있다`() {
        // 비교: 기본 직렬화기는 클래스 등록 없이 사용 가능
        val defaultSerializer = KryoBinarySerializer()
        val cart = UnregisteredCart(id = 99L, item = "banana")

        val bytes = defaultSerializer.serialize(cart)
        val actual = defaultSerializer.deserialize<UnregisteredCart>(bytes)

        actual.shouldNotBeNull() shouldBeEqualTo cart
    }
}
