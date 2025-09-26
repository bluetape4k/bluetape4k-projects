package io.bluetape4k.cache.nearcache.coroutines

import io.bluetape4k.cache.jcache.coroutines.RedissonSuspendCache
import io.bluetape4k.cache.jcache.coroutines.SuspendCache
import io.bluetape4k.cache.jcache.jcacheConfiguration
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.junit5.awaitility.suspendUntil
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import javax.cache.expiry.CreatedExpiryPolicy
import javax.cache.expiry.Duration

class RedisNearSuspendCacheTest: AbstractNearSuspendCacheTest() {

    companion object: KLogging() {
        private val redis by lazy { RedisServer.Launcher.redis }

        private val redisson by lazy {
            RedisServer.Launcher.RedissonLib.getRedisson()
        }
    }

    override val backSuspendCache: SuspendCache<String, Any> by lazy {
        val configuration = jcacheConfiguration<String, Any> {
            setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration(TimeUnit.MILLISECONDS, 1000L)))
        }
        RedissonSuspendCache("redis-back-cocache" + TimebasedUuid.nextBase62String(), redisson, configuration)
    }

    @Test
    fun `back cache entry가 expire 되면 event listener를 통해 front cache가 삭제됩니다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        nearSuspendCache1.put(key, value)
        await suspendUntil { nearSuspendCache2.containsKey(key) }

        nearSuspendCache1.containsKey(key).shouldBeTrue()
        nearSuspendCache2.containsKey(key).shouldBeTrue()

        // NOTE: backCache 에서 cache expire 가 수행될 때까지 대기한다 (backCache.entries 에 접근하면 expired event 가 발생한다)
        // NearCache 내에서 Expire 검사 Thread로 동작해야 합니다.
        await suspendUntil { !nearSuspendCache2.containsKey(key) }
        await suspendUntil { !nearSuspendCache1.containsKey(key) }

        backSuspendCache.containsKey(key).shouldBeFalse()
        nearSuspendCache1.containsKey(key).shouldBeFalse()
        nearSuspendCache2.containsKey(key).shouldBeFalse()
    }
}
