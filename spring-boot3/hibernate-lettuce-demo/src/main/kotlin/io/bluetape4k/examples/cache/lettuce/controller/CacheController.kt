package io.bluetape4k.examples.cache.lettuce.controller

import io.bluetape4k.hibernate.cache.lettuce.LettuceNearCacheRegionFactory
import jakarta.persistence.EntityManagerFactory
import org.hibernate.cache.spi.RegionFactory
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/cache")
/**
 * Hibernate near-cache 상태를 조회하고 local(L1) cache만 비우는 관리용 API이다.
 *
 * 이 컨트롤러의 eviction은 Redis L2를 건드리지 않는다.
 */
class CacheController(private val entityManagerFactory: EntityManagerFactory) {

    data class CacheStats(
        val regionName: String,
        val localSize: Long,
        val localHitCount: Long?,
        val localMissCount: Long?,
        val localHitRate: Double?,
    )

    @GetMapping("/stats")
    fun getCacheStats(): Map<String, CacheStats> {
        val factory = getRegionFactory() ?: return emptyMap()
        return factory.getCaches().mapValues { (regionName, cache) ->
            val stats = cache.localStats()
            CacheStats(
                regionName = regionName,
                localSize = cache.localCacheSize(),
                localHitCount = stats?.hitCount(),
                localMissCount = stats?.missCount(),
                localHitRate = stats?.hitRate(),
            )
        }
    }

    @DeleteMapping("/evict/{region}")
    fun evictRegion(@PathVariable region: String): ResponseEntity<String> {
        val factory = getRegionFactory()
            ?: return ResponseEntity.internalServerError().body("RegionFactory not available")
        val cache = factory.getCaches()[region]
            ?: return ResponseEntity.notFound().build()
        cache.clearLocal()
        return ResponseEntity.ok("Evicted local cache (L1 only) for region: $region")
    }

    @DeleteMapping("/evict")
    fun evictAll(): ResponseEntity<String> {
        val factory = getRegionFactory()
            ?: return ResponseEntity.internalServerError().body("RegionFactory not available")
        factory.getCaches().values.forEach { it.clearLocal() }
        return ResponseEntity.ok("Evicted all local caches (L1 only, ${factory.getCaches().size} regions)")
    }

    private fun getRegionFactory(): io.bluetape4k.hibernate.cache.lettuce.LettuceNearCacheRegionFactory? {
        return runCatching {
            val sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor::class.java)
            sessionFactory.serviceRegistry.getService(RegionFactory::class.java)
                    as? LettuceNearCacheRegionFactory
        }.getOrNull()
    }
}
