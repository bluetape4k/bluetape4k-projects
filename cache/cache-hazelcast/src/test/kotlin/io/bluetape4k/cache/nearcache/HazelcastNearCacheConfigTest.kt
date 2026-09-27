package io.bluetape4k.cache.nearcache

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.io.lookup
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import java.time.Duration

class HazelcastNearCacheConfigTest {

    companion object: KLogging()

    @Test
    fun `유효한 near cache 설정은 그대로 생성된다`() {
        val config = HazelcastNearCacheConfig(
            cacheName = "hz-near",
            maxLocalSize = 1_000,
            frontExpireAfterWrite = Duration.ofSeconds(30),
            frontExpireAfterAccess = Duration.ofSeconds(10),
            recordStats = true,
        )

        config.cacheName shouldBeEqualTo "hz-near"
        config.maxLocalSize shouldBeEqualTo 1_000
        config.frontExpireAfterWrite shouldBeEqualTo Duration.ofSeconds(30)
        config.frontExpireAfterAccess shouldBeEqualTo Duration.ofSeconds(10)
    }

    @Test
    fun `cacheName maxLocalSize duration 은 유효해야 한다`() {
        assertFailsWith<IllegalArgumentException> {
            HazelcastNearCacheConfig(cacheName = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            HazelcastNearCacheConfig(maxLocalSize = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            HazelcastNearCacheConfig(frontExpireAfterWrite = Duration.ZERO)
        }
        assertFailsWith<IllegalArgumentException> {
            HazelcastNearCacheConfig(frontExpireAfterAccess = Duration.ZERO)
        }
    }

    @Test
    fun `설정은 Java serialization round trip과 명시 UID를 유지한다`() {
        val config = HazelcastNearCacheConfig(
            cacheName = "users",
            maxLocalSize = 500,
            frontExpireAfterWrite = Duration.ofMinutes(5),
            frontExpireAfterAccess = Duration.ofMinutes(1),
            recordStats = true,
        )

        deserialize<HazelcastNearCacheConfig>(serialize(config)) shouldBeEqualTo config
        HazelcastNearCacheConfig::class.lookup().serialVersionUID shouldBeEqualTo 1L
    }

    private fun serialize(value: Any): ByteArray =
        BinarySerializers.FastFory.serialize(value)


    private inline fun <reified T: Any> deserialize(bytes: ByteArray): T =
        BinarySerializers.FastFory.deserialize<T>(bytes).shouldNotBeNull()
}
