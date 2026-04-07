package io.bluetape4k.jackson.binary

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * Deprecated 직렬화기 래퍼([CborJsonSerializer], [IonJsonSerializer], [SmileJsonSerializer])의
 * 기본 동작을 검증하는 테스트입니다.
 *
 * deprecated 선언 후에도 실제 직렬화/역직렬화가 정상 동작하는지 확인합니다.
 */
@Suppress("DEPRECATION")
class DeprecatedSerializersTest {
    companion object: KLogging()

    private val cborSerializer = CborJsonSerializer()
    private val ionSerializer = IonJsonSerializer()
    private val smileSerializer = SmileJsonSerializer()

    @Test
    fun `CborJsonSerializer - 직렬화 역직렬화 왕복 테스트`() {
        val expected = Box(x = 3, y = 7)
        val bytes = cborSerializer.serialize(expected)
        bytes.size shouldBeGreaterThan 0
        val actual = cborSerializer.deserialize<Box>(bytes).shouldNotBeNull()
        actual shouldBeEqualTo expected
    }

    @Test
    fun `IonJsonSerializer - 직렬화 역직렬화 왕복 테스트`() {
        val expected = Box(x = 5, y = 9)
        val bytes = ionSerializer.serialize(expected)
        bytes.size shouldBeGreaterThan 0
        val actual = ionSerializer.deserialize<Box>(bytes).shouldNotBeNull()
        actual shouldBeEqualTo expected
    }

    @Test
    fun `SmileJsonSerializer - 직렬화 역직렬화 왕복 테스트`() {
        val expected = Box(x = 1, y = 2)
        val bytes = smileSerializer.serialize(expected)
        bytes.size shouldBeGreaterThan 0
        val actual = smileSerializer.deserialize<Box>(bytes).shouldNotBeNull()
        actual shouldBeEqualTo expected
    }

    @Test
    fun `CborJsonSerializer - 중첩 객체 직렬화`() {
        val expected = Rectangle(topLeft = Point(0, 10), bottomRight = Point(10, 0))
        val bytes = cborSerializer.serialize(expected)
        bytes.size shouldBeGreaterThan 0
        val actual = cborSerializer.deserialize<Rectangle>(bytes).shouldNotBeNull()
        actual shouldBeEqualTo expected
    }

    @Test
    fun `IonJsonSerializer - 중첩 객체 직렬화`() {
        val expected = Rectangle(topLeft = Point(0, 10), bottomRight = Point(10, 0))
        val bytes = ionSerializer.serialize(expected)
        bytes.size shouldBeGreaterThan 0
        val actual = ionSerializer.deserialize<Rectangle>(bytes).shouldNotBeNull()
        actual shouldBeEqualTo expected
    }

    @Test
    fun `SmileJsonSerializer - 중첩 객체 직렬화`() {
        val expected = Rectangle(topLeft = Point(0, 10), bottomRight = Point(10, 0))
        val bytes = smileSerializer.serialize(expected)
        bytes.size shouldBeGreaterThan 0
        val actual = smileSerializer.deserialize<Rectangle>(bytes).shouldNotBeNull()
        actual shouldBeEqualTo expected
    }

    @Test
    fun `CborJsonSerializer - defaultMapper 사용 시 동일 결과`() {
        val serializerWithDefaultMapper = CborJsonSerializer(JacksonBinary.CBOR.defaultMapper)
        val expected = IdDesc(id = "test-id", desc = "설명")
        val bytes = serializerWithDefaultMapper.serialize(expected)
        bytes.size shouldBeGreaterThan 0
        val actual = serializerWithDefaultMapper.deserialize<IdDesc>(bytes).shouldNotBeNull()
        actual shouldBeEqualTo expected
    }

    @Test
    fun `SmileJsonSerializer - defaultMapper 사용 시 동일 결과`() {
        val serializerWithDefaultMapper = SmileJsonSerializer(JacksonBinary.Smile.defaultMapper)
        val expected = IdDesc(id = "test-id", desc = "설명")
        val bytes = serializerWithDefaultMapper.serialize(expected)
        bytes.size shouldBeGreaterThan 0
        val actual = serializerWithDefaultMapper.deserialize<IdDesc>(bytes).shouldNotBeNull()
        actual shouldBeEqualTo expected
    }

    @Test
    fun `IonJsonSerializer - defaultMapper 사용 시 동일 결과`() {
        val serializerWithDefaultMapper = IonJsonSerializer(JacksonBinary.ION.defaultMapper)
        val expected = IdDesc(id = "test-id", desc = "설명")
        val bytes = serializerWithDefaultMapper.serialize(expected)
        bytes.size shouldBeGreaterThan 0
        val actual = serializerWithDefaultMapper.deserialize<IdDesc>(bytes).shouldNotBeNull()
        actual shouldBeEqualTo expected
    }
}
