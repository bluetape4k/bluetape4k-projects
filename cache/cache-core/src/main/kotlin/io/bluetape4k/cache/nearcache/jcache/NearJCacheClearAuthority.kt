package io.bluetape4k.cache.nearcache.jcache

/**
 * 공유될 수 있는 back namespace 전체 삭제에 대한 runtime 권한입니다.
 *
 * 이 값은 [NearJCacheConfig]에 포함되지 않으며 직렬화되는 설정이나 provider
 * ownership을 나타내지 않습니다. `EXCLUSIVE_BACK_CACHE`는 caller가 back cache
 * namespace를 독점적으로 관리한다는 명시적 판단일 때만 선택해야 합니다.
 */
enum class NearJCacheClearAuthority {
    /** `clear()` 계열의 namespace-wide destructive operation을 거부합니다. */
    DENY,

    /** caller가 back cache namespace를 독점한다고 명시한 경우에만 허용합니다. */
    EXCLUSIVE_BACK_CACHE,
}
