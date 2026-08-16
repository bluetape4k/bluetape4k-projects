plugins {
    `java-test-fixtures`
    alias(bt4k.plugins.kover)
}

kover {
    reports {
        filters {
            excludes {
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

    api(bt4k.javax.cache.api)

    // Local Java Cache providers (cache-local에서 병합)
    api(bt4k.caffeine)
    api(bt4k.caffeine.jcache)
    compileOnly(bt4k.cache2k.core)
    compileOnly(bt4k.cache2k.jcache)
    compileOnly(bt4k.ehcache)
    compileOnly(bt4k.ehcache.clustered)
    compileOnly(bt4k.ehcache.transactions)

    // bluetape4k-resilience4j는 cache-redisson에 compileOnly 의존하여 순환 의존성 발생 → 직접 라이브러리 사용
    implementation(bt4k.resilience4j.retry)
    implementation(bt4k.resilience4j.kotlin)

    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)

    testFixturesApi(project(":bluetape4k-junit5"))
    testFixturesApi(
        "org.awaitility:awaitility-kotlin:${bt4k.versions.awaitility.get()}",
    )
    testFixturesImplementation(libs.kotlinx.coroutines.core)
    testFixturesImplementation(libs.kotlinx.coroutines.test)

    testImplementation(testFixtures(project(":bluetape4k-cache-core")))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.awaitility.kotlin)
}
