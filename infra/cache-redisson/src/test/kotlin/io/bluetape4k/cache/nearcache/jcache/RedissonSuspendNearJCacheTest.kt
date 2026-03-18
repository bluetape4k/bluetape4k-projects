package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.cache.jcache.CaffeineSuspendJCache
import io.bluetape4k.cache.jcache.RedissonSuspendJCache
import io.bluetape4k.cache.jcache.SuspendJCache
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.Disabled
import java.time.Duration

/**
 * Redisson JCache 백엔드를 사용하는 [AbstractSuspendNearJCacheTest] 구현체입니다.
 *
 * Caffeine front + Redisson SuspendCache back 2-tier SuspendNearCache 패턴을 검증합니다.
 */
@Disabled("버그가 많아 일단 테스트에서 제외한다.")
class RedissonSuspendNearJCacheTest: AbstractSuspendNearJCacheTest() {

    companion object: KLoggingChannel()

    override val backSuspendJCache: SuspendJCache<String, Any> =
        RedissonSuspendJCache(
            "redisson-jcache-suspend-back-" + Base58.randomString(12),
            RedisServers.redisson,
        )

    override fun createFrontSuspendCache(expireAfterAccess: Duration): SuspendJCache<String, Any> =
        CaffeineSuspendJCache {
            expireAfterAccess(expireAfterAccess)
            maximumSize(100_000)
        }
}
