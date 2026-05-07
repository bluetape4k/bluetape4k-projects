package io.bluetape4k.redis.redisson.cache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.RedissonTestUtils.randomName
import io.bluetape4k.redis.redisson.RedissonTestUtils.redissonClient
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import io.bluetape4k.assertions.invoking
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

@DisplayName("mapCache DSL")
class MapCacheSupportTest {

    companion object: KLogging()

    @Test
    fun `mapCache - 기본 빌더로 RMapCache 를 생성한다`() {
        val cache = mapCache<String, String>(randomName(), redissonClient)

        cache.shouldNotBeNull()
        cache.put("k1", "v1", 1, TimeUnit.HOURS)
        cache["k1"] shouldBeEqualTo "v1"
    }

    @Test
    fun `mapCache - TTL 지정 시 만료된 엔트리는 조회되지 않는다`() {
        val cache = mapCache<String, String>(randomName(), redissonClient) {
            codec(RedissonCodecs.LZ4Fory)
        }

        cache.put("short-lived", "value", 100, TimeUnit.MILLISECONDS)
        // 만료 전엔 존재
        cache["short-lived"] shouldBeEqualTo "value"

        Thread.sleep(300)
        // 만료 후엔 null
        cache["short-lived"].shouldBeNull()
    }

    @Test
    fun `mapCache - 빈 이름은 IllegalArgumentException 을 던진다`() {
        invoking {
            mapCache<String, String>("", redissonClient)
        } shouldThrow IllegalArgumentException::class
    }
}
