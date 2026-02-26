package io.bluetape4k.cache.nearcache.management

import io.bluetape4k.cache.nearcache.NearCache
import javax.cache.management.CacheMXBean

class NearCacheManagementMXBean(private val cache: NearCache<*, *>): CacheMXBean {

    // TODO: Configuration 정보를 얻기 위한 getConfiguration 에서 CapturedType 문제가 있다.
    // TODO: 이 부분은 Java class 로 제작해야 할 듯 하다
    //    private val configClazz = CompleteConfiguration::class.java as Class<out CompleteConfiguration<*, *>>
    //    private val cacheConfiguration = cache.backCache.getConfiguration(configClazz)

    /**
     * Determines the required type of keys for this [Cache], if any.
     *
     * @return the fully qualified class name of the key type,
     * or "java.lang.Object" if the type is undefined.
     */
    override fun getKeyType(): String {
        TODO("구현 중")
        //        return cacheConfiguration.keyType.name
    }

    /**
     * Determines the required type of values for this [Cache], if any.
     * @return the fully qualified class name of the value type,
     * or "java.lang.Object" if the type is undefined.
     */
    override fun getValueType(): String {
        TODO("구현 중")
        //        return cacheConfiguration.valueType.name
    }

    /**
     * Determines if a [Cache] should operate in read-through mode.
     *
     *
     * When in read-through mode, cache misses that occur due to cache entries
     * not existing as a result of performing a "get" call via one of
     * [Cache.get],
     * [Cache.getAll],
     * [Cache.getAndRemove] and/or
     * [Cache.getAndReplace] will appropriately
     * cause the configured [CacheLoader] to be
     * invoked.
     *
     *
     * The default value is `false`.
     *
     * @return `true` when a [Cache] is in
     * "read-through" mode.
     * @see CacheLoader
     */
    override fun isReadThrough(): Boolean {
        TODO("구현 중")
        //        return cacheConfiguration.isReadThrough
    }

    /**
     * Determines if a [Cache] should operate in "write-through"
     * mode.
     *
     *
     * When in "write-through" mode, cache updates that occur as a result of
     * performing "put" operations called via one of
     * [Cache.put],
     * [Cache.getAndRemove],
     * [Cache.removeAll],
     * [Cache.getAndPut]
     * [Cache.getAndRemove],
     * [Cache.getAndReplace],
     * [Cache.invoke]
     * [Cache.invokeAll]
     *
     *
     * will appropriately cause the configured [CacheWriter] to be invoked.
     *
     *
     * The default value is `false`.
     *
     * @return `true` when a [Cache] is in "write-through" mode.
     * @see CacheWriter
     */
    override fun isWriteThrough(): Boolean {
        TODO("구현 중")
        //        return cacheConfiguration.isWriteThrough
    }

    /**
     * Whether storeByValue (true) or storeByReference (false).
     * When true, both keys and values are stored by value.
     *
     *
     * When false, both keys and values are stored by reference.
     * Caches stored by reference are capable of mutation by any threads holding
     * the reference. The effects are:
     *
     *  * if the key is mutated, then the key may not be retrievable or
     * removable
     *  * if the value is mutated, then all threads in the JVM can potentially
     * observe those mutations, subject to the normal Java Memory Model rules.
     *
     * Storage by reference only applies to the local heap. If an entry is moved off
     * heap it will need to be transformed into a representation. Any mutations that
     * occur after transformation may not be reflected in the cache.
     *
     *
     * When a cache is storeByValue, any mutation to the key or value does not affect
     * the key of value stored in the cache.
     *
     *
     * The default value is `true`.
     *
     * @return true if the cache is store by value
     */
    override fun isStoreByValue(): Boolean {
        TODO("구현 중")
        //        return cacheConfiguration.isStoreByValue
    }

    /**
     * Checks whether statistics collection is enabled in this cache.
     *
     *
     * The default value is `false`.
     *
     * @return true if statistics collection is enabled
     */
    override fun isStatisticsEnabled(): Boolean {
        TODO("구현 중")
        //        return cacheConfiguration.isStatisticsEnabled
    }

    /**
     * Checks whether management is enabled on this cache.
     *
     *
     * The default value is `false`.
     *
     * @return true if management is enabled
     */
    override fun isManagementEnabled(): Boolean {
        TODO("구현 중")
        //        return cacheConfiguration.isManagementEnabled
    }
}
