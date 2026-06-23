package io.bluetape4k.spring.redis.serializer

import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.support.emptyByteArray
import org.junit.jupiter.api.Test
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

class RedisBinarySerializerTest: AbstractRedisSerializerTest() {

    @Test
    fun `Redis JDK serializer constants preserve deprecation warning`() {
        val expectedReplacements = mapOf(
            "Jdk" to "RedisBinarySerializers.Kryo",
            "GzipJdk" to "RedisBinarySerializers.GzipKryo",
            "LZ4Jdk" to "RedisBinarySerializers.LZ4Kryo",
            "SnappyJdk" to "RedisBinarySerializers.SnappyKryo",
            "ZstdJdk" to "RedisBinarySerializers.ZstdKryo",
        )
        val properties = RedisBinarySerializers::class.memberProperties.associateBy { it.name }
        val invalidDeprecations = expectedReplacements
            .filter { (propertyName, replacement) ->
                val deprecation = properties[propertyName]?.findAnnotation<Deprecated>()
                deprecation == null ||
                    !deprecation.message.contains("JDK deserialization can expose Redis values to RCE gadget-chain risk") ||
                    deprecation.replaceWith.expression != replacement
            }
            .keys

        invalidDeprecations.toList().shouldBeEmpty()
        RedisBinarySerializers::class.memberProperties
            .filter { it.name.endsWith("Jdk") && it.name !in expectedReplacements.keys }
            .shouldBeEmpty()
        expectedReplacements.values shouldContain "RedisBinarySerializers.Kryo"
        expectedReplacements.values shouldContain "RedisBinarySerializers.ZstdKryo"
    }

    @Test
    fun `null 직렬화는 emptyByteArray 를 반환한다`() {
        val serializer = RedisBinarySerializer(BinarySerializers.LZ4Fory)
        serializer.serialize(null) shouldBeEqualTo emptyByteArray
    }

    @Test
    fun `null 역직렬화는 null을 반환한다`() {
        val serializer = RedisBinarySerializer(BinarySerializers.LZ4Fory)
        serializer.deserialize(null).shouldBeNull()
    }

    @Test
    fun `String을 LZ4Fory로 직렬화 후 복원한다`() {
        val serializer = RedisBinarySerializer(BinarySerializers.LZ4Fory)
        val original = "Hello, bluetape4k!"

        val bytes = serializer.serialize(original)
        bytes.shouldNotBeNull()

        val restored = serializer.deserialize(bytes)
        restored shouldBeEqualTo original
    }

    @Test
    fun `데이터 클래스를 LZ4Fory로 직렬화 후 복원한다`() {
        val serializer = RedisBinarySerializer(BinarySerializers.LZ4Fory)
        val original = TestData(id = 1L, name = "Alice", description = "테스트 데이터")

        val bytes = serializer.serialize(original)
        bytes.shouldNotBeNull()

        val restored = serializer.deserialize(bytes)
        restored shouldBeEqualTo original
    }

    @Test
    fun `데이터 클래스를 LZ4Kryo로 직렬화 후 복원한다`() {
        val serializer = RedisBinarySerializer(BinarySerializers.LZ4Kryo)
        val original = TestData(id = 2L, name = "Bob", description = "Kryo 직렬화 테스트")

        val bytes = serializer.serialize(original)
        bytes.shouldNotBeNull()

        val restored = serializer.deserialize(bytes)
        restored shouldBeEqualTo original
    }

    @Test
    fun `데이터 클래스를 ZstdFory로 직렬화 후 복원한다`() {
        val serializer = RedisBinarySerializer(BinarySerializers.ZstdFory)
        val original = TestData(id = 3L, name = "Carol", description = "Zstd 압축 테스트")

        val bytes = serializer.serialize(original)
        bytes.shouldNotBeNull()

        val restored = serializer.deserialize(bytes)
        restored shouldBeEqualTo original
    }

    @Test
    @Suppress("DEPRECATION")
    fun `데이터 클래스를 Jdk로 직렬화 후 복원한다`() {
        val serializer = RedisBinarySerializer(BinarySerializers.Jdk)
        val original = TestData(id = 4L, name = "Dave", description = "JDK 직렬화 테스트")

        val bytes = serializer.serialize(original)
        bytes.shouldNotBeNull()

        val restored = serializer.deserialize(bytes)
        restored shouldBeEqualTo original
    }

    @Test
    fun `리스트를 LZ4Fory로 직렬화 후 복원한다`() {
        val serializer = RedisBinarySerializer(BinarySerializers.LZ4Fory)
        val original = listOf(
            TestData(1L, "A", description = "first"),
            TestData(2L, "B", description = "second"),
            TestData(3L, "C", description = "third"),
        )

        val bytes = serializer.serialize(original)
        bytes.shouldNotBeNull()

        @Suppress("UNCHECKED_CAST")
        val restored = serializer.deserialize(bytes) as List<TestData>
        restored shouldBeEqualTo original
    }
}
