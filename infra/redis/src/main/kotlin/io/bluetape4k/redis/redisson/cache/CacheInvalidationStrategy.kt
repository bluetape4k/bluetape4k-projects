package io.bluetape4k.redis.redisson.cache

import org.redisson.api.RMap

interface CacheInvalidationStrategy<ID: Any> {
    fun invalidate(vararg ids: ID)
    fun invalidateAll()
    fun invalidateByPattern(pattern: String)
}

class RedisCacheInvalidationStrategy<ID: Any>(
    private val cache: RMap<ID, *>,
): CacheInvalidationStrategy<ID> {

    override fun invalidate(vararg ids: ID) {
        cache.fastRemove(*ids)
    }

    override fun invalidateAll() {
        cache.clear()
    }

    override fun invalidateByPattern(pattern: String) {
        val keys = cache.keySet(pattern)
        cache.fastRemove(*keys.toTypedArray())
    }
}
