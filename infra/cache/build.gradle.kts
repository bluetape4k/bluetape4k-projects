configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-idgenerators"))
    compileOnly(project(":bluetape4k-coroutines"))
    testImplementation(project(":bluetape4k-netty"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Cache Providers
    api(Libs.javax_cache_api)
    api(Libs.caffeine)
    api(Libs.caffeine_jcache)
    compileOnly(Libs.cache2k_core)
    compileOnly(Libs.cache2k_jcache)
    compileOnly(Libs.ehcache)
    compileOnly(Libs.ehcache_clustered)
    compileOnly(Libs.ehcache_transactions)

    compileOnly(Libs.redisson)

    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    testImplementation(Libs.springBootStarter("cache"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude("org.junit.vintage", "junit-vintage-engine")
        exclude("junit", "junit")
    }
}
