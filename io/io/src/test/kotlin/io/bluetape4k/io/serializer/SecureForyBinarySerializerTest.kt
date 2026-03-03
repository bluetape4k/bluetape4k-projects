package io.bluetape4k.io.serializer

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * 보안 설정의 [ForyBinarySerializer] 사용 예제 테스트
 *
 * ## 언제 사용하나?
 * 신뢰할 수 없는 데이터 소스(외부 API, 사용자 입력 등)에서 역직렬화가 필요할 때는
 * [ForyBinarySerializer.secureFory]로 허용할 클래스를 명시적으로 등록하여 사용합니다.
 *
 * ## 기본 설정 vs 보안 설정
 * | 설정 | 클래스 등록 | 미등록 클래스 처리 | 적합한 환경 |
 * |------|------------|------------------|------------|
 * | 기본 (`ForyBinarySerializer()`) | 불필요 | 허용 | 내부 캐시, 신뢰된 마이크로서비스 |
 * | 보안 (`secureFory(...)`) | 필수 | 예외 발생 | 외부 API, 신뢰 불가 데이터 수신 |
 */
class SecureForyBinarySerializerTest {

    companion object: KLogging() {
        private val faker = Fakers.faker
    }

    // 직렬화를 허용할 클래스 (등록 대상)
    data class AllowedPerson(val name: String, val age: Int)

    // 등록하지 않은 클래스 (직렬화 불가)
    data class UnregisteredOrder(val id: Long, val item: String)

    /**
     * 보안 Fory: 명시적으로 등록한 클래스만 직렬화/역직렬화를 허용합니다.
     *
     * - [ForyBinarySerializer.secureFory]에 허용할 클래스를 전달합니다.
     * - 생성된 [ThreadSafeFory]를 [ForyBinarySerializer]에 주입합니다.
     */
    private val serializer = ForyBinarySerializer(
        fory = ForyBinarySerializer.secureFory(
            AllowedPerson::class.java,
        )
    )

    @Test
    fun `등록된 클래스는 직렬화-역직렬화가 가능하다`() {
        val expected = AllowedPerson(
            name = faker.name().fullName(),
            age = faker.random().nextInt(18, 90),
        )

        val bytes = serializer.serialize(expected)
        val actual = serializer.deserialize<AllowedPerson>(bytes)

        actual.shouldNotBeNull() shouldBeEqualTo expected
    }

    @Test
    fun `등록되지 않은 클래스는 직렬화 시 BinarySerializationException이 발생한다`() {
        val unregistered = UnregisteredOrder(id = 1L, item = "laptop")

        assertFailsWith<BinarySerializationException> {
            serializer.serialize(unregistered)
        }
    }

    @Test
    fun `기본 ForyBinarySerializer는 등록 없이 모든 클래스를 직렬화할 수 있다`() {
        // 비교: 기본 직렬화기는 클래스 등록 없이 사용 가능
        val defaultSerializer = ForyBinarySerializer()
        val order = UnregisteredOrder(id = 42L, item = "keyboard")

        val bytes = defaultSerializer.serialize(order)
        val actual = defaultSerializer.deserialize<UnregisteredOrder>(bytes)

        actual.shouldNotBeNull() shouldBeEqualTo order
    }
}
