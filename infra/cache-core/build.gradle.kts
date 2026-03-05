plugins {
    `java-test-fixtures`
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
    
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)

    testFixturesApi(project(":bluetape4k-junit5"))
    testFixturesImplementation(Libs.kotlinx_coroutines_test)

    testImplementation(testFixtures(project(":bluetape4k-cache-core")))
    testImplementation(project(":bluetape4k-junit5"))
}
