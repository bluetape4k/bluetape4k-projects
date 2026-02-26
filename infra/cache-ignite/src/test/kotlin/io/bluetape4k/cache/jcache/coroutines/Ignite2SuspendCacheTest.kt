package io.bluetape4k.cache.jcache.coroutines

import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.testcontainers.storage.Ignite2Server
import io.bluetape4k.utils.ShutdownQueue
import org.apache.ignite.Ignition
import org.apache.ignite.client.IgniteClient
import org.apache.ignite.configuration.ClientConfiguration
import java.util.*

class Ignite2SuspendCacheTest: AbstractSuspendCacheTest() {

    companion object {
        private val ignite2Server by lazy { Ignite2Server.Launcher.ignite2 }
        private val igniteClient: IgniteClient by lazy {
            val cfg = ClientConfiguration().apply { setAddresses(ignite2Server.url) }
            Ignition.startClient(cfg).also {
                ShutdownQueue.register(it)
            }
        }
    }

    override val suspendCache: SuspendCache<String, Any> =
        Ignite2ClientSuspendCache(
            igniteClient.getOrCreateCache("ignite2-suspendCache-" + UUID.randomUUID().encodeBase62())
        )
}
