package io.bluetape4k.cache.jcache

import io.bluetape4k.cache.IgniteServers.igniteClient
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IgniteSuspendCacheTest: AbstractSuspendCacheTest() {

    companion object: KLogging()

    override val suspendCache: SuspendCache<String, Any> by lazy {
        IgniteClientSuspendCache(
            igniteClient.getOrCreateCache("ignite2-suspendCache-" + UUID.randomUUID().encodeBase62())
        )
    }
}
