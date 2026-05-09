package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.codec.Base58
import javax.cache.CacheManager
import javax.cache.configuration.Factory
import javax.cache.configuration.MutableConfiguration

/**
 * [NearJCacheConfig]를 DSL 방식으로 생성하기 위한 빌더 클래스입니다.
 *
 * ```kotlin
 * val config = nearJCacheConfig<String, MyValue> {
 *     cacheName = "my-near-cache"
 *     isSynchronous = true
 *     syncRemoteTimeout = 1000L
 * }
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 */
class NearJCacheConfigBuilder<K: Any, V: Any> {

    /** Front Cache용 [CacheManager] 팩토리. 기본값: Caffeine */
    var cacheManagerFactory: Factory<CacheManager> = NearJCacheConfig.CaffeineCacheManagerFactory

    /** 캐시 이름. 기본값: 무작위 생성 */
    var cacheName: String = "near-jcache-" + Base58.randomString(8)

    /** Front Cache 설정. 기본값: 접근 기준 30분 만료 */
    var frontCacheConfiguration: MutableConfiguration<K, V> = NearJCacheConfig.getDefaultFrontCacheConfiguration()

    /** Back Cache 이벤트 동기화 여부. 기본값: false (비동기) */
    var isSynchronous: Boolean = false

    /** 원격 캐시 동기화 타임아웃 (밀리초). 기본값: 500ms */
    var syncRemoteTimeout: Long = NearJCacheConfig.DEFAULT_SYNC_REMOTE_TIMEOUT

    /**
     * 설정값으로 [NearJCacheConfig] 인스턴스를 생성합니다.
     */
    fun build(): NearJCacheConfig<K, V> = NearJCacheConfig(
        cacheManagerFactory = cacheManagerFactory,
        cacheName = cacheName,
        frontCacheConfiguration = frontCacheConfiguration,
        isSynchronous = isSynchronous,
        syncRemoteTimeout = syncRemoteTimeout,
    )
}

/**
 * DSL 방식으로 [NearJCacheConfig]를 생성합니다.
 *
 * ```kotlin
 * val config = nearJCacheConfig<String, MyValue> {
 *     cacheName = "my-near-cache"
 *     isSynchronous = true
 * }
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @param block [NearJCacheConfigBuilder]를 구성하는 DSL 블록
 * @return 생성된 [NearJCacheConfig] 인스턴스
 */
inline fun <K: Any, V: Any> nearJCacheConfig(
    block: NearJCacheConfigBuilder<K, V>.() -> Unit,
): NearJCacheConfig<K, V> =
    NearJCacheConfigBuilder<K, V>().apply(block).build()
