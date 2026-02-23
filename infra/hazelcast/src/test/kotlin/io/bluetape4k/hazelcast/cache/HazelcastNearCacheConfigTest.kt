package io.bluetape4k.hazelcast.cache

import com.hazelcast.config.EvictionPolicy
import com.hazelcast.config.InMemoryFormat
import com.hazelcast.config.NearCacheConfig
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * [HazelcastNearCacheConfig]의 기본값과 [NearCacheConfig] 변환을 검증하는 테스트입니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HazelcastNearCacheConfigTest {

    @Test
    fun `기본값으로 생성된 HazelcastNearCacheConfig 검증`() {
        val config = HazelcastNearCacheConfig(mapName = "test-map")

        config.mapName shouldBeEqualTo "test-map"
        config.timeToLiveSeconds shouldBeEqualTo 60
        config.maxIdleSeconds shouldBeEqualTo 120
        config.maxSize shouldBeEqualTo 10_000
        config.evictionPolicy shouldBeEqualTo EvictionPolicy.LRU
        config.invalidateOnChange.shouldBeTrue()
        config.inMemoryFormat shouldBeEqualTo InMemoryFormat.BINARY
        config.localUpdatePolicy shouldBeEqualTo NearCacheConfig.LocalUpdatePolicy.CACHE_ON_UPDATE
    }

    @Test
    fun `default 팩토리로 생성한 설정이 기본값과 동일함`() {
        val config = HazelcastNearCacheConfig.default("my-map")

        config.mapName shouldBeEqualTo "my-map"
        config.timeToLiveSeconds shouldBeEqualTo 60
        config.maxIdleSeconds shouldBeEqualTo 120
        config.maxSize shouldBeEqualTo 10_000
        config.evictionPolicy shouldBeEqualTo EvictionPolicy.LRU
        config.invalidateOnChange.shouldBeTrue()
    }

    @Test
    fun `readOnly 팩토리로 생성한 설정이 읽기 전용 값을 가짐`() {
        val config = HazelcastNearCacheConfig.readOnly("read-only-map")

        config.mapName shouldBeEqualTo "read-only-map"
        config.timeToLiveSeconds shouldBeEqualTo 3600
        config.maxIdleSeconds shouldBeEqualTo 0
        config.invalidateOnChange.shouldBeFalse()
    }

    @Test
    fun `toNearCacheConfig 변환 시 NearCacheConfig가 올바르게 생성됨`() {
        val config = HazelcastNearCacheConfig(
            mapName = "convert-map",
            timeToLiveSeconds = 30,
            maxIdleSeconds = 60,
            maxSize = 500,
            evictionPolicy = EvictionPolicy.LFU,
            invalidateOnChange = false,
        )

        val nearCacheConfig = config.toNearCacheConfig()

        nearCacheConfig.shouldNotBeNull()
        nearCacheConfig.name shouldBeEqualTo "convert-map"
        nearCacheConfig.timeToLiveSeconds shouldBeEqualTo 30
        nearCacheConfig.maxIdleSeconds shouldBeEqualTo 60
        nearCacheConfig.isInvalidateOnChange.shouldBeFalse()
        nearCacheConfig.evictionConfig.shouldNotBeNull()
        nearCacheConfig.evictionConfig.size shouldBeEqualTo 500
    }

    @Test
    fun `readOnly 설정의 toNearCacheConfig 변환 검증`() {
        val config = HazelcastNearCacheConfig.readOnly("ro-map")
        val nearCacheConfig = config.toNearCacheConfig()

        nearCacheConfig.shouldNotBeNull()
        nearCacheConfig.name shouldBeEqualTo "ro-map"
        nearCacheConfig.timeToLiveSeconds shouldBeEqualTo 3600
        nearCacheConfig.maxIdleSeconds shouldBeEqualTo 0
        nearCacheConfig.isInvalidateOnChange.shouldBeFalse()
    }
}
