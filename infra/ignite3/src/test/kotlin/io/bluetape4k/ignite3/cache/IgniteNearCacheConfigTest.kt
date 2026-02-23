package io.bluetape4k.ignite3.cache

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Duration

/**
 * [IgniteNearCacheConfig]의 기본값과 팩토리 메서드를 검증하는 테스트입니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IgniteNearCacheConfigTest {

    @Test
    fun `기본값으로 생성된 IgniteNearCacheConfig 검증`() {
        val config = IgniteNearCacheConfig(tableName = "TEST_TABLE")

        config.tableName shouldBeEqualTo "TEST_TABLE"
        config.frontCacheMaxSize shouldBeEqualTo 10_000L
        config.frontCacheTtl shouldBeEqualTo Duration.ofMinutes(10)
        config.frontCacheMaxIdleTime shouldBeEqualTo Duration.ofMinutes(30)
        config.syncOnWrite.shouldBeTrue()
    }

    @Test
    fun `readOnly 팩토리로 생성한 설정이 긴 TTL과 비동기 모드를 가짐`() {
        val config = IgniteNearCacheConfig.readOnly("RO_TABLE")

        config.tableName shouldBeEqualTo "RO_TABLE"
        config.frontCacheTtl shouldBeEqualTo Duration.ofHours(1)
        config.frontCacheMaxIdleTime shouldBeEqualTo Duration.ZERO
        config.syncOnWrite.shouldBeFalse()
    }

    @Test
    fun `writeOptimized 팩토리로 생성한 설정이 짧은 TTL과 비동기 모드를 가짐`() {
        val config = IgniteNearCacheConfig.writeOptimized("WO_TABLE")

        config.tableName shouldBeEqualTo "WO_TABLE"
        config.frontCacheTtl shouldBeEqualTo Duration.ofMinutes(5)
        config.syncOnWrite.shouldBeFalse()
    }

    @Test
    fun `커스텀 값으로 설정 생성 검증`() {
        val config = IgniteNearCacheConfig(
            tableName = "CUSTOM_TABLE",
            frontCacheMaxSize = 5_000L,
            frontCacheTtl = Duration.ofSeconds(30),
            frontCacheMaxIdleTime = Duration.ofMinutes(5),
            syncOnWrite = false,
        )

        config.tableName shouldBeEqualTo "CUSTOM_TABLE"
        config.frontCacheMaxSize shouldBeEqualTo 5_000L
        config.frontCacheTtl shouldBeEqualTo Duration.ofSeconds(30)
        config.frontCacheMaxIdleTime shouldBeEqualTo Duration.ofMinutes(5)
        config.syncOnWrite.shouldBeFalse()
    }
}
