package io.bluetape4k.cache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.testcontainers.storage.Ignite2Server
import io.bluetape4k.utils.ShutdownQueue
import org.apache.ignite.Ignition
import org.apache.ignite.client.ClientCache
import org.apache.ignite.client.IgniteClient
import org.apache.ignite.configuration.ClientConfiguration

object IgniteServers: KLogging() {

    /**
     * 테스트에서 사용하는 알려진 캐시 이름 목록.
     * 클라이언트 초기화 시 일괄 생성하여 개별 재시도 비용을 제거합니다.
     */
    private val KNOWN_CACHES = listOf(
        // memoizer 테스트
        "memoizer:heavy", "memoizer:factorial", "memoizer:fibonacci", "memoizer:concurrent",
        "async:memoizer:heavy", "async:memoizer:factorial", "async:memoizer:fibonacci",
        "async:memoizer:concurrent", "async:memoizer:bypass", "async:memoizer:fail-retry",
        "suspend:memoizer:heavy", "suspend:memoizer:factorial", "suspend:memoizer:fibonacci",
        "suspend:memoizer:concurrent",
        // jcache / nearcache 테스트
        "ignite2-suspendCache",
        "ignite2-back-cocache",
        "ignite2-near-suspend",
        "ignite-caches-test-client-suspend",
    )

    val ignite2Server by lazy { Ignite2Server.Launcher.ignite2 }

    val igniteClient: IgniteClient by lazy {
        Ignition.startClient(
            ClientConfiguration().apply {
                setAddresses(ignite2Server.url)
                setTimeout(60_000)  // 60초 소켓 타임아웃 (arm64 느린 초기화 대비)
            }
        ).also { client ->
            ShutdownQueue.register { client.close() }
            // 알려진 캐시를 일괄 생성 후 각각 readiness 대기
            KNOWN_CACHES.forEach { name ->
                val cache = client.getOrCreateCache<Any, Any>(name)
                waitForCacheReady(cache, name)
            }
        }
    }

    /**
     * 서버 측 캐시 초기화가 완료될 때까지 대기합니다.
     * arm64 Ignite에서 캐시 생성 직후 "Cache does not exist" 예외가 발생할 수 있습니다.
     */
    private fun waitForCacheReady(cache: ClientCache<*, *>, cacheName: String) {
        repeat(60) { attempt ->
            try {
                cache.size()
                return
            } catch (e: Exception) {
                log.warn(e) { "캐시 초기화 대기 중... (attempt=${attempt + 1}, cache=$cacheName)" }
                Thread.sleep(500)
            }
        }
    }

    /**
     * 캐시를 가져오거나 생성합니다.
     * 알려진 캐시는 이미 초기화되어 있으므로 즉시 반환됩니다.
     * 동적 캐시(테스트별 임시 캐시)는 짧은 재시도로 readiness를 확인합니다.
     */
    fun <K: Any, V: Any> getOrCreateCache(name: String): ClientCache<K, V> {
        val cache = igniteClient.getOrCreateCache<K, V>(name)
        if (KNOWN_CACHES.contains(name)) return cache

        // 동적 캐시: 짧은 재시도
        repeat(20) { attempt ->
            try {
                cache.size()
                return cache
            } catch (e: Exception) {
                log.warn(e) { "캐시 초기화 대기 중... (attempt=${attempt + 1}, cache=$name)" }
                Thread.sleep(200)
            }
        }
        return cache
    }
}
