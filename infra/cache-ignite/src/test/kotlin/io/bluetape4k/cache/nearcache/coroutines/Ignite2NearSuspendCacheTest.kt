package io.bluetape4k.cache.nearcache.coroutines

import io.bluetape4k.cache.jcache.coroutines.Ignite2ClientSuspendCache
import io.bluetape4k.cache.jcache.coroutines.SuspendCache
import io.bluetape4k.cache.jcache.coroutines.CaffeineSuspendCache
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.testcontainers.storage.Ignite2Server
import io.bluetape4k.utils.ShutdownQueue
import org.apache.ignite.Ignition
import org.apache.ignite.client.IgniteClient
import org.apache.ignite.configuration.ClientConfiguration
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
}
