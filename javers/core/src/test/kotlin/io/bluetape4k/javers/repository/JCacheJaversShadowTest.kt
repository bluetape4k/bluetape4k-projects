package io.bluetape4k.javers.repository

import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.javers.repository.jcache.JCacheCdoSnapshotRepository
import org.javers.repository.api.JaversRepository
import org.javers.repository.jql.AbstractJaversShadowTest
import java.util.*

class JCacheJaversShadowTest: AbstractJaversShadowTest() {

    override fun prepareJaversRepository(): JaversRepository {
        val cacheManager = JCaching.Caffeine.cacheManager
        return JCacheCdoSnapshotRepository("jcache-${UUID.randomUUID()}", cacheManager)
    }
}
