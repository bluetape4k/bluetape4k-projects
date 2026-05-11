package io.bluetape4k.cache.jcache

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.Test
import java.util.*
import javax.cache.configuration.MutableConfiguration

class RedissonSuspendJCacheTest: AbstractSuspendJCacheTest() {

    companion object: KLoggingChannel()

    override val suspendJCache: SuspendJCache<String, Any> =
        RedissonSuspendJCache(
            "redis-suspend-cache-" + UUID.randomUUID().encodeBase62(),
            RedisServers.redisson,
            MutableConfiguration()
        )

    @Test
    fun `close releases wrapper but keeps Redisson JCache data`() = runSuspendIO {
        val cacheName = "redis-suspend-cache-close-" + UUID.randomUUID().encodeBase62()
        val configuration = MutableConfiguration<String, String>().apply {
            setTypes(String::class.java, String::class.java)
        }
        val cache = RedissonSuspendJCache(cacheName, RedisServers.redisson, configuration)
        cache.put("close-key", "close-value")
        cache.close()

        val reopened = RedissonSuspendJCache(cacheName, RedisServers.redisson, configuration)
        try {
            // close()는 resource lifecycle 계약이며 저장된 cache entry 삭제는 clear()가 담당한다.
            reopened.get("close-key") shouldBeEqualTo "close-value"
        } finally {
            reopened.clear()
            reopened.close()
        }
    }
}
