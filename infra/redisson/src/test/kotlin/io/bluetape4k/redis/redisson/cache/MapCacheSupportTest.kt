package io.bluetape4k.redis.redisson.cache

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.RedissonTestUtils.randomName
import io.bluetape4k.redis.redisson.RedissonTestUtils.redissonClient
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@DisplayName("mapCache DSL")
class MapCacheSupportTest {

    companion object: KLogging()

    @Test
    fun `mapCache - 기본 빌더로 RMapCache 를 생성한다`() {
        val cache = mapCache<String, String>(randomName(), redissonClient)
        cache.shouldNotBeNull()

        val key = "k1"
        cache.put(key, "v1", 1, TimeUnit.HOURS)
        cache[key] shouldBeEqualTo "v1"
    }

    @Test
    fun `mapCache - TTL 지정 시 만료된 엔트리는 조회되지 않는다`() {
        val cache = mapCache<String, String>(randomName(), redissonClient) {
            codec(RedissonCodecs.FastFory)
        }
        val key = "short-lived"

        cache.put(key, "value", 1000, TimeUnit.MILLISECONDS)
        // 만료 전엔 존재
        cache[key] shouldBeEqualTo "value"

        await atMost 5.seconds withPollInterval 100.milliseconds until {
            cache[key] == null
        }
        // 만료 후엔 null
        cache[key].shouldBeNull()
    }

    @Test
    fun `mapCache - 빈 이름은 IllegalArgumentException 을 던진다`() {
        assertFailsWith<IllegalArgumentException> {
            mapCache<String, String>("", redissonClient)
        }
    }
}
