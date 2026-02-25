package io.bluetape4k.ignite3.cache

import io.bluetape4k.cache.nearcache.NearCacheConfig
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * [IgniteNearCacheConfig]의 기본값과 팩토리 메서드를 검증하는 테스트입니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Ignite3NearCacheConfigTest {

    @Test
    fun `기본값으로 생성된 IgniteNearCacheConfig 검증`() {
        val config = igniteNearCacheConfig<Long, String>(tableName = "TEST_TABLE")

        config.tableName shouldBeEqualTo "TEST_TABLE"
        config.keyType shouldBeEqualTo Long::class.javaObjectType
        config.valueType shouldBeEqualTo String::class.java
        config.keyColumn shouldBeEqualTo "ID"
        config.valueColumn shouldBeEqualTo "DATA"
        config.isSynchronous.shouldBeFalse()
        config.checkExpiryPeriod shouldBeEqualTo NearCacheConfig.DEFAULT_EXPIRY_CHECK_PERIOD
    }

    @Test
    fun `readOnly 팩토리로 생성한 설정이 긴 만료 주기와 비동기 모드를 가짐`() {
        val config = IgniteNearCacheConfig.readOnly<Long, String>("RO_TABLE")

        config.tableName shouldBeEqualTo "RO_TABLE"
        config.isSynchronous.shouldBeFalse()
        config.checkExpiryPeriod shouldBeEqualTo 60_000L
    }

    @Test
    fun `writeOptimized 팩토리로 생성한 설정이 짧은 만료 주기와 비동기 모드를 가짐`() {
        val config = IgniteNearCacheConfig.writeOptimized<Long, String>("WO_TABLE")

        config.tableName shouldBeEqualTo "WO_TABLE"
        config.isSynchronous.shouldBeFalse()
        config.checkExpiryPeriod shouldBeEqualTo 10_000L
    }

    @Test
    fun `커스텀 컬럼 이름으로 설정 생성 검증`() {
        val config = igniteNearCacheConfig<Long, String>(
            tableName = "CUSTOM_TABLE",
            keyColumn = "MY_KEY",
            valueColumn = "MY_VALUE",
        )

        config.tableName shouldBeEqualTo "CUSTOM_TABLE"
        config.keyColumn shouldBeEqualTo "MY_KEY"
        config.valueColumn shouldBeEqualTo "MY_VALUE"
    }

    @Test
    fun `동일한 tableName, keyType, valueType을 가진 설정은 equals`() {
        val config1 = igniteNearCacheConfig<Long, String>(tableName = "SAME_TABLE")
        val config2 = igniteNearCacheConfig<Long, String>(tableName = "SAME_TABLE")

        (config1 == config2).shouldBeTrue()
    }

    @Test
    fun `다른 tableName을 가진 설정은 not equals`() {
        val config1 = igniteNearCacheConfig<Long, String>(tableName = "TABLE_A")
        val config2 = igniteNearCacheConfig<Long, String>(tableName = "TABLE_B")

        (config1 == config2).shouldBeFalse()
    }
}
