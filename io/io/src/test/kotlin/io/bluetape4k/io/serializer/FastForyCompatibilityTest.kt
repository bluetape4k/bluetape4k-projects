package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.Serializable

/**
 * [BinarySerializers.FastFory] 및 압축 조합 직렬화기의 roundtrip 검증과
 * 기본 Fory(COMPATIBLE 모드)와의 와이어 포맷 비호환성을 검증하는 테스트.
 */
class FastForyCompatibilityTest {

    companion object : KLogging()

    data class TestDomain(
        val id: Long,
        val name: String,
        val value: Double,
    ) : Serializable

    data class DomainWithNullable(
        val id: Long,
        val name: String?,
        val tags: List<String>?,
    ) : Serializable

    data class Nested(
        val inner: TestDomain,
        val label: String,
    ) : Serializable

    private val sample = TestDomain(id = 1L, name = "bluetape4k", value = 3.14)

    // ─── roundtrip tests ────────────────────────────────────────────────────

    @Test
    fun `FastFory roundtrip 성공`() {
        val serializer = BinarySerializers.FastFory

        val bytes = serializer.serialize(sample)
        bytes.shouldNotBeEmpty()

        val restored = serializer.deserialize<TestDomain>(bytes)
        log.debug { "restored=$restored" }
        restored.shouldNotBeNull() shouldBeEqualTo sample
    }

    @Test
    fun `LZ4FastFory roundtrip 성공`() {
        val serializer = BinarySerializers.LZ4FastFory

        val bytes = serializer.serialize(sample)
        bytes.shouldNotBeEmpty()

        val restored = serializer.deserialize<TestDomain>(bytes)
        log.debug { "LZ4FastFory restored=$restored" }
        restored.shouldNotBeNull() shouldBeEqualTo sample
    }

    @Test
    fun `ZstdFastFory roundtrip 성공`() {
        val serializer = BinarySerializers.ZstdFastFory

        val bytes = serializer.serialize(sample)
        bytes.shouldNotBeEmpty()

        val restored = serializer.deserialize<TestDomain>(bytes)
        log.debug { "ZstdFastFory restored=$restored" }
        restored.shouldNotBeNull() shouldBeEqualTo sample
    }

    @Test
    fun `SnappyFastFory roundtrip 성공`() {
        val serializer = BinarySerializers.SnappyFastFory

        val bytes = serializer.serialize(sample)
        bytes.shouldNotBeEmpty()

        val restored = serializer.deserialize<TestDomain>(bytes)
        log.debug { "SnappyFastFory restored=$restored" }
        restored.shouldNotBeNull() shouldBeEqualTo sample
    }

    // ─── edge case tests ────────────────────────────────────────────────────

    @Test
    fun `FastFory - null 필드 roundtrip 성공`() {
        val obj = DomainWithNullable(id = 1L, name = null, tags = null)
        val bytes = BinarySerializers.FastFory.serialize(obj)
        val restored = BinarySerializers.FastFory.deserialize<DomainWithNullable>(bytes)
        restored.shouldNotBeNull() shouldBeEqualTo obj
        restored.name.shouldBeNull()
        restored.tags.shouldBeNull()
    }

    @Test
    fun `FastFory - 빈 컬렉션 roundtrip 성공`() {
        val obj = DomainWithNullable(id = 2L, name = "empty", tags = emptyList())
        val bytes = BinarySerializers.FastFory.serialize(obj)
        val restored = BinarySerializers.FastFory.deserialize<DomainWithNullable>(bytes)
        restored.shouldNotBeNull() shouldBeEqualTo obj
        restored.tags!!.shouldNotBeNull()
    }

    @Test
    fun `FastFory - 대형 컬렉션 roundtrip 성공`() {
        val obj = DomainWithNullable(id = 3L, name = "large", tags = List(10_000) { "tag-$it" })
        val bytes = BinarySerializers.FastFory.serialize(obj)
        val restored = BinarySerializers.FastFory.deserialize<DomainWithNullable>(bytes)
        restored.shouldNotBeNull()
        restored.tags!!.shouldContainSame(obj.tags!!)
    }

    @Test
    fun `FastFory - 중첩 객체 roundtrip 성공`() {
        val obj = Nested(inner = sample, label = "nested-test")
        val bytes = BinarySerializers.FastFory.serialize(obj)
        val restored = BinarySerializers.FastFory.deserialize<Nested>(bytes)
        restored.shouldNotBeNull() shouldBeEqualTo obj
    }

    // ─── cross-codec incompatibility tests ──────────────────────────────────

    /**
     * 방향 A: 기본 Fory(COMPATIBLE)로 직렬화 → FastFory(SCHEMA_CONSISTENT)로 역직렬화 → 예외 발생.
     *
     * io/io 경로는 fallback이 없으므로 반드시 예외가 발생해야 합니다.
     */
    @Test
    fun `방향A - Fory로 직렬화한 데이터를 FastFory로 역직렬화하면 예외 발생`() {
        val foryBytes = BinarySerializers.Fory.serialize(sample)

        assertThrows<Exception> {
            BinarySerializers.FastFory.deserialize<TestDomain>(foryBytes)
        }
    }

    /**
     * 방향 B: FastFory(SCHEMA_CONSISTENT)로 직렬화 → 기본 Fory(COMPATIBLE)로 역직렬화 → 예외 발생.
     */
    @Test
    fun `방향B - FastFory로 직렬화한 데이터를 Fory로 역직렬화하면 예외 발생`() {
        val fastForyBytes = BinarySerializers.FastFory.serialize(sample)

        assertThrows<Exception> {
            BinarySerializers.Fory.deserialize<TestDomain>(fastForyBytes)
        }
    }
}
