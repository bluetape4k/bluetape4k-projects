package io.bluetape4k.exposed.cache

/**
 * 캐시 저장 방식을 정의합니다.
 *
 * 인프로세스 로컬 캐시만 사용하거나, 원격 캐시(Redis)만 사용하거나,
 * 로컬(Near) 캐시와 Redis를 함께 사용하는 방식을 선택할 수 있습니다.
 */
enum class CacheMode {

    /**
     * 인프로세스 캐시만 사용합니다 (예: Caffeine, Cache2k).
     *
     * 네트워크 없이 JVM 힙 메모리만 활용하므로 가장 빠르지만,
     * 프로세스 간 공유가 불가능합니다.
     */
    LOCAL,

    /**
     * Redis(원격 캐시)만 사용합니다.
     */
    REMOTE,

    /**
     * 로컬(Near) 캐시 + Redis를 함께 사용합니다.
     *
     * 로컬 캐시(예: Caffeine)를 L1, Redis를 L2로 사용하여
     * 네트워크 왕복을 줄이고 읽기 성능을 극대화합니다.
     */
    NEAR_CACHE,
}
