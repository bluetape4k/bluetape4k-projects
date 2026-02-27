package io.bluetape4k.cache.nearcache.coroutines

import io.bluetape4k.cache.jcache.coroutines.CaffeineSuspendCache
import io.bluetape4k.cache.jcache.coroutines.Ignite2ClientSuspendCache
import io.bluetape4k.cache.jcache.coroutines.SuspendCache
import io.bluetape4k.cache.nearcache.ignite.coroutines.IgniteNearSuspendCache
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.testcontainers.storage.Ignite2Server
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.apache.ignite.Ignition
import org.apache.ignite.client.IgniteClient
import org.apache.ignite.configuration.ClientConfiguration
import org.junit.jupiter.api.Test
import java.util.*

class Ignite2NearSuspendCacheTest: AbstractNearSuspendCacheTest() {

    companion object {
        private val ignite2Server by lazy { Ignite2Server.Launcher.ignite2 }
        private val igniteClient: IgniteClient by lazy {
            Ignition.startClient(
                ClientConfiguration().apply {
                    setAddresses(ignite2Server.url)
                }
            ).also { ShutdownQueue.register { it.close() } }
        }
    }

    override val backSuspendCache: SuspendCache<String, Any> by lazy {
        Ignite2ClientSuspendCache(
            igniteClient.getOrCreateCache("ignite2-back-cocache-" + UUID.randomUUID().encodeBase62())
        )
    }

    override fun createFrontSuspendCache(expireAfterAccess: java.time.Duration): SuspendCache<String, Any> =
        CaffeineSuspendCache {
            this.expireAfterAccess(expireAfterAccess)
            this.maximumSize(10_000)
        }

    @Test
    fun `Ignite 전용 NearSuspendCache를 생성하고 동작해야 한다`() = runSuspendIO {
        val cacheName = "ignite2-near-suspend-" + UUID.randomUUID().encodeBase62()
        val cache = IgniteNearSuspendCache<String, Any>(cacheName)
        cache shouldBeInstanceOf IgniteNearSuspendCache::class

        val key = getKey()
        val value = getValue()
        cache.put(key, value)
        cache.get(key) shouldBeEqualTo value

        cache.clearAll()
        cache.containsKey(key).shouldBeFalse()
        cache.close()
    }
}
