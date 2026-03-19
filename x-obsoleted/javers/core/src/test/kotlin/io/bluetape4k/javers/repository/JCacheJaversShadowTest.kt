package io.bluetape4k.javers.repository

import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.codec.Base58
import io.bluetape4k.javers.repository.jcache.JCacheCdoSnapshotRepository
import org.javers.repository.api.JaversRepository
import org.javers.repository.jql.AbstractJaversShadowTest

class JCacheJaversShadowTest: AbstractJaversShadowTest() {

    override fun prepareJaversRepository(): JaversRepository {
        val cacheManager = JCaching.Caffeine.cacheManager
        return JCacheCdoSnapshotRepository("jcache-${Base58.randomString(12)}", cacheManager)
    }
}
