package io.bluetape4k.hazelcast.cache

import com.hazelcast.config.EvictionConfig
import com.hazelcast.config.EvictionPolicy
import com.hazelcast.config.InMemoryFormat
import com.hazelcast.config.MaxSizePolicy
import com.hazelcast.config.NearCacheConfig
import java.io.Serializable

/**
 * Hazelcast [com.hazelcast.map.IMap]에 적용할 Near Cache 설정을 담는 클래스입니다.
 *
 * Near Cache는 **클라이언트 모드**에서만 지원됩니다 (Hazelcast 5.x 오픈소스).
 * 임베디드(서버) 모드에서의 Near Cache는 Enterprise 기능입니다.
 *
 * ```kotlin
 * val nearCacheCfg = HazelcastNearCacheConfig(
 *     mapName = "my-map",
 *     timeToLiveSeconds = 60,
 *     maxSize = 10_000,
 * )
 * val clientConfig = ClientConfig().apply {
 *     addNearCacheConfig(nearCacheCfg.toNearCacheConfig())
 * }
 * val client = HazelcastClient.newHazelcastClient(clientConfig)
 * val map = client.getMap<String, MyEntity>("my-map") // Near Cache 자동 적용
 * ```
 *
 * @property mapName Near Cache를 적용할 [com.hazelcast.map.IMap] 이름 (와일드카드 `*` 사용 가능)
 * @property timeToLiveSeconds 항목의 최대 생존 시간 (초), 0이면 무제한
 * @property maxIdleSeconds 마지막 접근 후 최대 유휴 시간 (초), 0이면 무제한
 * @property maxSize Near Cache의 최대 항목 수
 * @property evictionPolicy 제거 정책 (LRU, LFU, RANDOM, NONE)
 * @property invalidateOnChange Back Cache(원격) 변경 시 Near Cache 무효화 여부
 * @property inMemoryFormat Near Cache 저장 형식 (BINARY 또는 OBJECT)
 */
data class HazelcastNearCacheConfig(
    val mapName: String,
    val timeToLiveSeconds: Int = 60,
    val maxIdleSeconds: Int = 120,
    val maxSize: Int = 10_000,
    val evictionPolicy: EvictionPolicy = EvictionPolicy.LRU,
    val invalidateOnChange: Boolean = true,
    val inMemoryFormat: InMemoryFormat = InMemoryFormat.BINARY,
    val localUpdatePolicy: NearCacheConfig.LocalUpdatePolicy = NearCacheConfig.LocalUpdatePolicy.CACHE_ON_UPDATE,
): Serializable {

    companion object {
        /** 기본 Near Cache 설정 (맵 이름을 지정해야 합니다) */
        fun default(mapName: String) = HazelcastNearCacheConfig(mapName = mapName)

        /**
         * 읽기 전용(Read-Only)에 최적화된 Near Cache 설정입니다.
         * TTL이 길고 무효화를 비활성화합니다.
         */
        fun readOnly(mapName: String) = HazelcastNearCacheConfig(
            mapName = mapName,
            timeToLiveSeconds = 3600,
            maxIdleSeconds = 0,
            invalidateOnChange = false,
        )
    }

    /**
     * Hazelcast의 [NearCacheConfig]으로 변환합니다.
     *
     * @return Hazelcast [NearCacheConfig] 인스턴스
     */
    fun toNearCacheConfig(): NearCacheConfig =
        NearCacheConfig(mapName).apply {
            timeToLiveSeconds = this@HazelcastNearCacheConfig.timeToLiveSeconds
            maxIdleSeconds = this@HazelcastNearCacheConfig.maxIdleSeconds
            isInvalidateOnChange = invalidateOnChange
            setInMemoryFormat(this@HazelcastNearCacheConfig.inMemoryFormat)
            localUpdatePolicy = this@HazelcastNearCacheConfig.localUpdatePolicy
            evictionConfig = EvictionConfig().apply {
                evictionPolicy = this@HazelcastNearCacheConfig.evictionPolicy
                maxSizePolicy = MaxSizePolicy.ENTRY_COUNT
                size = maxSize
            }
        }
}
