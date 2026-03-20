package io.bluetape4k.spring.redis.serializer

import io.bluetape4k.io.serializer.BinarySerializers
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class RedisBinarySerializerTest: AbstractRedisSerializerTest() {

    @Test
    fun `null 직렬화는 null을 반환한다`() {
        val serializer = RedisBinarySerializer(BinarySerializers.LZ4Fory)
        serializer.serialize(null).shouldBeNull()
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
