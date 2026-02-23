package io.bluetape4k.ignite3.cache

import java.io.Serializable
import java.time.Duration

/**
 * Apache Ignite 3.x 기반 NearCache 설정 클래스입니다.
 *
 * Ignite 3.x 오픈소스는 클라이언트 측 Near Cache를 내장하지 않으므로,
 * Caffeine을 Front Cache로, Ignite [org.apache.ignite.table.KeyValueView]를
 * Back Cache로 사용하는 2-Tier NearCache를 구성합니다.
 *
 * @property tableName Ignite 3.x 테이블 이름 (Back Cache로 사용)
 * @property frontCacheMaxSize Front Cache(Caffeine)의 최대 항목 수
 * @property frontCacheTtl Front Cache 항목의 최대 생존 시간
 * @property frontCacheMaxIdleTime Front Cache 항목의 최대 유휴 시간 (ZERO이면 무제한)
 * @property syncOnWrite 쓰기 시 Front와 Back을 동기 방식으로 동기화할지 여부
 */
data class IgniteNearCacheConfig(
    val tableName: String,
    val frontCacheMaxSize: Long = 10_000L,
    val frontCacheTtl: Duration = Duration.ofMinutes(10),
    val frontCacheMaxIdleTime: Duration = Duration.ofMinutes(30),
    val syncOnWrite: Boolean = true,
): Serializable {
    companion object {
        /** 읽기 전용에 최적화된 설정 (TTL 길고 무효화 없음) */
        fun readOnly(tableName: String) = IgniteNearCacheConfig(
            tableName = tableName,
            frontCacheTtl = Duration.ofHours(1),
            frontCacheMaxIdleTime = Duration.ZERO,
            syncOnWrite = false,
        )

        /** 고성능 쓰기에 최적화된 설정 (짧은 TTL, 비동기) */
        fun writeOptimized(tableName: String) = IgniteNearCacheConfig(
            tableName = tableName,
            frontCacheTtl = Duration.ofMinutes(5),
            syncOnWrite = false,
        )
    }
}
