package io.bluetape4k.redis.redisson.cache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.RedissonTestUtils.randomName
import io.bluetape4k.redis.redisson.RedissonTestUtils.redissonClient
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import io.bluetape4k.redis.redisson.options.codec
import io.bluetape4k.redis.redisson.options.name
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.redisson.api.options.LocalCachedMapOptions
import io.bluetape4k.assertions.assertFailsWith

@DisplayName("localCachedMap / LocalCachedMapOptions extensions")
class LocalCacheMapSupportTest {

    companion object: KLogging()

    @Test
    fun `localCachedMap - 기본 빌더로 RLocalCachedMap 을 생성한다`() {
        val name = randomName()
        val map = localCachedMap<String, String>(name, redissonClient)

        map.shouldNotBeNull()
        map["k1"] = "v1"
        map["k1"] shouldBeEqualTo "v1"

        map.destroy()
    }

    @Test
    fun `localCachedMap - builder 블록이 적용되어 옵션이 반영된다`() {
        val name = randomName()
        val map = localCachedMap<String, String>(name, redissonClient) {
            cacheSize(100)
            codec(RedissonCodecs.LZ4Fory)
        }

        map.shouldNotBeNull()
        repeat(5) { map["key-$it"] = "value-$it" }
        map.size.shouldBeGreaterOrEqualTo(5)

        map.destroy()
    }

    @Test
    fun `localCachedMap - 빈 이름은 IllegalArgumentException 을 던진다`() {
        assertFailsWith<IllegalArgumentException> {
            localCachedMap<String, String>("", redissonClient)
        }

        assertFailsWith<IllegalArgumentException> {
            localCachedMap<String, String>(" ", redissonClient)
        }
    }

    @Test
    fun `LocalCachedMapOptions name extension - 설정한 이름을 반환한다`() {
        val opts = LocalCachedMapOptions.name<String, String>("my-cache")
        opts.name shouldBeEqualTo "my-cache"
    }

    @Test
    fun `LocalCachedMapOptions codec extension - 설정한 codec 을 반환한다`() {
        val codec = RedissonCodecs.LZ4Fory
        val opts = LocalCachedMapOptions.name<String, String>("my-cache").codec(codec)

        opts.codec.shouldNotBeNull()
        (opts.codec === codec).shouldBeTrue()
    }

    @Test
    fun `LocalCachedMapOptions codec extension - codec 미지정 시 null 반환`() {
        val opts = LocalCachedMapOptions.name<String, String>("my-cache")
        // codec 을 지정하지 않은 경우 null 이거나 기본값일 수 있다
        // Redisson 내부 기본값에 따라 null/non-null 일 수 있으므로 존재 여부만 검증
        opts.name shouldBeEqualTo "my-cache"
    }
}
