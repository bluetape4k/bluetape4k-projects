package io.bluetape4k.cache.nearcache.management

class EmptyNearCacheStatisticsMXBean: NearCacheStatisticsMXBean() {

    override fun addHits(value: Long) {
        // Nothing to do.
    }

    override fun addMisses(value: Long) {
        // Nothing to do.
    }

    override fun addPuts(value: Long) {
        // Nothing to do.
    }

    override fun addEvitions(value: Long) {
        // Nothing to do.
    }

    override fun addGetTime(value: Long) {
        // Nothing to do.
    }

    override fun addPutTime(value: Long) {
        // Nothing to do.
    }

    override fun addRemoveTime(value: Long) {
        // Nothing to do.
    }
}
