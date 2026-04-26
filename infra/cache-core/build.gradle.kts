plugins {
    `java-test-fixtures`
    id(Plugins.kover)
}

kover {
    reports {
        filters {
            excludes {
                // management MXBean 클래스는 향후 전면 재정비 예정 — 커버리지 측정 제외
                packages("io.bluetape4k.cache.nearcache.jcache.management")
                // testFixtures의 abstract 테스트 클래스: 실제 실행은 downstream 모듈(cache-lettuce/redisson/hazelcast)에서만 됨
                // cache-core 자체 테스트에서 0% 커버되므로 제외
                classes(
                    "io.bluetape4k.cache.nearcache.AbstractNearCacheOperationsTest*",
                    "io.bluetape4k.cache.nearcache.AbstractSuspendNearCacheOperationsTest*",
                    "io.bluetape4k.cache.nearcache.AbstractResilientNearCacheOperationsTest*",
                    "io.bluetape4k.cache.nearcache.AbstractResilientSuspendNearCacheOperationsTest*",
                    "io.bluetape4k.cache.nearcache.jcache.AbstractSuspendNearJCacheTest*"
                )
            }
        }
    }
}

configurations {
    // compileOnly 나 runtimeOnly로 지정된 Dependency를 testImplementation 으로도 지정하도록 합니다.
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-idgenerators"))

    api(Libs.javax_cache_api)

    // Local Java Cache providers (cache-local에서 병합)
    api(Libs.caffeine)
    api(Libs.caffeine_jcache)
    compileOnly(Libs.cache2k_core)
    compileOnly(Libs.cache2k_jcache)
    compileOnly(Libs.ehcache)
    compileOnly(Libs.ehcache_clustered)
    compileOnly(Libs.ehcache_transactions)

    // bluetape4k-resilience4j는 cache-redisson에 compileOnly 의존하여 순환 의존성 발생 → 직접 라이브러리 사용
    implementation(Libs.resilience4j_retry)
    implementation(Libs.resilience4j_kotlin)

    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)

    testFixturesApi(project(":bluetape4k-junit5"))
    testFixturesApi(Libs.awaitility_kotlin)
    testFixturesImplementation(Libs.kotlinx_coroutines_core)
    testFixturesImplementation(Libs.kotlinx_coroutines_test)

    testImplementation(testFixtures(project(":bluetape4k-cache-core")))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(Libs.awaitility_kotlin)
}
