package io.bluetape4k.redis.redisson.cache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.RedissonTestUtils.randomName
import io.bluetape4k.redis.redisson.RedissonTestUtils.redissonClient
import io.bluetape4k.assertions.invoking
import org.redisson.api.RMap
import org.redisson.client.codec.StringCodec
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RedisCacheInvalidationStrategy")
class CacheInvalidationStrategyTest {

    companion object: KLogging()

    @Test
    fun `invalidate - 지정한 키들만 캐시에서 제거한다`() {
        val cache = redissonClient.getMap<String, String>(randomName())
        cache["a"] = "1"
        cache["b"] = "2"
        cache["c"] = "3"

        val strategy = RedisCacheInvalidationStrategy(cache)
        strategy.invalidate("a", "b")

        cache.containsKey("a").shouldBeFalse()
        cache.containsKey("b").shouldBeFalse()
        cache.containsKey("c").shouldBeTrue()
    }

    @Test
    fun `invalidate - 존재하지 않는 키 제거는 안전하게 처리된다`() {
        val cache = redissonClient.getMap<String, String>(randomName())
        cache["a"] = "1"

        val strategy = RedisCacheInvalidationStrategy(cache)
        strategy.invalidate("missing-1", "missing-2")

        cache.containsKey("a").shouldBeTrue()
        cache.size shouldBeEqualTo 1
    }

    @Test
    fun `invalidateAll - 모든 항목을 제거한다`() {
        val cache = redissonClient.getMap<String, String>(randomName())
        repeat(10) { cache["key-$it"] = "value-$it" }

        val strategy = RedisCacheInvalidationStrategy(cache)
        strategy.invalidateAll()

        cache.size shouldBeEqualTo 0
    }

    private fun stringMap(name: String): RMap<String, String> =
        redissonClient.getMap(name, StringCodec.INSTANCE)

    @Test
    fun `invalidateByPattern - 패턴 일치 키만 제거한다`() {
        // Redisson keySet(pattern) 은 Redis glob 패턴을 사용하므로
        // 키가 StringCodec 으로 직렬화되어야 패턴 매칭이 가능하다.
        val cache = stringMap(randomName())
        cache["user:1"] = "alice"
        cache["user:2"] = "bob"
        cache["product:1"] = "phone"
        cache["product:2"] = "laptop"

        val strategy = RedisCacheInvalidationStrategy(cache)
        strategy.invalidateByPattern("user:*")

        cache.containsKey("user:1").shouldBeFalse()
        cache.containsKey("user:2").shouldBeFalse()
        cache.containsKey("product:1").shouldBeTrue()
        cache.containsKey("product:2").shouldBeTrue()
    }

    @Test
    fun `invalidateByPattern - 일치하는 키가 없으면 아무 것도 하지 않는다`() {
        val cache = stringMap(randomName())
        cache["user:1"] = "alice"
        cache["user:2"] = "bob"

        val strategy = RedisCacheInvalidationStrategy(cache)
        strategy.invalidateByPattern("product:*")

        cache.size shouldBeEqualTo 2
    }

    @Test
    fun `invalidateByPattern - 빈 패턴은 IllegalArgumentException 을 던진다`() {
        val cache = redissonClient.getMap<String, String>(randomName())
        val strategy = RedisCacheInvalidationStrategy(cache)

        invoking { strategy.invalidateByPattern("") } shouldThrow IllegalArgumentException::class
        invoking { strategy.invalidateByPattern("  ") } shouldThrow IllegalArgumentException::class
    }
}
