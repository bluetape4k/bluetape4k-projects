package io.bluetape4k.spring.redis.serializer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable
import java.util.stream.Stream

/**
 * Redis serializer가 실제 저장 바이트 경계에서 Fory Kotlin metadata payload를 보존하는지 확인한다.
 *
 * FastFory는 휘발성 `SCHEMA_CONSISTENT` 캐시 경로만 검증하며 기본 Fory와의 wire 호환성을 약속하지 않는다.
 */
class RedisForyWireCompatibilityTest : AbstractRedisSerializerTest() {

    @ParameterizedTest(name = "[{1}] Kotlin metadata payload roundtrip")
    @MethodSource("forySerializers")
    @Suppress("UNUSED_PARAMETER")
    fun `Redis Fory serializer paths preserve Kotlin metadata`(
        serializer: RedisBinarySerializer,
        name: String,
    ) {
        val original = RedisForyWireFixtures.sample()

        val bytes = serializer.serialize(original)
        bytes.shouldNotBeEmpty()

        serializer.deserialize(bytes) shouldBeEqualTo original
    }

    companion object {

        @JvmStatic
        fun forySerializers(): Stream<Arguments> = Stream.of(
            Arguments.of(RedisBinarySerializers.Fory, "Fory"),
            Arguments.of(RedisBinarySerializers.LZ4Fory, "LZ4Fory"),
            Arguments.of(RedisBinarySerializers.FastFory, "FastFory"),
        )
    }
}

@JvmInline
value class RedisForyWireOwnerId(val value: String)

data class RedisForyWireMetadata(
    val source: String = "issue-1639",
    val note: String? = null,
) : Serializable {
    private companion object {
        const val serialVersionUID: Long = 1L
    }
}

data class RedisForyWirePayload(
    val id: Long,
    val title: String = "fory-wire",
    val description: String? = null,
    val tags: List<String> = listOf("kotlin", "metadata", "redis"),
    val owner: RedisForyWireOwnerId = RedisForyWireOwnerId("owner-1639"),
    val metadata: RedisForyWireMetadata = RedisForyWireMetadata(),
) : Serializable {
    private companion object {
        const val serialVersionUID: Long = 1L
    }
}

object RedisForyWireFixtures {

    @JvmStatic
    fun sample(): RedisForyWirePayload = RedisForyWirePayload(id = 1639L)
}
